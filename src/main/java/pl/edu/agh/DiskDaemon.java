package pl.edu.agh;

import org.apache.log4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * Created by rmartyna on 21.04.16.
 */
public class DiskDaemon extends Daemon {

    private Date startLoopTime;

    private Integer daemonId;

    private Double diskRead;

    private Double diskWrite;

    private double[] partitionCurrent;

    private double[] partitionMax;

    private static final Logger LOGGER = Logger.getLogger(DiskDaemon.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        daemonId = getDaemonId();
    }


    public void run() {
        while(true) {
            if(getEnabled()) {
                LOGGER.info("Disk loop start");
                startLoopTime = new Date();

                //TODO set values
                LOGGER.info("Disk read/write speed and partitions current/max usage");
                diskRead = 20000.0;
                diskWrite = 10000.0;

                partitionCurrent = new double[] {1000.0, 2000.0};
                partitionMax = new double[] {100000.0, 200000.0};

                LOGGER.info("Saving logs");
                saveLogs();
            }
            waitForNextLoop();
        }
    }

    //TODO add configuration
    public void configure(Map<String, String> configuration) {
        LOGGER.info("Received configuration: " + configuration);
    }

    public void saveLogs() {
        try {
            PreparedStatement saveDiskUsage = getConnection().prepareStatement("INSERT INTO disk_usage(disk_id, read, write, date) VALUES(?,?,?,?)");
            saveDiskUsage.setInt(1, daemonId);
            saveDiskUsage.setDouble(2, diskRead);
            saveDiskUsage.setDouble(3, diskWrite);
            saveDiskUsage.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            saveDiskUsage.executeUpdate();

            PreparedStatement savePartitionUsage = getConnection().prepareStatement("INSERT INTO partition(disk_id, name, current, max, date) VALUES(?,?,?,?,?)");
            if(partitionMax.length != partitionCurrent.length) {
                LOGGER.error("Partition current length is " + partitionCurrent.length + ", while partition max length is "
                        + partitionMax.length + ". Could not save logs in the database");
            }
            for(int i = 0; i < partitionCurrent.length; i++) {
                savePartitionUsage.setInt(1, daemonId);
                savePartitionUsage.setString(2, "No name " + (i+1));
                savePartitionUsage.setDouble(3, partitionCurrent[i]);
                savePartitionUsage.setDouble(4, partitionMax[i]);
                savePartitionUsage.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                savePartitionUsage.executeUpdate();
            }
        } catch(Exception e) {
            LOGGER.error("Could not save logs in the database", e);
        }
    }

    public Date getStartLoopTime() {
        return startLoopTime;
    }

    public Integer getDaemonId() {
        try {
            PreparedStatement getDaemonIdStatement = getConnection().prepareStatement("SELECT id FROM disk WHERE service_id=" + getServiceId());
            try {
                ResultSet result = getDaemonIdStatement.executeQuery();
                result.next();
                return result.getInt(1);
            } catch (Exception e) {
                LOGGER.info("Could not get disk id from database", e);
            }

            PreparedStatement putDaemonIdStatement = getConnection().prepareStatement("INSERT INTO disk(service_id, description) VALUES(?, ?)");
            putDaemonIdStatement.setInt(1, getServiceId());
            putDaemonIdStatement.setString(2, "No disk info");
            putDaemonIdStatement.executeUpdate();

            ResultSet result = getDaemonIdStatement.executeQuery();
            result.next();
            return result.getInt(1);
        } catch (Exception e) {
            LOGGER.error("Could not put disk information into database", e);
            throw new RuntimeException(e);
        }
    }
}
