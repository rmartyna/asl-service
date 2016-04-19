package pl.edu.agh;

import org.springframework.beans.factory.InitializingBean;

import javax.naming.spi.InitialContextFactory;
import java.sql.Connection;
import java.util.Date;
import java.util.Map;

/**
 * Created by rmartyna on 18.04.16.
 */
public abstract class Daemon implements InitializingBean, Runnable {

    private Integer sleepTime;

    private String name;

    private Boolean enabled;

    private DbConnection dbConnection;

    private Connection connection;

    private Integer serviceId;

    public abstract void run();

    public abstract void configure(Map<String, String> configuration);

    public abstract void saveLogs();

    public abstract Date getStartLoopTime();

    public abstract Integer getDaemonId();

    @Override
    public void afterPropertiesSet() throws Exception {
        if (sleepTime == null)
            throw new IllegalArgumentException("Sleep time property cannot be null");

        if (name == null)
            throw new IllegalArgumentException("Name property cannot be null");

        if (enabled == null)
            throw new IllegalArgumentException("Enabled property cannot be null");

        if(dbConnection == null)
            throw new IllegalArgumentException("Db connection property cannot be null");

        connection = dbConnection.getConnection();
        serviceId = dbConnection.getServiceId();
    }

    public void waitForNextLoop() {
        try {
            long waitTime = getStartLoopTime().getTime() + sleepTime;
            while (System.currentTimeMillis() < waitTime)
                Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setSleepTime(Integer sleepTime) {
        this.sleepTime = sleepTime;
    }

    public Integer getSleepTime() {
        return sleepTime;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getEnabled() {
        return enabled;
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
}
