/*
Copyright (c) 2015, Bosch Software Innovations GmbH, Germany
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer
   in the documentation and/or other materials provided with the distribution.

3. Neither the name of the Bosch Software Innovations GmbH, Germany nor the names of its contributors
   may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
OF SUCH DAMAGE.
*/

package com.bosch.cr.examples.carintegrator;

import com.bosch.cr.integration.IntegrationClient;
import com.bosch.cr.integration.IntegrationClientConfiguration;
import com.bosch.cr.integration.IntegrationClientImpl;
import com.bosch.cr.integration.authentication.AuthenticationConfiguration;
import com.bosch.cr.integration.authentication.PublicKeyAuthenticationConfiguration;
import com.bosch.cr.integration.messaging.stomp.StompProviderConfiguration;
import com.bosch.cr.integration.registration.ThingLifecycleEvent;
import org.jboss.netty.util.internal.ThreadLocalRandom;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Example implementation of a "Gateway" that brings devices into your Solution.
 * This example simulates vehicle movements.
 */

public class VehicleSimulator {

    public static void main(String[] args) throws IOException, InterruptedException {

        Properties props = new Properties(System.getProperties());
        FileReader r = new FileReader("config.properties");
        props.load(r);
        r.close();
        System.out.println(props);

        final AuthenticationConfiguration authConfig = PublicKeyAuthenticationConfiguration.newBuilder()
                .clientId(props.getProperty("clientId"))
                .keyStoreLocation(new File("CRClient.jks").toURI().toURL())
                .keyStorePassword(props.getProperty("keyStorePassword"))
                .signatureAlgorithm("SHA512withECDSA")
                .alias(props.getProperty("keyAlias")).aliasPassword(props.getProperty("keyAliasPassword")).build();

        final StompProviderConfiguration.StompProviderConfigurationBuilder providerConfigBuilder = StompProviderConfiguration.newBuilder()
                .sslKeyStoreLocation(VehicleSimulator.class.getResource("/bosch-iot-cloud.jks"))
                .sslKeyStorePassword("jks");
        if (props.getProperty("http.proxyHost") != null) {
            providerConfigBuilder.proxyHost(props.getProperty("http.proxyHost"));
        }
        if (props.getProperty("http.proxyPort") != null) {
            providerConfigBuilder.proxyPort(Integer.valueOf(props.getProperty("http.proxyPort")));
        }
        final StompProviderConfiguration providerConfig = providerConfigBuilder.build();

        final IntegrationClientConfiguration clientConfig = IntegrationClientConfiguration.newBuilder()
                .authenticationConfiguration(authConfig)
                .centralRegistryEndpointUrl(props.getProperty("centralRegistryEndpointUrl"))
                .providerConfiguration(providerConfig)
                .build();

        final IntegrationClient client = IntegrationClientImpl.newInstance(clientConfig);

        final TreeSet<String> activeThings = new TreeSet<>();
        activeThings.addAll(readActiveThings());

        System.out.println("Started...");
        System.out.println("Active things: " + activeThings);

        client.things().registerForLifecycleEvent("lifecycle", e -> {
            if (e.getType() == ThingLifecycleEvent.Lifecycle.CREATED) {
                activeThings.add(e.getThingId());
                writeActiveThings(activeThings);
                System.out.println("New thing " + e.getThingId() + " created -> active things: " + activeThings);
            }
        });

        final Thread thread = new Thread(() -> {
            final Random random = new ThreadLocalRandom();
            while (true) {
                for (String thingId : activeThings) {

                    client.things().forId(thingId).retrieve()
                            .fields("thingId", "attributes/_features/geolocation/properties/geoposition")
                            .onFailure(e -> System.out.println("Retrieve thing " + thingId + " failed: " + e))
                            .onSuccess(thing -> {

                                JsonObject geoposition = thing.getAttributes().getJsonObject("_features")
                                        .getJsonObject("geolocation").getJsonObject("properties")
                                        .getJsonObject("geoposition");

                                JsonObject newPeoposition = Json.createObjectBuilder()
                                        .add("latitude", geoposition.getJsonNumber("latitude").doubleValue() + (random.nextDouble() - 0.5) / 250)
                                        .add("longitude", geoposition.getJsonNumber("longitude").doubleValue() + (random.nextDouble() - 0.5) / 250).build();

                                client.things().forId(thingId).changeAttribute("_features/geolocation/properties/geoposition", newPeoposition)
                                        .onFailure(e -> System.out.println("Change failed: " + e))
                                        .onSuccess(_void -> {
                                            System.out.print(".");
                                            if (random.nextDouble() < 0.01) {
                                                System.out.println();
                                            }
                                        })
                                        .apply(5, TimeUnit.SECONDS);
                            }).apply(5, TimeUnit.SECONDS);
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    System.out.println("Update thread interrupted");
                    return;
                }
            }
        });

        thread.start();

        System.out.println("Press enter to terminate");
        System.in.read();

        System.out.println("Shutting down ...");
        thread.interrupt();
        Thread.sleep(5000);
        client.destroy(5000, TimeUnit.MILLISECONDS);
        System.out.println("Client destroyed");
    }

    private static Collection<String> readActiveThings() {
        Properties p = new Properties();
        try {
            FileReader r = new FileReader("things.properties");
            p.load(r);
            r.close();
            return Arrays.asList(p.getProperty("thingIds").split(","));
        } catch (FileNotFoundException ex) {
            return Collections.EMPTY_LIST;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void writeActiveThings(TreeSet<String> activeThings) {
        Properties p = new Properties();
        p.setProperty("thingIds", String.join(",", activeThings));
        try {
            FileWriter w = new FileWriter("things.properties");
            p.store(w, "List of currently managed things by this gateway");
            w.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
