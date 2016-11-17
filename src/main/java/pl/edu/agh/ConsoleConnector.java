package pl.edu.agh;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Formatter;

/**
 * This software may be modified and distributed under the terms
 *  of the BSD license.  See the LICENSE.txt file for details.
 */

/**
 * Class that communicates with console
 */
public class ConsoleConnector implements InitializingBean, Runnable {

    private DaemonMaster daemonMaster;

    private DbConnector dbConnector;

    private ServerSocket serverSocket;

    private Integer daemonPort;

    private String host;

    private Integer port;

    private String password;

    private static final Logger LOGGER = Logger.getLogger(ConsoleConnector.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        serverSocket = new ServerSocket(daemonPort);
        new Thread(this).start();
    }

    /**
     * Runs in either pull or push mode
     */
    @Override
    public void run() {

        while(true) {
            LOGGER.info("Running console connector in mode: " + daemonMaster.getMode());
            if(daemonMaster.getMode().equalsIgnoreCase("pull")) {
                pull();
                try {
                    LOGGER.info("Poll rate: " + daemonMaster.getPollRate());
                    Thread.sleep(daemonMaster.getPollRate());
                } catch(Exception e) {
                    LOGGER.error("Interrupted sleep", e);
                }
            } else if(daemonMaster.getMode().equalsIgnoreCase("push")) {
                push();
            } else {
                LOGGER.error("Invalid mode: " + daemonMaster.getMode());
                System.exit(0);
            }
        }
    }

    /**
     * In push mode it waits for input from console
     */
    public void push() {
        try {
            Socket client = serverSocket.accept();

            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String message = in.readLine();
            DataOutputStream out = new DataOutputStream(client.getOutputStream());

            LOGGER.info("Server message: " + message);

            String[] data = message.trim().split("\\|");
            LOGGER.info("Data: " + Arrays.toString(data));
            comparePasswords(data[0]);

            tryToSaveData(data[1]);
            out.writeBytes("OK\r\n");

            in.close();
            out.close();
            client.close();

            daemonMaster.logData();
            daemonMaster.configure();

        } catch(Exception e) {
            LOGGER.error("Error connecting with server", e);
        }
    }

    /**
     * In pull mode it initiates communication with console
     */
    public void pull() {
        try {

            LOGGER.info("Connecting to " + host + ":" + port);
            Socket socket = new Socket(host, port);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));


            out.writeBytes(getPasswordHash(password) + "|" + dbConnector.getServiceId() + "\r\n");
            String result = in.readLine();

            in.close();
            out.close();
            socket.close();

            if(!result.trim().equals("OK"))
                throw new RuntimeException("Invalid response from server");

            daemonMaster.logData();
            daemonMaster.configure();

        } catch(Exception e) {
            LOGGER.error("Error connecting with server", e);
        }

    }

    private void tryToSaveData(String message) throws URISyntaxException, IOException {
        File dataFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() + "\\" + DbConnector.DATA_FILE_NAME);

        FileUtils.writeStringToFile(dataFile, message);

        dbConnector.setServiceId(Integer.parseInt(message));

    }

    private void comparePasswords(String consoleHash) {
        LOGGER.info("Console hash: " + consoleHash);
       String passwordHash = getPasswordHash(password);
        if(!passwordHash.equalsIgnoreCase(consoleHash))
            throw new RuntimeException("Password from console: " + consoleHash + ", does not match " +
                                "password from service: " + passwordHash);
    }

    private String getPasswordHash(String password) {
        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            String result = byteToHex(digest.digest(password.getBytes("UTF-8")));
            LOGGER.info("Password digest: " + result);
            return result;
        } catch(Exception e) {
            throw new RuntimeException("Could not generate digest", e);
        }
    }

    private static String byteToHex(byte[] digest) {
        Formatter formatter = new Formatter();
        for(byte b : digest) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    public void setDaemonMaster(DaemonMaster daemonMaster) {
        this.daemonMaster = daemonMaster;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setDaemonPort(Integer daemonPort) {
        this.daemonPort = daemonPort;
    }

    public void setDbConnector(DbConnector dbConnector) {
        this.dbConnector = dbConnector;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
