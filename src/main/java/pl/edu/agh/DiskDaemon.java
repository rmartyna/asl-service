package pl.edu.agh;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TODO current implementation assumes that there is only one disk
public class DiskDaemon extends Daemon {

    private Date startLoopTime;

    private Integer daemonId;

    private Double diskRead;

    private Double diskWrite;

    private Double[] partitionCurrent;

    private Double[] partitionMax;

    private String[] partitionName;

    private static final Logger LOGGER = Logger.getLogger(DiskDaemon.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        daemonId = getDaemonId();

    }


    public void run() {
        while(true) {
            startLoopTime = new Date();
            if(getConfiguration().get("enabled") != 0) {
                LOGGER.info("Disk loop start");

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
                            diskRead = Double.parseDouble(matcher.group(1).replace(',', '.'));
                            diskWrite = Double.parseDouble(matcher.group(2).replace(',', '.'));
                        } catch(Exception e) {
                        }
                    }

                    LOGGER.info("Result disk read: " + diskRead);
                    LOGGER.info("Result disk write: " + diskWrite);

                } catch(IOException e) {
                    LOGGER.error("Error getting disk I/O", e);
                }

                LOGGER.info("Computing partition size");
                try {
                    Process dfProcess = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "df | grep /dev/"});
                    String dfOutput = IOUtils.toString(dfProcess.getInputStream(), "UTF-8");
                    LOGGER.info("df output:\n" + dfOutput);

                    Pattern pattern = Pattern.compile("([^\\s]+)\\s+(\\d+)\\s+(\\d+).*");

                    ArrayList<Double> currentList = new ArrayList<Double>();
                    ArrayList<Double> maxList = new ArrayList<Double>();
                    ArrayList<String> nameList = new ArrayList<String>();
                    for(String line : dfOutput.split("\n")) {
                        Matcher matcher = pattern.matcher(line);
                        matcher.find();
                        nameList.add(matcher.group(1));
                        maxList.add(Double.parseDouble(matcher.group(2)));
                        currentList.add(Double.parseDouble(matcher.group(3)));
                    }

                    partitionCurrent = currentList.toArray(new Double[currentList.size()]);
                    partitionMax = maxList.toArray(new Double[maxList.size()]);
                    partitionName = nameList.toArray(new String[nameList.size()]);

                    LOGGER.info("Partition name: " + Arrays.toString(partitionName));
                    LOGGER.info("Partition current: " + Arrays.toString(partitionCurrent));
                    LOGGER.info("Partition max: " + Arrays.toString(partitionMax));
                } catch(IOException e) {
                    LOGGER.error("Error getting partition info", e);
                }

                LOGGER.info("Saving logs");
                saveLogs();
            }
            waitForNextLoop();
        }
    }

    public void saveLogs() {
        try {

            if((diskRead >= getConfiguration().get("readMin") && diskRead <= getConfiguration().get("readMax"))
            || (diskWrite >= getConfiguration().get("writeMin") && diskWrite <= getConfiguration().get("writeMax"))) {
                PreparedStatement saveDiskUsage = getConnection().prepareStatement("INSERT INTO disk_usage(disk_id, read, write, date) VALUES(?,?,?,?)");

                saveDiskUsage.setInt(1, daemonId);
                saveDiskUsage.setDouble(2, diskRead);
                saveDiskUsage.setDouble(3, diskWrite);
                saveDiskUsage.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                saveDiskUsage.executeUpdate();
            }


            PreparedStatement savePartitionUsage = getConnection().prepareStatement("INSERT INTO partition(disk_id, name, current, max, date) VALUES(?,?,?,?,?)");
            if(partitionMax.length != partitionCurrent.length || partitionMax.length != partitionName.length) {
                LOGGER.error("Partition current length is " + partitionCurrent.length + ", partition max length is "
                        + partitionMax.length + ", partition name length is " + partitionName.length
                        + ". Could not save logs in the database");
            }
            for(int i = 0; i < partitionCurrent.length; i++) {
                savePartitionUsage.setInt(1, daemonId);
                savePartitionUsage.setString(2, partitionName[i]);
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

    @Override
    public String operation(String name, String value) {
        throw new IllegalArgumentException("No available operations");
    }
}
