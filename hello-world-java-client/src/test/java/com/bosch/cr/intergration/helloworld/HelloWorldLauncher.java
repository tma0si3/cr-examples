package com.bosch.cr.intergration.helloworld;

import com.bosch.cr.integration.helloworld.HelloWorld;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Created by mau1imb on 07.03.2016.
 */
public class HelloWorldLauncher {

    public static void main(final String... args) throws InterruptedException, ExecutionException, TimeoutException
    {
        // After Step 1 was successfully add the Thing ID here
        final String thingId = "<your-thingId>";

        // Instantiate the Java Client
        HelloWorld helloWorld = new HelloWorld();

        // Step 1: Create an empty Thing and get Thing ID
        helloWorld.createEmptyThing();

        // Step 2: Update the ACL with your User ID
        // Before this Step you have to add your User ID in the HelloWorld Class
        // helloWorld.updateACL(thingId);

        // Step 3: Loop to update the attributes of the Thing
        /* for (int i = 0; i <= 200; i++) {
             helloWorld.updateThing(thingId);
                Thread.sleep(2000);
        }*/

        // This step must always be concluded to terminate the Java client.
        helloWorld.terminate();
    }
}
