package pl.edu.agh;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import pl.edu.agh.beans.Network;
import pl.edu.agh.dao.NetworkDAO;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This software may be modified and distributed under the terms
 *  of the BSD license.  See the LICENSE.txt file for details.
 */

public class NetworkDaemon extends Daemon {

    private Date startLoopTime;

    private List<Network> data = new ArrayList<Network>();

    private NetworkDAO networkDAO;

    private static final Logger LOGGER = Logger.getLogger(NetworkDaemon.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }


    public void run() {
        while(true) {
            LOGGER.info("Network loop start");
            startLoopTime = new Date();
            if(configured) {
                getData();
            }
            waitForNextLoop();
        }
    }

    public void getData() {
        if(getConfiguration().get("enabled") != 0) {
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
                        Double download = Double.parseDouble(matcher.group(1));
                        Double upload = Double.parseDouble(matcher.group(2));

                        Network network = new Network(getServiceId(), download, upload, new Timestamp(new Date().getTime()));
                        synchronized (this) {
                            data.add(network);
                        }
                    } catch(Exception e) {
                    }
                }

            } catch(Exception e) {
                LOGGER.error("Error getting network I/O", e);
            }
        }
    }

    public synchronized void saveLogs() {
        try {
            for(Network network : data) {
                if((network.getDownload() >= getConfiguration().get("downloadMin") && network.getDownload() <= getConfiguration().get("downloadMax"))
                        || (network.getUpload() >= getConfiguration().get("uploadMin") && network.getUpload() <= getConfiguration().get("uploadMax"))) {
                    networkDAO.insert(network);
                }
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

    public void setNetworkDAO(NetworkDAO networkDAO) {
        this.networkDAO = networkDAO;
    }
}
