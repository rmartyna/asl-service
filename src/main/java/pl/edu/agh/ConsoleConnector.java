package pl.edu.agh;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This software may be modified and distributed under the terms
 *  of the BSD license.  See the LICENSE.txt file for details.
 */

public class ConsoleConnector implements InitializingBean, Runnable {

    private DaemonMaster daemonMaster;

    private DbConnector dbConnector;

    private ServerSocket serverSocket;

    private Integer daemonPort;

    private String host;

    private Integer port;

    private static final Logger LOGGER = Logger.getLogger(ConsoleConnector.class);

    public void afterPropertiesSet() throws Exception {
        serverSocket = new ServerSocket(daemonPort);
        new Thread(this).start();
    }

    public void run() {

        while(true) {
            LOGGER.info("Running console connector im mode: " + daemonMaster.getMode());
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
            } else
                throw new RuntimeException("Invalid mode: " + daemonMaster.getMode());
        }
    }

    public void push() {
        while(true) {
            try {
                Socket client = serverSocket.accept();

                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String message = in.readLine();
                DataOutputStream out = new DataOutputStream(client.getOutputStream());

                LOGGER.info("Server message: " + message);
                if(message.equalsIgnoreCase("data")) {
                    try {
                        daemonMaster.logData();
                        out.writeBytes("OK\r\n");
                    } catch(Exception e) {
                        LOGGER.error("Error logging data", e);
                        out.writeBytes("ERROR \r\n");
                    }
                }
                else if(message.equalsIgnoreCase("conf")) {
                    try {
                        daemonMaster.configure();
                        out.writeBytes("OK\r\n");
                        return;
                    } catch(Exception e) {
                        LOGGER.error("Error configuring data", e);
                        out.writeBytes("ERROR \r\n");
                    }
                } else if(message.equalsIgnoreCase("remove")) {
                    out.writeBytes("OK\r\n");
                    out.close();
                    System.exit(0);
                }
                else {
                    LOGGER.error("Invalid message");
                }

                in.close();
                out.close();
                client.close();

            } catch(Exception e) {
                LOGGER.error("Error connecting with server", e);
            }
        }
    }

    public void pull() {
        try {
            LOGGER.info("Connecting to " + host + ":" + port);
            Socket socket = new Socket(host, port);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));


            out.writeBytes("" + dbConnector.getServiceId().toString() + "\r\n");
            String result = in.readLine();

            in.close();
            out.close();
            socket.close();

            socket = new Socket(host, port);

            out = new DataOutputStream(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            LOGGER.info("Service response: " + result);
            if(result.equalsIgnoreCase("remove")) {
                out.writeBytes("OK\r\n");
                in.readLine();
                in.close();
                out.close();
                System.exit(0);
            }


            out.writeBytes("data\r\n");
            result = in.readLine();
            LOGGER.info("Service response: " + result);

            if(!result.equalsIgnoreCase("OK")) {
                LOGGER.error("Invalid response");
                return;
            }

            daemonMaster.logData();

            in.close();
            out.close();
            socket.close();

            socket = new Socket(host, port);

            out = new DataOutputStream(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.writeBytes("conf\r\n");
            result = in.readLine();
            LOGGER.info("Service response: " + result);

            if(!result.equalsIgnoreCase("OK")) {
                LOGGER.error("Invalid response");
                return;
            }

            daemonMaster.configure();

            in.close();
            out.close();
            socket.close();

        } catch(Exception e) {
            LOGGER.error("Error connecting with server", e);
        }

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
}
