package pl.edu.agh;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by rmartyna on 18.04.16.
 */
public class DaemonMaster implements InitializingBean {

    private List<Daemon> daemons;

    private static final Logger LOGGER = Logger.getLogger(DaemonMaster.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.info("Starting daemons: ");
        for(Daemon daemon : daemons) {
            LOGGER.info("Starting " + daemon.getName());
            new Thread(daemon).run();
        }
    }

    public void configureSlaves(String configuration) {
        LOGGER.info("Configuring daemons");

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

        LOGGER.info("Configurations for daemons: ");
        for(String name : daemonNameToConfigurationMap.keySet())
            LOGGER.info(name + ": " + daemonNameToConfigurationMap.get(name));

        for (Daemon daemon : daemons) {
            Set<String> keySet = daemonNameToConfigurationMap.keySet();
            for (String key : keySet) {
                if (daemon.getName().equalsIgnoreCase(key))
                    daemon.configure(daemonNameToConfigurationMap.get(key));
            }
        }
    }

    public void setDaemons(List<Daemon> daemons) {
        this.daemons = daemons;
    }

}
