package pl.edu.agh;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import java.sql.Connection;
import java.util.*;

/**
 * This software may be modified and distributed under the terms
 *  of the BSD license.  See the LICENSE.txt file for details.
 */

public abstract class Daemon implements InitializingBean, Runnable {

    private String name;

    private DbConnector dbConnector;

    private Map<String, Integer> configuration = new HashMap<String, Integer>();

    protected boolean configured = false;

    private static final Logger LOGGER = Logger.getLogger(Daemon.class);

    public abstract void run();

    public abstract void getData();

    public abstract void saveLogs();

    public abstract Date getStartLoopTime();

    public abstract Integer getDaemonId();

    @Override
    public void afterPropertiesSet() throws Exception {

        if(name == null)
            throw new IllegalArgumentException("Name property cannot be null");

        if(dbConnector == null)
            throw new IllegalArgumentException("Db connector property cannot be null");

        if(configuration == null)
            throw new IllegalArgumentException("Configuration property cannot be null");
    }

    public void waitForNextLoop() {
        try {

            while (true) {
                long waitTime = getStartLoopTime().getTime() + getConfiguration().get("sleepTime");
                Thread.sleep(1000);
                if(System.currentTimeMillis() > waitTime)
                    break;
            }

        } catch (InterruptedException e) {
            LOGGER.error("Interrupted sleep", e);
        }
    }

    public synchronized void configure(Map<String, String> newConfiguration) throws IllegalArgumentException {
        LOGGER.info("Name: " + getName() + ", received configuration: " + newConfiguration);

        for(String attribute: newConfiguration.keySet()) {
            if(configuration.containsKey(attribute)) {
                try {
                    configuration.put(attribute, Integer.parseInt(newConfiguration.get(attribute)));
                } catch(Exception e) {
                    throw new IllegalArgumentException("Could not update attribute '" + attribute + "'.", e);
                }
            } else {
                throw new IllegalArgumentException("Attribute '" + attribute + "' is not configurable.");
            }
        }

        configured = true;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDbConnector(DbConnector dbConnector) {
        this.dbConnector = dbConnector;
    }

    public Integer getServiceId() {
        return dbConnector.getServiceId();
    }

    public Map<String, Integer> getConfiguration() { return configuration; }

    public void setConfiguration(Map<String, Integer> configuration) { this.configuration = configuration; }

}
