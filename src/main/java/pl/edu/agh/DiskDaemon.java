package pl.edu.agh;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import pl.edu.agh.beans.Cpu;
import pl.edu.agh.beans.Disk;
import pl.edu.agh.beans.DiskUsage;
import pl.edu.agh.beans.Partition;
import pl.edu.agh.dao.DiskDAO;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This software may be modified and distributed under the terms
 *  of the BSD license.  See the LICENSE.txt file for details.
 */

/**
 * Daemon that collects disk logs
 */
public class DiskDaemon extends Daemon {

    private Date startLoopTime;

    private Integer daemonId;

    private DiskDAO diskDAO;

    private List<DiskUsage> usageData = new ArrayList<DiskUsage>();

    private List<Partition> partitionData = new ArrayList<Partition>();

    private static final Logger LOGGER = Logger.getLogger(DiskDaemon.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }

    @Override
    public synchronized void configure(Map<String, String> newConfiguration) throws IllegalArgumentException {
        super.configure(newConfiguration);
        daemonId = getDaemonId();
    }

    @Override
    public void run() {
        while (true) {
            LOGGER.info("Disk loop start");
            startLoopTime = new Date();
            if (configured) {
                getData();
            }
            waitForNextLoop();
        }
    }

    @Override
    public void getData() {
        if(getConfiguration().get("enabled") != 0) {

            LOGGER.info("Computing disk read/write speed");
            try {
                Process iostatProcess = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "iostat -yd 1 1"});
                String iostatOutput = IOUtils.toString(iostatProcess.getInputStream(), "UTF-8");
                LOGGER.info("iostat disk output:\n" + iostatOutput);

                Pattern pattern = Pattern.compile("sda\\s+[0-9,]+\\s+([0-9,]+)\\s+([0-9,]+).*");
                for(String line : iostatOutput.split("\n")) {
                    try {
                        Matcher matcher = pattern.matcher(line);
                        matcher.find();
                        Double diskRead = Double.parseDouble(matcher.group(1).replace(',', '.'));
                        Double diskWrite = Double.parseDouble(matcher.group(2).replace(',', '.'));

                        DiskUsage diskUsage = new DiskUsage(daemonId, diskRead, diskWrite, new Timestamp(new Date().getTime()));
                        synchronized (this) {
                            usageData.add(diskUsage);
                        }
                    } catch(Exception e) {
                    }
                }

            } catch(Exception e) {
                LOGGER.error("Error getting disk I/O", e);
            }

            LOGGER.info("Computing partition size");
            try {
                Process dfProcess = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "df | grep /dev/"});
                String dfOutput = IOUtils.toString(dfProcess.getInputStream(), "UTF-8");
                LOGGER.info("df output:\n" + dfOutput);

                Pattern pattern = Pattern.compile("([^\\s]+)\\s+(\\d+)\\s+(\\d+).*");

                for(String line : dfOutput.split("\n")) {
                    Matcher matcher = pattern.matcher(line);
                    matcher.find();
                    String name = matcher.group(1);
                    Double max = Double.parseDouble(matcher.group(2));
                    Double current = Double.parseDouble(matcher.group(3));

                    Partition partition = new Partition(daemonId, name, current, max, new Timestamp(new Date().getTime()));
                    synchronized (this) {
                        partitionData.add(partition);
                    }
                }
            } catch(Exception e) {
                LOGGER.error("Error getting partition info", e);
            }
        }
    }

    @Override
    public synchronized void saveLogs() {
        try {
            for(DiskUsage diskUsage : usageData) {
                if((diskUsage.getRead() >= getConfiguration().get("readMin") && diskUsage.getRead() <= getConfiguration().get("readMax"))
                        || (diskUsage.getWrite() >= getConfiguration().get("writeMin") && diskUsage.getWrite() <= getConfiguration().get("writeMax"))) {
                    diskDAO.insertUsage(diskUsage);
                }
            }

            for(Partition partition : partitionData) {
                diskDAO.insertPartition(partition);
            }
        } catch(Exception e) {
            LOGGER.error("Could not save logs in the database", e);
        }
    }

    @Override
    public Date getStartLoopTime() {
        return startLoopTime;
    }

    @Override
    public Integer getDaemonId() {
        try {
            try {
                Disk disk = diskDAO.getByServiceId(getServiceId());
                return (int) disk.getId();
            } catch (Exception e) {
                LOGGER.info("Could not get disk id from database", e);
            }

            Disk disk = new Disk(getServiceId(), "No description");
            diskDAO.insert(disk);

            disk = diskDAO.getByServiceId(getServiceId());
            return (int) disk.getId();

        } catch (Exception e) {
            LOGGER.error("Could not get disk id from database", e);
            throw new RuntimeException();
        }
    }

    public void setDiskDAO(DiskDAO diskDAO) {
        this.diskDAO = diskDAO;
    }
}
