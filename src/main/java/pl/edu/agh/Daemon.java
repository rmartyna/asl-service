package pl.edu.agh;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import java.sql.Connection;
import java.util.*;

public abstract class Daemon implements InitializingBean, Runnable {

    private String name;

    private DbConnection dbConnection;

    private Connection connection;

    private Integer serviceId;

    private Map<String, Integer> configuration = new HashMap<String, Integer>();

    private List<String> operations = new LinkedList<String>();

    private static final Logger LOGGER = Logger.getLogger(Daemon.class);

    public abstract void run();

    public abstract void saveLogs();

    public abstract Date getStartLoopTime();

    public abstract Integer getDaemonId();

    public abstract String operation(String name, String value);

    @Override
    public void afterPropertiesSet() throws Exception {

        if (name == null)
            throw new IllegalArgumentException("Name property cannot be null");

        if(dbConnection == null)
            throw new IllegalArgumentException("Db connection property cannot be null");

        if(configuration == null)
            throw new IllegalArgumentException("Configuration property cannot be null");

        if(configuration.get("sleepTime") == null)
            throw new IllegalArgumentException("Configuration must contain sleepTime parameter");

        if(configuration.get("enabled") == null)
            throw new IllegalArgumentException("Configuration must contain enabled parameter");

        connection = dbConnection.getConnection();
        serviceId = dbConnection.getServiceId();
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
            e.printStackTrace();
        }
    }

    public void configure(Map<String, String> newConfiguration) throws IllegalArgumentException {
        LOGGER.info("Name: " + getName() + ", received configuration: " + newConfiguration);

        for(String attribute: newConfiguration.keySet()) {
            if(operations.contains(attribute)) {
                operation(attribute, newConfiguration.get(attribute));
            } else if(configuration.containsKey(attribute)) {
                try {
                    configuration.put(attribute, Integer.parseInt(newConfiguration.get(attribute)));
                } catch(Exception e) {
                    throw new IllegalArgumentException("Could not update attribute '" + attribute + "'.", e);
                }
            } else {
                throw new IllegalArgumentException("Attribute '" + attribute + "' is not configurable.");
            }
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDbConnection(DbConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public Connection getConnection() {
        return connection;
    }

    public Integer getServiceId() {
        return serviceId;
    }

    public Map<String, Integer> getConfiguration() { return configuration; }

    public void setConfiguration(Map<String, Integer> configuration) { this.configuration = configuration; }

    public List<String> getOperations() { return operations; }

    public void setOperations(List<String> operations) { this.operations = operations; }

    public DbConnection getDbConnection() { return dbConnection; }
}
