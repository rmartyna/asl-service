package pl.edu.agh;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import pl.edu.agh.beans.Memory;
import pl.edu.agh.dao.MemoryDAO;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemoryDaemon extends Daemon {

    private Date startLoopTime;

    private List<Memory> data = new ArrayList<Memory>();

    private MemoryDAO memoryDAO;

    private static final Logger LOGGER = Logger.getLogger(MemoryDaemon.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }

    public void run() {
        while(true) {
            LOGGER.info("Memory loop start");
            startLoopTime = new Date();
            if(configured) {
                getData();
            }
            waitForNextLoop();
        }
    }

    public void getData() {
        if(getConfiguration().get("enabled") != 0) {
            LOGGER.info("Computing current/max memory usage");
            try {
                Process freeProcess = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "free | grep Mem"});
                String freeOutput = IOUtils.toString(freeProcess.getInputStream(), "UTF-8");
                LOGGER.info("Free output:\n" + freeOutput);

                Pattern pattern = Pattern.compile("Mem:\\s+(\\d+)\\s+(\\d+)");
                Matcher matcher = pattern.matcher(freeOutput);
                matcher.find();
                Double max = Double.parseDouble(matcher.group(1));
                Double current= Double.parseDouble(matcher.group(2));

                Memory memory = new Memory(getServiceId(), current, max, new Timestamp(new Date().getTime()));
                synchronized(this) {
                    data.add(memory);
                }

            } catch(Exception e) {
                LOGGER.error("Error getting memory usage", e);
            }
        }
    }

    public synchronized void saveLogs() {
        try {
            for(Memory memory : data) {
                if (memory.getCurrent() >= getConfiguration().get("memoryMin") && memory.getCurrent() <= getConfiguration().get("memoryMax")) {
                    memoryDAO.insert(memory);
                }
            }
        } catch(Exception e) {
            LOGGER.error("Could not save logs in the database", e);
        }
        data = new ArrayList<Memory>();
    }

    public Integer getDaemonId() {
        LOGGER.error("getDaemonId method is not implemented for NetworkDaemon");
        return null;
    }

    public Date getStartLoopTime() {
        return startLoopTime;
    }

    public void setMemoryDAO(MemoryDAO memoryDAO) {
        this.memoryDAO = memoryDAO;
    }
}
