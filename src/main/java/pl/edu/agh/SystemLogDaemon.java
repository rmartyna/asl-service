package pl.edu.agh;

import org.apache.log4j.Logger;

import java.util.Date;
import java.util.List;

public class SystemLogDaemon extends Daemon {

    private Date startLoopTime;

    private List<SystemLog> systemLogs;

    private static final Logger LOGGER = Logger.getLogger(SystemLogDaemon.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }

    public void run() {
        while(true) {
            startLoopTime = new Date();
            if(getConfiguration().get("enabled") != 0) {
                LOGGER.info("System log loop start");

                LOGGER.info("Gathering logs: ");
                for(SystemLog systemLog : systemLogs) {
                    try {
                        systemLog.gatherLogs();
                    } catch(Exception e) {
                        LOGGER.error("Error gathering logs for: " + systemLog.getFilePath(), e);
                    }
                }

                LOGGER.info("Saving logs");
                saveLogs();
            }
            waitForNextLoop();
        }
    }

    public void saveLogs() {
        for(SystemLog systemLog : systemLogs)
            systemLog.saveLogs();
    }

    public Date getStartLoopTime() {
        return startLoopTime;
    }

    public Integer getDaemonId() {
        LOGGER.error("getDaemonId method is not implemented for NetworkDaemon");
        return null;
    }

    @Override
    public String operation(String name, String value) {
        LOGGER.info("Executing operation " + name + " with value " + value);

        if(name.equals("addLog"))
            return addLog(value);

        if(name.equals("removeLog"))
            return removeLog(value);

        //TODO implement
        if(name.equals("listLogs"))
            throw new IllegalArgumentException("listLogs operation is not implemented yet");

        throw new IllegalArgumentException("Operation " + name + " is not available");
    }

    public String addLog(String path) {
        LOGGER.info("Adding log at path " + path);

        for(SystemLog systemLog : systemLogs)
            if(systemLog.getFilePath().equals(path))
                throw new IllegalArgumentException("System log " + path + " already exists.");

        try {
            SystemLog systemLog = new SystemLog();
            systemLog.setDbConnection(getDbConnection());
            systemLog.setFilePath(path);
            systemLog.afterPropertiesSet();
            systemLogs.add(systemLog);
            LOGGER.info("System log added successfully");
            return "Added system log " + path;
        } catch(Exception e) {
            throw new IllegalArgumentException("Failed to add system log " + path, e);
        }

    }

    public String removeLog(String path) {
        LOGGER.info("Removing log at path " + path);

        for(SystemLog systemLog : systemLogs) {
            if(systemLog.getFilePath().equals(path)) {
                systemLogs.remove(systemLog);
                LOGGER.info("System log removed successfully");
                return "Removed system log " + path;
            }
        }

        throw new IllegalArgumentException("System log " + path + " cannot be removed because it does not exist");
    }

    public void setSystemLogs(List<SystemLog> systemLogs) {
        this.systemLogs = systemLogs;
    }

}
