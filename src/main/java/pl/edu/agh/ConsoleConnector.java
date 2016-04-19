package pl.edu.agh;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by rmartyna on 18.04.16.
 */
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

                PrintWriter out = new PrintWriter(client.getOutputStream());
                out.print("HTTP/1.1 200 \r\n");
                out.print("Content-Type: text/plain\r\n");
                out.print("Connection: close\r\n");
                out.print("\r\n");
                out.close();

                String message = IOUtils.toString(client.getInputStream(), "UTF-8");
                LOGGER.info("Received configuration: " + message);

                client.close();

                daemonMaster.configureSlaves(message);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void setDaemonMaster(DaemonMaster daemonMaster) {
        this.daemonMaster = daemonMaster;
    }
}
