package pl.edu.agh;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import pl.edu.agh.beans.ConsoleConfiguration;
import pl.edu.agh.beans.Service;
import pl.edu.agh.beans.ServiceConfiguration;
import pl.edu.agh.dao.ConsoleConfigurationDAO;
import pl.edu.agh.dao.ServiceConfigurationDAO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This software may be modified and distributed under the terms
 *  of the BSD license.  See the LICENSE.txt file for details.
 */

/**
 * Manages all daemons
 */
public class DaemonMaster implements InitializingBean {

    private List<Daemon> daemons;

    private String mode;

    private Integer pollRate;

    private ServiceConfigurationDAO serviceConfigurationDAO;

    private DbConnector dbConnector;

    private static final Logger LOGGER = Logger.getLogger(DaemonMaster.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.info("Starting daemons: ");
        ExecutorService threadPool = Executors.newCachedThreadPool();
        for(Daemon daemon : daemons) {
            LOGGER.info("Executing " + daemon.getName());
            threadPool.execute(daemon);
        }

    }

    /**
     * Saves logs for all daemons
     */
    public void logData() {
        for(Daemon daemon : daemons)
            daemon.saveLogs();
    }

    /**
     * Gets service configuration and configures daemons
     */
    public void configure() {
        ServiceConfiguration serviceConfiguration = getServiceConfiguration();

        pollRate = serviceConfiguration.getPollRate();
        mode = serviceConfiguration.getMode();

        Map<String, String> cpuConf = new HashMap<String, String>();
        cpuConf.put("sleepTime", Integer.toString(serviceConfiguration.getCpuFrequency()));
        cpuConf.put("enabled", Integer.toString(serviceConfiguration.getCpuEnabled()));

        Map<String, String> diskConf = new HashMap<String, String>();
        diskConf.put("sleepTime", Integer.toString(serviceConfiguration.getDiskFrequency()));
        diskConf.put("enabled", Integer.toString(serviceConfiguration.getDiskEnabled()));

        Map<String, String> memoryConf = new HashMap<String, String>();
        memoryConf.put("sleepTime", Integer.toString(serviceConfiguration.getMemoryFrequency()));
        memoryConf.put("enabled", Integer.toString(serviceConfiguration.getMemoryEnabled()));

        Map<String, String> networkConf = new HashMap<String, String>();
        networkConf.put("sleepTime", Integer.toString(serviceConfiguration.getNetworkFrequency()));
        networkConf.put("enabled", Integer.toString(serviceConfiguration.getNetworkEnabled()));

        Map<String, String> syslogConf = new HashMap<String, String>();
        syslogConf.put("sleepTime", Integer.toString(serviceConfiguration.getSyslogFrequency()));
        syslogConf.put("enabled", Integer.toString(serviceConfiguration.getSyslogEnabled()));
        syslogConf.put("list", serviceConfiguration.getSyslogList());

        for(Daemon daemon : daemons) {
            if(daemon.getName().equals("cpu"))
                daemon.configure(cpuConf);
            if(daemon.getName().equals("disk"))
                daemon.configure(diskConf);
            if(daemon.getName().equals("network"))
                daemon.configure(networkConf);
            if(daemon.getName().equals("memory"))
                daemon.configure(memoryConf);
            if(daemon.getName().equals("syslog"))
                daemon.configure(syslogConf);
        }
    }

    /**
     * Returns configuration for this server. In none was found in database then creates new one.
     */
    private ServiceConfiguration getServiceConfiguration() {
        try {
            return serviceConfigurationDAO.getByServiceId(dbConnector.getServiceId());
        } catch (Exception e) {
            LOGGER.error("Could not get service configuration from database", e);
            System.exit(0);
            return null;
        }
    }

    public void setDaemons(List<Daemon> daemons) {
        this.daemons = daemons;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Integer getPollRate() {
        return pollRate;
    }

    public void setPollRate(Integer pollRate) {
        this.pollRate = pollRate;
    }

    public void setServiceConfigurationDAO(ServiceConfigurationDAO serviceConfigurationDAO) {
        this.serviceConfigurationDAO = serviceConfigurationDAO;
    }

    public void setDbConnector(DbConnector dbConnector) {
        this.dbConnector = dbConnector;
    }
}
