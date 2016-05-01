package pl.edu.agh;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.pattern.IntegerPatternConverter;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CpuDaemon extends Daemon {

    private Date startLoopTime;

    private Integer daemonId;

    private Double[] temperatures;

    private Integer fanSpeed;

    private Double cpuUserUsage;

    private Double cpuSystemUsage;

    private Double cpuIowaitUsage;

    private static final Logger LOGGER = Logger.getLogger(CpuDaemon.class);

    //TODO initialize configuration map with spring in bean with parameters from config file
    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        daemonId = getDaemonId();

    }


    public void run() {
        while(true) {
            if(getConfiguration().get("enabled") != 0) {
                LOGGER.info("CPU loop start");
                startLoopTime = new Date();

                LOGGER.info("Computing temperature of CPU cores");
                try {
                    Process sensorsProcess = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "sensors | grep \"core [0-9]\" -i"});
                    String sensorsOutput = IOUtils.toString(sensorsProcess.getInputStream(), "UTF-8");
                    LOGGER.info("Sensors output for CPU temperature:\n" + sensorsOutput);

                    Pattern pattern = Pattern.compile(".*\\+([0-9]+\\.[0-9]+).*\\(.*");
                    List<Double> tempList = new ArrayList<Double>();
                    for(String line : sensorsOutput.split("\n")) {
                        Matcher matcher = pattern.matcher(line);
                        matcher.find();
                        tempList.add(Double.parseDouble(matcher.group(1)));
                    }
                    temperatures = tempList.toArray(new Double[tempList.size()]);
                    LOGGER.info("Result temperature: " + Arrays.toString(temperatures));

                } catch(IOException e) {
                    LOGGER.error("Error getting CPU temperature", e);
                }

                LOGGER.info("Computing speed of CPU fan");
                try {
                    Process sensorsProcess = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "sensors | grep \"fan\" -i"});
                    String sensorsOutput = IOUtils.toString(sensorsProcess.getInputStream(), "UTF-8");
                    LOGGER.info("Sensors output for CPU fan speed:\n" + sensorsOutput);

                    Pattern pattern = Pattern.compile(".*\\s+(\\d+) RPM.*");
                    Matcher matcher = pattern.matcher(sensorsOutput);
                    matcher.find();
                    fanSpeed = Integer.parseInt(matcher.group(1));
                    LOGGER.info("Result fan speed: " + fanSpeed);
                } catch(IOException e) {
                    LOGGER.error("Error getting CPU fan speed", e);
                }

                LOGGER.info("Computing cpu usage");
                try {
                    Process iostatProcess = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "iostat -yc 1 1"});
                    String iostatOutput = IOUtils.toString(iostatProcess.getInputStream(), "UTF-8");
                    LOGGER.info("iostat disk output:\n" + iostatOutput);

                    Pattern pattern = Pattern.compile("\\s+([0-9,]+)\\s+[0-9,]+\\s+([0-9,]+)\\s+([0-9,]+).*");
                    for (String line : iostatOutput.split("\n")) {
                        try {
                            Matcher matcher = pattern.matcher(line);
                            matcher.find();
                            cpuUserUsage = Double.parseDouble(matcher.group(1).replace(',', '.'));
                            cpuSystemUsage = Double.parseDouble(matcher.group(2).replace(',', '.'));
                            cpuIowaitUsage = Double.parseDouble(matcher.group(3).replace(',', '.'));

                            LOGGER.info("CPU user usage: " + cpuUserUsage);
                            LOGGER.info("CPU system usage: " + cpuSystemUsage);
                            LOGGER.info("CPU iowait usage: " + cpuIowaitUsage);
                        } catch (Exception e) {
                        }
                    }
                } catch(IOException e) {
                    LOGGER.error("Error getting CPU usage", e);
                }

                LOGGER.info("Saving logs");
                saveLogs();
            }
            waitForNextLoop();
        }
    }

    public void saveLogs() {
        try {
            PreparedStatement saveTemp = getConnection().prepareStatement("INSERT INTO cpu_temp(cpu_id, core, value, date) VALUES(?,?,?,?)");
            saveTemp.setInt(1, daemonId);
            for(int i = 0; i < temperatures.length; i++) {
                if(temperatures[i] >= getConfiguration().get("tempMin") && temperatures[i] <= getConfiguration().get("tempMax")) {
                    saveTemp.setInt(2, i+1);
                    saveTemp.setDouble(3, temperatures[i]);
                    saveTemp.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    saveTemp.executeUpdate();
                }
            }

            if(fanSpeed >= getConfiguration().get("fanMin") && fanSpeed <= getConfiguration().get("fanMax")) {
                PreparedStatement saveFanSpeed = getConnection().prepareStatement("INSERT INTO cpu_fan(cpu_id, speed, date) VALUES(?,?,?)");
                saveFanSpeed.setInt(1, daemonId);
                saveFanSpeed.setInt(2, fanSpeed);
                saveFanSpeed.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                saveFanSpeed.executeUpdate();
            }

            Double overallUsage = cpuIowaitUsage + cpuSystemUsage + cpuUserUsage;
            if(overallUsage >= getConfiguration().get("usageMin") && overallUsage <= getConfiguration().get("usageMax")) {
                PreparedStatement saveUsage = getConnection().prepareStatement("INSERT INTO cpu_usage(cpu_id, \"user\", system, iowait, date) VALUES(?,?,?,?,?)");
                saveUsage.setInt(1, daemonId);
                saveUsage.setDouble(2, cpuUserUsage);
                saveUsage.setDouble(3, cpuSystemUsage);
                saveUsage.setDouble(4, cpuIowaitUsage);
                saveUsage.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                saveUsage.executeUpdate();
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
            PreparedStatement getDaemonIdStatement = getConnection().prepareStatement("SELECT id FROM cpu WHERE service_id=" + getServiceId());
            try {
                ResultSet result = getDaemonIdStatement.executeQuery();
                result.next();
                return result.getInt(1);
            } catch (Exception e) {
                LOGGER.info("Could not get cpu id from database", e);
            }

            PreparedStatement putDaemonIdStatement = getConnection().prepareStatement("INSERT INTO cpu(service_id, description) VALUES(?, ?)");
            putDaemonIdStatement.setInt(1, getServiceId());
            putDaemonIdStatement.setString(2, "No CPU info");
            putDaemonIdStatement.executeUpdate();

            ResultSet result = getDaemonIdStatement.executeQuery();
            result.next();
            return result.getInt(1);
        } catch (Exception e) {
            LOGGER.error("Could not put cpu information into database", e);
            throw new RuntimeException(e);
        }
    }
}
