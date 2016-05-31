package pl.edu.agh;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * This software may be modified and distributed under the terms
 *  of the BSD license.  See the LICENSE.txt file for details.
 */

public class Main {

    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("asl-service-application-context.xml");
    }
}
