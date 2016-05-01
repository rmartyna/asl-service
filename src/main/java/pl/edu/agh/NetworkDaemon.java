package pl.edu.agh;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkDaemon extends Daemon {

    private Date startLoopTime;

    private Double download;

    private Double upload;

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
                LOGGER.info("Computing network in/out");
                try {
                    Process ifstatProcess = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "ifstat -Tb 1 1"});
                    String ifstatOutput = IOUtils.toString(ifstatProcess.getInputStream(), "UTF-8");
                    LOGGER.info("ifstat output:\n" + ifstatOutput);

                    Pattern pattern = Pattern.compile(".*\\s+([0-9\\.]+)\\s+([0-9\\.]+).*");
                    for(String line : ifstatOutput.split("\n")) {
                        try {
                            Matcher matcher = pattern.matcher(line);
                            matcher.find();
                            download = Double.parseDouble(matcher.group(1));
                            upload = Double.parseDouble(matcher.group(2));
                        } catch(Exception e) {
                        }
                    }

                    LOGGER.info("Result network download: " + download);
                    LOGGER.info("Result network upload: " + upload);

                } catch(IOException e) {
                    LOGGER.error("Error getting network I/O", e);
                }

                LOGGER.info("Saving logs");
                saveLogs();
            }
            waitForNextLoop();
        }
    }

    public void saveLogs() {
        try {
            if((download >= getConfiguration().get("downloadMin") && download <= getConfiguration().get("downloadMax"))
            || (upload >= getConfiguration().get("uploadMin") && upload <= getConfiguration().get("uploadMax"))) {
                PreparedStatement saveNetworkUsage = getConnection().prepareStatement("INSERT INTO network_usage(service_id, download, upload, date) VALUES(?,?,?,?)");
                saveNetworkUsage.setInt(1, getServiceId());
                saveNetworkUsage.setDouble(2, download);
                saveNetworkUsage.setDouble(3, upload);
                saveNetworkUsage.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                saveNetworkUsage.executeUpdate();
            }

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
