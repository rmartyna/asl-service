package pl.edu.agh;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemoryDaemon extends Daemon {

    private Date startLoopTime;

    private Double current;

    private Double max;

    private static final Logger LOGGER = Logger.getLogger(MemoryDaemon.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }


    public void run() {
        while(true) {
            if(getConfiguration().get("enabled") != 0) {
                LOGGER.info("Memory loop start");
                startLoopTime = new Date();

                LOGGER.info("Computing current/max memory usage");
                try {
                    Process freeProcess = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "free | grep Mem"});
                    String freeOutput = IOUtils.toString(freeProcess.getInputStream(), "UTF-8");
                    LOGGER.info("Free output:\n" + freeOutput);

                    Pattern pattern = Pattern.compile("Mem:\\s+(\\d+)\\s+(\\d+)");
                    Matcher matcher = pattern.matcher(freeOutput);
                    matcher.find();
                    max = Double.parseDouble(matcher.group(1));
                    current = Double.parseDouble(matcher.group(2));

                    LOGGER.info("Current memory usage: " + current);
                    LOGGER.info("Max memory: " + max);
                } catch(IOException e) {
                    LOGGER.error("Error getting memory usage", e);
                }

                LOGGER.info("Saving logs");
                saveLogs();
            }
            waitForNextLoop();
        }
    }

    public void saveLogs() {
        try {
            if(current >= getConfiguration().get("memoryMin") && current <= getConfiguration().get("memoryMax")) {
                PreparedStatement saveMemoryUsage = getConnection().prepareStatement("INSERT INTO memory_usage(service_id, current, max, date) VALUES(?,?,?,?)");
                saveMemoryUsage.setInt(1, getServiceId());
                saveMemoryUsage.setDouble(2, current);
                saveMemoryUsage.setDouble(3, max);
                saveMemoryUsage.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                saveMemoryUsage.executeUpdate();
            }

        } catch(Exception e) {
            LOGGER.error("Could not save logs in the database", e);
        }
    }

    public Date getStartLoopTime() {
        return startLoopTime;
    }

    public Integer getDaemonId() {
        LOGGER.error("getDaemonId method is not implemented for MemoryDaemon");
        return null;
    }

}
