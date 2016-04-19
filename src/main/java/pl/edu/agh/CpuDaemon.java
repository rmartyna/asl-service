package pl.edu.agh;

import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by rmartyna on 18.04.16.
 */
public class CpuDaemon extends Daemon {

    private Date startLoopTime;

    private Integer daemonId;

    private double temperature;

    private Integer fanSpeed;

    private static final Logger LOGGER = Logger.getLogger(CpuDaemon.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        daemonId = getDaemonId();
    }

    public void run() {
        while(true) {
            if(getEnabled()) {
                LOGGER.info("Loop start");
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

    public void configure(Map<String, String> configuration) {
        LOGGER.info("Received configuration: " + configuration);
    }

    public void saveLogs() {
        try {
            PreparedStatement saveTemp = getConnection().prepareStatement("INSERT INTO cpu_temp(cpu_id, core, value) VALUES(?,?,?)");
            saveTemp.setInt(1, daemonId);
            saveTemp.setInt(2, 1);
            saveTemp.setDouble(3, temperature);
            saveTemp.executeUpdate();

            PreparedStatement saveFanSpeed = getConnection().prepareStatement("INSERT INTO cpu_fan(cpu_id, speed) VALUES(?,?)");
            saveFanSpeed.setInt(1, daemonId);
            saveFanSpeed.setInt(2, fanSpeed);
            saveFanSpeed.executeUpdate();
        } catch(Exception e) {
            LOGGER.error("Could not save logs in ot the database", e);
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
            putDaemonIdStatement.setString(2, "''");
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
