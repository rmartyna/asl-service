package pl.edu.agh;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import pl.edu.agh.beans.Service;
import pl.edu.agh.dao.ServiceDAO;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * This software may be modified and distributed under the terms
 *  of the BSD license.  See the LICENSE.txt file for details.
 */

/**
 * Utility class that gets service ID from database
 */
public class DbConnector implements InitializingBean {

    private ServiceDAO serviceDAO;

    private Integer serviceId;

    public static final String DATA_FILE_NAME = "service.data";

    private static final Logger LOGGER = Logger.getLogger(DbConnection.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        if(serviceDAO == null)
            throw new IllegalArgumentException("Service DAO property cannot be null");

        readFile();
    }

    /**
     * Returns ID of this service
     */
    public Integer getServiceId() {
        if (serviceId != null)
            return serviceId;
        else
            throw new IllegalArgumentException("No service id read from file/database");
    }

    private void readFile() throws IOException, URISyntaxException {
        File dataFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() + "\\" + DATA_FILE_NAME);

        if(dataFile.isFile()) {
            String data = FileUtils.readFileToString(dataFile);
            serviceId = Integer.parseInt(data);
        } else
            LOGGER.warn("No data file");
    }

    public void setServiceDAO(ServiceDAO serviceDAO) {
        this.serviceDAO = serviceDAO;
    }

    public void setServiceId(Integer serviceId) {
        this.serviceId = serviceId;
    }
}
