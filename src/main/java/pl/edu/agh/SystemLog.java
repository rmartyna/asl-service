package pl.edu.agh;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.sql.*;

public class SystemLog implements InitializingBean {

    private String filePath;

    private DbConnection dbConnection;

    private Connection connection;

    private Integer serviceId;

    private String lastFile;

    private String currentFile;

    private String difference;

    private Integer fileNumber;

    private static final Logger LOGGER = Logger.getLogger(SystemLog.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        if(dbConnection == null)
            throw new IllegalArgumentException("Db connection property cannot be null");

        if(filePath == null)
            throw new IllegalArgumentException("File path property cannot be null");

        File file = new File(filePath);
        if(!file.exists())
            throw new IllegalArgumentException("File '" + filePath + "' does not exist");

        connection = dbConnection.getConnection();
        serviceId = dbConnection.getServiceId();
    }

    public void gatherLogs() {
        if(lastFile == null)
            getLastFile();

        getCurrentFile();
        computeDifference();
    }

    private void getLastFile() {
        LOGGER.info("Getting last file");
        fileNumber = 1;
        lastFile = "";

        try {
            PreparedStatement getFileNumber = connection.prepareStatement("SELECT MAX(file_number) FROM system_logs WHERE service_id=? AND file_path=?");
            getFileNumber.setInt(1, serviceId);
            getFileNumber.setString(2, filePath);
            ResultSet resultSet = getFileNumber.executeQuery();
            resultSet.next();
            fileNumber = resultSet.getInt(1);
            LOGGER.info("File number: " + fileNumber);
        } catch(SQLException e) {
            LOGGER.error("Could not retrieve file number from database", e);
        }

        try {
            PreparedStatement getLastFile = connection.prepareStatement("SELECT log FROM system_logs WHERE service_id=? AND file_path=? AND file_number=? ORDER BY id");
            getLastFile.setInt(1, serviceId);
            getLastFile.setString(2, filePath);
            getLastFile.setInt(3, fileNumber);
            ResultSet resultSet = getLastFile.executeQuery();
            while(resultSet.next())
                lastFile += resultSet.getString(1);
        } catch(SQLException e) {
            LOGGER.error("Could not retrieve last file from database", e);
        }

    }

    private void getCurrentFile() {
        try {
            currentFile = new String(Files.readAllBytes(FileSystems.getDefault().getPath(filePath)));
        } catch(Exception e) {
            currentFile = "";
            LOGGER.error("Could not read file", e);
        }
    }

    private void computeDifference() {
        if(currentFile.startsWith(lastFile))
            difference = currentFile.substring(lastFile.length());
        else
            difference = currentFile;

        LOGGER.info("File difference: " + difference);

        lastFile = currentFile;
    }

    public void saveLogs() {
        if (difference.equals("")) {
            LOGGER.info("No new logs. Not saving in database");
            return;
        }

        try {
            PreparedStatement saveSystemLog = connection.prepareStatement("INSERT INTO system_logs(service_id, file_path, log, file_number, date) VALUES(?,?,?,?,?)");
            saveSystemLog.setInt(1, serviceId);
            saveSystemLog.setString(2, filePath);
            saveSystemLog.setString(3, difference);
            saveSystemLog.setInt(4, fileNumber);
            saveSystemLog.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            saveSystemLog.executeUpdate();

        } catch(Exception e) {
            LOGGER.error("Could not save logs in the database", e);
        }
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setDbConnection(DbConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public String getFilePath() {
        return filePath;
    }
}
