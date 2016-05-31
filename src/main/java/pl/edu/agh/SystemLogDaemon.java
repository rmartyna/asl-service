package pl.edu.agh;

import org.apache.log4j.Logger;
import pl.edu.agh.dao.SystemLogsDAO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This software may be modified and distributed under the terms
 *  of the BSD license.  See the LICENSE.txt file for details.
 */

public class SystemLogDaemon extends Daemon {

    private Date startLoopTime;

    private List<SystemLog> systemLogs;

    private SystemLogsDAO systemLogsDAO;

    private static final Logger LOGGER = Logger.getLogger(SystemLogDaemon.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }

    public void run() {
        while (true) {
            LOGGER.info("System log loop start");
            startLoopTime = new Date();
            if (configured) {
                getData();
            }
            waitForNextLoop();
        }
    }

    public void getData() {
        if(getConfiguration().get("enabled") != 0) {
            LOGGER.info("Gathering logs: ");
            for(SystemLog systemLog : systemLogs) {
                try {
                    systemLog.gatherLogs();
                } catch(Exception e) {
                    LOGGER.error("Error gathering logs for: " + systemLog.getFilePath(), e);
                }
            }
        }
    }

    public synchronized void saveLogs() {
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
    public synchronized void configure(Map<String, String> newConfiguration) throws IllegalArgumentException {
        String logsList = null;

        for(String attribute: newConfiguration.keySet()) {
            if (attribute.equalsIgnoreCase("list")) {
                logsList = newConfiguration.get(attribute);

            }
        }
        if(logsList != null)
            newConfiguration.remove("list");

        super.configure(newConfiguration);

        logsList = logsList.trim();

        String[] logs = logsList.split(",");

        //remove if not found
        List<SystemLog> toRemove = new ArrayList<SystemLog>();
        for(SystemLog systemLog : systemLogs) {
            boolean found = false;
            for(String path : logs) {
                if(systemLog.getFilePath().equals(path)) {
                    found = true;
                    break;
                }
            }
            if(!found)
                toRemove.add(systemLog);
        }

        for(SystemLog systemLog : toRemove)
            systemLogs.remove(systemLog);


        //add if not found
        List<String> toAdd = new ArrayList<String>();
        for(String path : logs) {
            boolean found = false;
            for(SystemLog systemLog: systemLogs) {
                if(systemLog.getFilePath().equals(path)) {
                    found = true;
                    break;
                }
            }
            if(!found)
                toAdd.add(path);
        }

        for(String path : toAdd)
            addLog(path);

        //set service id
        for(SystemLog systemLog : systemLogs)
            systemLog.setServiceId(getServiceId());

        //get last log
        for(SystemLog systemLog : systemLogs)
            systemLog.getLastFile();
    }

    public void addLog(String path) {
        if(path.length() == 0)
            return;
        LOGGER.info("Adding log at path " + path);

        try {
            SystemLog systemLog = new SystemLog();
            systemLog.setFilePath(path);
            systemLog.afterPropertiesSet();
            systemLog.setSystemLogsDAO(systemLogsDAO);
            systemLogs.add(systemLog);
            LOGGER.info("System log added successfully");
        } catch(Exception e) {
            throw new IllegalArgumentException("Failed to add system log " + path, e);
        }
    }

    public void setSystemLogs(List<SystemLog> systemLogs) {
        this.systemLogs = systemLogs;
    }

    public void setSystemLogsDAO(SystemLogsDAO systemLogsDAO) {
        this.systemLogsDAO = systemLogsDAO;
    }
}
