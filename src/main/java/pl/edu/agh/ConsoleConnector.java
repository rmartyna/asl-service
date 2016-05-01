package pl.edu.agh;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ConsoleConnector implements InitializingBean, Runnable {

    private Integer port = 30303;

    private DaemonMaster daemonMaster;

    private ServerSocket serverSocket;

    private static final Logger LOGGER = Logger.getLogger(ConsoleConnector.class);

    public void afterPropertiesSet() throws Exception {
        serverSocket = new ServerSocket(port);
        new Thread(this).start();
    }

    public void run() {
        LOGGER.info("Waiting for incoming messages on port " + port + "...");
        while(true) {
            try {
                Socket client = serverSocket.accept();
                LOGGER.info("Message received from: " + client.getInetAddress());

                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                DataOutputStream out = new DataOutputStream(client.getOutputStream());

                String message = in.readLine();
                LOGGER.info("Received configuration: " + message);

                try {
                    daemonMaster.configureSlaves(message);
                    out.writeBytes("HTTP/1.1 200 \r\n");
                } catch(Exception e) {
                    LOGGER.error("Invalid daemons configuration", e);
                    out.writeBytes("HTTP/1.1 400 \r\n");
                }

                in.close();
                out.close();
                client.close();

            } catch(Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void setDaemonMaster(DaemonMaster daemonMaster) {
        this.daemonMaster = daemonMaster;
    }
}
