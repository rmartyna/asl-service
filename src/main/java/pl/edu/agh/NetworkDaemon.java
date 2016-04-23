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
public class NetworkDaemon extends Daemon {

    private Date startLoopTime;

    private double download;

    private double upload;

    private static final Logger LOGGER = Logger.getLogger(NetworkDaemon.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }


    public void run() {
        while(true) {
            if(getEnabled()) {
                LOGGER.info("Network loop start");
                startLoopTime = new Date();

                //TODO set values
                LOGGER.info("Computing network download/upload");
                download = 1000.0;
                upload = 200.0;

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
            PreparedStatement saveNetworkUsage = getConnection().prepareStatement("INSERT INTO network_usage(service_id, download, upload, date) VALUES(?,?,?,?)");
            saveNetworkUsage.setInt(1, getServiceId());
            saveNetworkUsage.setDouble(2, download);
            saveNetworkUsage.setDouble(3, upload);
            saveNetworkUsage.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            saveNetworkUsage.executeUpdate();

        } catch(Exception e) {
            LOGGER.error("Could not save logs in the database", e);
        }
    }

    public Date getStartLoopTime() {
        return startLoopTime;
    }

    public Integer getDaemonId() {
        LOGGER.error("getDaemonId method is not implemented for NetworkDaemon");
        return null;
    }

}
