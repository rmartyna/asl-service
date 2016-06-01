package pl.edu.agh;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import pl.edu.agh.beans.Cpu;
import pl.edu.agh.beans.CpuFan;
import pl.edu.agh.beans.CpuTemp;
import pl.edu.agh.beans.CpuUsage;
import pl.edu.agh.dao.CpuDAO;

import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This software may be modified and distributed under the terms
 *  of the BSD license.  See the LICENSE.txt file for details.
 */

/**
 * Daemon that collects cpu logs
 */
public class CpuDaemon extends Daemon {

    private Date startLoopTime;

    private Integer daemonId;

    private CpuDAO cpuDAO;

    private List<CpuTemp> tempData = new ArrayList<CpuTemp>();

    private List<CpuFan> fanData = new ArrayList<CpuFan>();

    private List<CpuUsage> usageData = new ArrayList<CpuUsage>();

    private static final Logger LOGGER = Logger.getLogger(CpuDaemon.class);

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
            LOGGER.info("CPU loop start");
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
                Double[] temperatures = tempList.toArray(new Double[tempList.size()]);
                for(int i  = 0; i < temperatures.length; i++) {
                    CpuTemp cpuTemp = new CpuTemp(daemonId, i+1, temperatures[i], new Timestamp(new Date().getTime()));
                    synchronized (this) {
                        tempData.add(cpuTemp);
                    }
                }

            } catch(Exception e) {
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
                Integer fanSpeed = Integer.parseInt(matcher.group(1));

                CpuFan cpuFan = new CpuFan(daemonId, fanSpeed, new Timestamp(new Date().getTime()));
                synchronized (this) {
                    fanData.add(cpuFan);
                }
            } catch(Exception e) {
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
                        Double cpuUserUsage = Double.parseDouble(matcher.group(1).replace(',', '.'));
                        Double cpuSystemUsage = Double.parseDouble(matcher.group(2).replace(',', '.'));
                        Double cpuIowaitUsage = Double.parseDouble(matcher.group(3).replace(',', '.'));

                        CpuUsage cpuUsage = new CpuUsage(daemonId, cpuUserUsage, cpuSystemUsage, cpuIowaitUsage,
                                new Timestamp(new Date().getTime()));
                        synchronized (this) {
                            usageData.add(cpuUsage);
                        }
                    } catch (Exception e) {
                    }
                }
            } catch(Exception e) {
                LOGGER.error("Error getting CPU usage", e);
            }
        }
    }

    @Override
    public synchronized void saveLogs() {
        try {
            for(CpuFan cpuFan : fanData) {
                if(cpuFan.getSpeed() >= getConfiguration().get("fanMin") && cpuFan.getSpeed() <= getConfiguration().get("fanMax")) {
                    cpuDAO.insertFan(cpuFan);
                }
            }

            fanData = new ArrayList<CpuFan>();

            for(CpuTemp cpuTemp : tempData) {
                if(cpuTemp.getValue() >= getConfiguration().get("tempMin") && cpuTemp.getValue() <= getConfiguration().get("tempMax")) {
                    cpuDAO.insertTemp(cpuTemp);
                }
            }

            tempData = new ArrayList<CpuTemp>();

            for(CpuUsage cpuUsage : usageData) {
                Double overallUsage = cpuUsage.getIowait() + cpuUsage.getUser() + cpuUsage.getSystem();
                if(overallUsage >= getConfiguration().get("usageMin") && overallUsage <= getConfiguration().get("usageMax")) {
                    cpuDAO.insertUsage(cpuUsage);
                }
            }

            usageData = new ArrayList<CpuUsage>();

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
                Cpu cpu = cpuDAO.getByServiceId(getServiceId());
                return (int) cpu.getId();
            } catch (Exception e) {
                LOGGER.info("Could not get cpu id from database", e);
            }

            Cpu cpu = new Cpu(getServiceId(), "No description");
            cpuDAO.insert(cpu);

            cpu = cpuDAO.getByServiceId(getServiceId());
            return (int) cpu.getId();

        } catch (Exception e) {
            LOGGER.error("Could not get cpu id from database", e);
            throw new RuntimeException();
        }
    }

    public void setCpuDAO(CpuDAO cpuDAO) {
        this.cpuDAO = cpuDAO;
    }
}
