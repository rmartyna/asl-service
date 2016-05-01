package pl.edu.agh;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        throw new IllegalArgumentException("No available operations");
    }

    public void setSystemLogs(List<SystemLog> systemLogs) {
        this.systemLogs = systemLogs;
    }

}
