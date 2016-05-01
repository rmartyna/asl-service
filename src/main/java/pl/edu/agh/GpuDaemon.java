package pl.edu.agh;

import org.apache.log4j.Logger;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

public class GpuDaemon extends Daemon {

    private Date startLoopTime;

    private Integer daemonId;

    private double temperature;

    private Integer fanSpeed;

    private static final Logger LOGGER = Logger.getLogger(GpuDaemon.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        daemonId = getDaemonId();
    }


    public void run() {
        while(true) {
            if(getEnabled()) {
                LOGGER.info("GPU loop start");
                startLoopTime = new Date();


                //TODO set temp, and fan speed
                LOGGER.info("Computing temperature and fan speed");
                temperature = 50;
                fanSpeed = 2000;

                LOGGER.info("Saving logs");
                saveLogs();
            }
            waitForNextLoop();
        }
    }

    public void saveLogs() {
        try {
            PreparedStatement saveTemp = getConnection().prepareStatement("INSERT INTO gpu_temp(gpu_id, value, date) VALUES(?,?,?)");
            saveTemp.setInt(1, daemonId);
            saveTemp.setDouble(2, temperature);
            saveTemp.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            saveTemp.executeUpdate();

            PreparedStatement saveFanSpeed = getConnection().prepareStatement("INSERT INTO gpu_fan(gpu_id, speed, date) VALUES(?,?,?)");
            saveFanSpeed.setInt(1, daemonId);
            saveFanSpeed.setInt(2, fanSpeed);
            saveFanSpeed.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            saveFanSpeed.executeUpdate();
        } catch(Exception e) {
            LOGGER.error("Could not save logs in the database", e);
        }
    }

    public Date getStartLoopTime() {
        return startLoopTime;
    }

    public Integer getDaemonId() {
        try {
            PreparedStatement getDaemonIdStatement = getConnection().prepareStatement("SELECT id FROM gpu WHERE service_id=" + getServiceId());
            try {
                ResultSet result = getDaemonIdStatement.executeQuery();
                result.next();
                return result.getInt(1);
            } catch (Exception e) {
                LOGGER.info("Could not get gpu id from database", e);
            }

            PreparedStatement putDaemonIdStatement = getConnection().prepareStatement("INSERT INTO gpu(service_id, description) VALUES(?, ?)");
            putDaemonIdStatement.setInt(1, getServiceId());
            putDaemonIdStatement.setString(2, "No GPU info");
            putDaemonIdStatement.executeUpdate();

            ResultSet result = getDaemonIdStatement.executeQuery();
            result.next();
            return result.getInt(1);
        } catch (Exception e) {
            LOGGER.error("Could not put gpu information into database", e);
            throw new RuntimeException(e);
        }
    }

}
