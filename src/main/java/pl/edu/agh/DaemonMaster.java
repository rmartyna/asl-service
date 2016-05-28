package pl.edu.agh;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DaemonMaster implements InitializingBean {

    private List<Daemon> daemons;

    private String mode;

    private Integer pollRate;

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

    public void logData() {
        for(Daemon daemon : daemons)
            daemon.saveLogs();
    }

    //TODO load configuration from database instead of parsing
    public void configure() {
        LOGGER.info("Configuring: ");

    }

    public void configureSlaves(String configuration) throws IllegalArgumentException {
        LOGGER.info("Configuring daemons: ");

        Map<String, Map<String, String>> daemonNameToConfigurationMap
                = new HashMap<String, Map<String, String>>();

        String[] properties = configuration.split(";");
        for (String p : properties) {
            String[] property = p.split(",");
            if (daemonNameToConfigurationMap.containsKey(property[0]))
                daemonNameToConfigurationMap.get(property[0]).put(property[1], property[2]);
            else {
                Map<String, String> propertyMap = new HashMap<String, String>();
                propertyMap.put(property[1], property[2]);
                daemonNameToConfigurationMap.put(property[0], propertyMap);
            }
        }

        for(String name : daemonNameToConfigurationMap.keySet())
            LOGGER.info(name + ": " + daemonNameToConfigurationMap.get(name));


        for(String key: daemonNameToConfigurationMap.keySet()) {
            boolean configured = false;
            for (Daemon daemon : daemons) {
                if (daemon.getName().equalsIgnoreCase(key)) {
                    daemon.configure(daemonNameToConfigurationMap.get(key));
                    configured = true;
                    break;
                }
            }
            if(!configured)
                throw new IllegalArgumentException("Invalid daemon name: " + key);
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
}
