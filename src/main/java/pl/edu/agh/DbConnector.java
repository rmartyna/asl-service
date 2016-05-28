package pl.edu.agh;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import pl.edu.agh.beans.Service;
import pl.edu.agh.dao.ServiceDAO;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DbConnector implements InitializingBean {

    private ServiceDAO serviceDAO;

    private String host;

    private Integer port;

    private Integer serviceId;

    private static final Logger LOGGER = Logger.getLogger(DbConnection.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        if(serviceDAO == null)
            throw new IllegalArgumentException("Service DAO property cannot be null");

        if(host == null)
            throw new IllegalArgumentException("Host property cannot be null");

        if(port == null)
            throw new IllegalArgumentException("Port property cannot be null");
    }

    public Integer getServiceId() {
        if (serviceId != null)
            return serviceId;

        try {
            try {
                Service service = serviceDAO.getByHostAndPort(host, port);
                serviceId =  (int) service.getId();
            } catch (Exception e) {
                LOGGER.info("Could not get service id from database", e);
            }

            String description = InetAddress.getLocalHost().toString();
            Service service = new Service(host, port, description);
            serviceDAO.insert(service);

            service = serviceDAO.getByHostAndPort(host, port);
            serviceId = (int) service.getId();
            return serviceId;

        } catch (Exception e) {
            LOGGER.error("Could not put service information into database", e);
            throw new RuntimeException();
        }
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setServiceDAO(ServiceDAO serviceDAO) {
        this.serviceDAO = serviceDAO;
    }
}
