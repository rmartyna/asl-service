package pl.edu.agh;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {

    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("asl-service-application-context.xml");
    }
}
