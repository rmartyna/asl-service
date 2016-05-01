package pl.edu.agh;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {

    public static void main(String[] args) {
        ApplicationContext content = new ClassPathXmlApplicationContext("asl-service-application-context.xml");


    }
}
