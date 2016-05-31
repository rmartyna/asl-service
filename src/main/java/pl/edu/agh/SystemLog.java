package pl.edu.agh;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import pl.edu.agh.dao.SystemLogsDAO;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * This software may be modified and distributed under the terms
 *  of the BSD license.  See the LICENSE.txt file for details.
 */

/**
 * Collects logs from particular file
 */
public class SystemLog implements InitializingBean {

    private String filePath;

    private Integer serviceId;

    private String lastFile;

    private String currentFile;

    private List<pl.edu.agh.beans.SystemLog> systemLogs = new ArrayList<pl.edu.agh.beans.SystemLog>();

    private Integer fileNumber;

    private SystemLogsDAO systemLogsDAO;

    private static final Logger LOGGER = Logger.getLogger(SystemLog.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        if(filePath == null)
            throw new IllegalArgumentException("File path property cannot be null");

        File file = new File(filePath);
        if(!file.exists())
            throw new IllegalArgumentException("File '" + filePath + "' does not exist");
    }

    /**
     * Gathers logs
     */
    public void gatherLogs() {
        if(lastFile == null) {
            LOGGER.error("Last file cannot be null");
            return;
        }

        getCurrentFile();
        computeDifference();
    }

    /**
     * Gets log file from database that was collected last time
     */
    public void getLastFile() {
        if(lastFile != null)
            return;
        LOGGER.info("Getting last file");
        fileNumber = 1;
        lastFile = "";

        try {
            fileNumber = systemLogsDAO.getMaxFileNumber(serviceId, filePath);
            List<pl.edu.agh.beans.SystemLog> systemLogs = systemLogsDAO.getSystemLogList(serviceId, filePath, fileNumber);
            for(pl.edu.agh.beans.SystemLog systemLog : systemLogs)
                lastFile += systemLog.getLog();
        } catch (Exception e) {
            LOGGER.error("Could not retrieve last file from database", e);
        }
    }

    /**
     * Gets current log file from file system
     */
    private void getCurrentFile() {
        try {
            currentFile = new String(Files.readAllBytes(FileSystems.getDefault().getPath(filePath)));
        } catch(Exception e) {
            currentFile = "";
            LOGGER.error("Could not read file", e);
        }
    }

    /**
     * Computes difference between current and last log and stores result in systemLogs list.
     * Increments file number if necessary. Sets last log file to current.
     */
    private void computeDifference() {
        pl.edu.agh.beans.SystemLog systemLog = null;
        if(currentFile.startsWith(lastFile)) {
            systemLog = new pl.edu.agh.beans.SystemLog(serviceId, filePath,
                    currentFile.substring(lastFile.length()), fileNumber, new Timestamp(new Date().getTime()));
        }
        else {
            fileNumber++;
            systemLog = new pl.edu.agh.beans.SystemLog(serviceId, filePath,
                    currentFile, fileNumber, new Timestamp(new Date().getTime()));
        }

        synchronized (this) {
            systemLogs.add(systemLog);
        }


        lastFile = currentFile;
    }

    /**
     * Saves logs in database and clears systemLogs list
     */
    public synchronized void saveLogs() {
        try {
            for(pl.edu.agh.beans.SystemLog systemLog : systemLogs) {
                systemLogsDAO.insert(systemLog);
            }
            systemLogs = new ArrayList<pl.edu.agh.beans.SystemLog>();
        } catch(Exception e) {
            LOGGER.error("Could not save logs in the database", e);
        }
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setSystemLogsDAO(SystemLogsDAO systemLogsDAO) {
        this.systemLogsDAO = systemLogsDAO;
    }

    public void setServiceId(Integer serviceId) {
        this.serviceId = serviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SystemLog)) return false;

        SystemLog systemLog = (SystemLog) o;

        return filePath != null ? filePath.equals(systemLog.filePath) : systemLog.filePath == null;

    }

    @Override
    public int hashCode() {
        return filePath != null ? filePath.hashCode() : 0;
    }
}
