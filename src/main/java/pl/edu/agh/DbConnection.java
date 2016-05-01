package pl.edu.agh;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DbConnection implements InitializingBean {

    private Connection connection;

    private DataSource dataSource;

    private Integer port = 30303;

    private Integer serviceId;

    private static final Logger LOGGER = Logger.getLogger(DbConnection.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        if(dataSource == null)
            throw new IllegalArgumentException("Data source property cannot be null");

        connection = dataSource.getConnection();
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection getConnection() {
        return connection;
    }

    public Integer getServiceId() {
        if(serviceId != null)
            return serviceId;

        try {
            String address = InetAddress.getLocalHost().toString();

            LOGGER.info("address: " + address);
            LOGGER.info("port: " + port);


            PreparedStatement getServiceId = connection.prepareStatement("SELECT id FROM service WHERE host='" + address + "' AND PORT=" + port);
            try {
                ResultSet result = getServiceId.executeQuery();
                result.next();
                serviceId = result.getInt(1);
                return serviceId;
            } catch(Exception e) {
                LOGGER.info("Could not get service id from database", e);
            }

            PreparedStatement putServiceId = connection.prepareStatement("INSERT INTO service(host, port, description) VALUES(?, ?, ?)");
            putServiceId.setString(1, address);
            putServiceId.setInt(2, port);
            putServiceId.setString(3, "''");
            putServiceId.executeUpdate();

            ResultSet result = getServiceId.executeQuery();
            result.next();
            serviceId = result.getInt(1);
            return serviceId;
        } catch(Exception e) {
            LOGGER.error("Could not put service information into database", e);
            throw new RuntimeException();
        }
    }
}
