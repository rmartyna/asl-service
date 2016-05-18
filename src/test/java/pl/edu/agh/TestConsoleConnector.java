package pl.edu.agh;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.*;
import java.net.Socket;
import java.util.AbstractCollection;

import static org.junit.Assert.assertTrue;

public class TestConsoleConnector {

    private static ApplicationContext applicationContext;

    private String host = "127.0.0.1";

    private Integer port = 30303;

    private Socket socket;

    private DataOutputStream out;

    private BufferedReader in;

    @BeforeClass
    public static void init() {
       //applicationContext = new ClassPathXmlApplicationContext("asl-service-test-application-context.xml");
    }

    @Test
    public void testOutsideIpAddress() throws Exception {
        host = "89.65.26.46";
        testCpuConfiguration();
        host = "127.0.0.1";
    }

    @Test
    public void testCpuConfiguration() throws Exception {
        testValid("cpu,sleepTime,3000;cpu,fanMin,2000");
    }

    @Test
    public void testTwoDaemonsConfiguration() throws Exception {
        testValid("cpu,sleepTime,3000;network,downloadMin,2000;cpu,fanMax,4000");
    }

    @Test
    public void testInvalidConfiguration() throws Exception {
        testInvalid("kahsfkjsahfjs");
    }

    @Test
    public void testInvalidDaemonName() throws Exception {
        testInvalid("asd,sleepTime,3000");
    }

    @Test
    public void testInvalidParameterName() throws Exception {
        testInvalid("cpu,asd,3000");
    }

    @Test
    public void testInvalidParameterValue() throws Exception {
        testInvalid("cpu,sleepTime,asd");
    }

    @Test
    public void testAddRemoveLog() throws Exception {
        testValid("sysLog,addLog,/var/log/dmesg");
        testValid("sysLog,removeLog,/var/log/dmesg");
    }

    @Test
    public void testAddInvalidLog() throws Exception {
        testInvalid("sysLog,addLog,/xxx/xzvasd/asds");
    }

    @Test
    public void testAddLogTwice() throws Exception {
        testValid("sysLog,addLog,/var/log/dmesg");
        testInvalid("sysLog,addLog,/var/log/dmesg");
        testValid("sysLog,removeLog,/var/log/dmesg");
    }

    @Test
    public void testRemoveLogInvalid() throws Exception {
        testInvalid("sysLog,removeLog,/sadf/asf/sf/sfas");
    }

    @Test
    public void testNullConfiguration() throws Exception {
        testInvalid(null);
    }

    @Test
    public void testEmptyConfiguration() throws Exception {
        testInvalid("");
    }

    private void testValid(String message) throws Exception {
        try {
            openSocket();

            out.writeBytes(message + "\r\n");
            String result = in.readLine();
            assertTrue(result.contains("HTTP/1.1 200"));
        } finally {
            closeSocket();
        }
    }

    private void testInvalid(String message) throws Exception {
        try {
            openSocket();

            out.writeBytes(message + "\r\n");
            String result = in.readLine();
            assertTrue(result.contains("HTTP/1.1 400"));
        } finally {
            closeSocket();
        }
    }

    private void openSocket() throws IOException {
        socket = new Socket(host, port);

        out = new DataOutputStream(socket.getOutputStream());
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void closeSocket() throws IOException {
        in.close();
        out.close();
        socket.close();
    }


}
