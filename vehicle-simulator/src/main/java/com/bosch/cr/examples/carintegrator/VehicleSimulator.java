/*
 * Copyright (c) 2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the Bosch Software Innovations GmbH, Germany nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.bosch.cr.examples.carintegrator;

import com.bosch.cr.examples.carintegrator.util.CrAsymmetricalSignatureCalculator;
import com.bosch.cr.examples.carintegrator.util.SignatureFactory;
import com.bosch.cr.integration.IntegrationClient;
import com.bosch.cr.integration.IntegrationClientConfiguration;
import com.bosch.cr.integration.IntegrationClientImpl;
import com.bosch.cr.integration.authentication.AuthenticationConfiguration;
import com.bosch.cr.integration.authentication.PublicKeyAuthenticationConfiguration;
import com.bosch.cr.integration.configuration.ProxyConfiguration;
import com.bosch.cr.integration.configuration.TrustStoreConfiguration;
import com.bosch.cr.integration.model.ThingLifecycleEvent;
import com.bosch.cr.json.JsonFactory;
import com.bosch.cr.json.JsonObject;
import com.bosch.cr.model.things.Thing;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Response;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Example implementation of a "Gateway" that brings devices into your Solution.
 * This example simulates vehicle movements.
 */
public class VehicleSimulator {

    private static String centralRegistryEndpointUrl;
    private static AsyncHttpClient asyncHttpClient;

    private static final Random random = new Random();

    public static void main(String[] args) throws IOException, InterruptedException {

        Properties props = new Properties(System.getProperties());
        try {
            if (new File("config.properties").exists()) {
                props.load(new FileReader("config.properties"));
            } else {
                InputStream i = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");
                props.load(i);
                i.close();
            }
            System.out.println("Config: " + props);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        centralRegistryEndpointUrl = props.getProperty("centralRegistryEndpointUrl");
        String centralRegistryMessagingUrl = props.getProperty("centralRegistryMessagingUrl");

        String clientId = props.getProperty("clientId");

        URI keystoreUri = new File("CRClient.jks").toURI();
        String keystorePassword = props.getProperty("keyStorePassword");
        String keyAlias = props.getProperty("keyAlias");
        String keyAliasPassword = props.getProperty("keyAliasPassword");

        final String proxyHost = props.getProperty("http.proxyHost");
        final String proxyPort = props.getProperty("http.proxyPort");

        AuthenticationConfiguration authenticationConfiguration = PublicKeyAuthenticationConfiguration.newBuilder()
                .clientId(clientId)
                .keyStoreLocation(keystoreUri.toURL()).keyStorePassword(keystorePassword)
                .alias(keyAlias).aliasPassword(keyAliasPassword).build();

        TrustStoreConfiguration trustStore = TrustStoreConfiguration.newBuilder()
                .location(VehicleSimulator.class.getResource("/bosch-iot-cloud.jks"))
                .password("jks").build();

        IntegrationClientConfiguration.OptionalConfigSettable configSettable = IntegrationClientConfiguration.newBuilder()
                .authenticationConfiguration(authenticationConfiguration)
                .centralRegistryEndpointUrl(centralRegistryMessagingUrl)
                .trustStoreConfiguration(trustStore);
        if (proxyHost != null && proxyPort != null) {
            configSettable = configSettable.proxyConfiguration(ProxyConfiguration.newBuilder()
                    .proxyHost(proxyHost).proxyPort(Integer.parseInt(proxyPort)).build());
        }

        IntegrationClient client = IntegrationClientImpl.newInstance(configSettable.build());

        // ### WORKAROUND: prepare HttpClient to make REST calls the CR-Integration-Client does not support yet
        String apiToken = props.getProperty("apiToken");
        final SignatureFactory signatureFactory = SignatureFactory.newInstance(keystoreUri, keystorePassword, keyAlias, keyAliasPassword);
        final AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        builder.setSSLContext(setupAcceptingSelfSignedCertificates());
        if (proxyHost != null && proxyPort != null) {
            builder.setProxyServer(new ProxyServer(ProxyServer.Protocol.HTTPS, proxyHost, Integer.valueOf(proxyPort)));
        }
        builder.setAllowPoolingConnections(true).setAllowPoolingSslConnections(true);
        asyncHttpClient = new AsyncHttpClient(builder.build());
        asyncHttpClient.setSignatureCalculator(new CrAsymmetricalSignatureCalculator(signatureFactory, clientId, apiToken));

        final TreeSet<String> activeThings = new TreeSet<>();
        activeThings.addAll(readActiveThings());

        System.out.println("Started...");
        System.out.println("Active things: " + activeThings);

        client.things().registerForLifecycleEvent("lifecycle", e -> {
            System.out.println("Lifecycle event: " + e);
            if (e.getType() == ThingLifecycleEvent.Type.CREATED) {
                activeThings.add(e.getThingId());
                writeActiveThings(activeThings);
                System.out.println("New thing " + e.getThingId() + " created -> active things: " + activeThings);
            }
        });

        final Thread thread = new Thread(() -> {
            while (true) {
                for (String thingId : activeThings) {

                    try {
                        Thing thing = client.things().forId(thingId).retrieve(JsonFactory.newFieldSelector("thingId", "features/geolocation/properties/geoposition")).get(5, TimeUnit.SECONDS);

                        if (!thing.getFeatures().isPresent() || !thing.getFeatures().get().getFeature("geolocation").isPresent()) {
                            System.out.println("Thing " + thingId + " has no Feature \"geolocation\"");
                            return;
                        }

                        JsonObject geoposition = thing.getFeatures().get().getFeature("geolocation").orElseThrow(RuntimeException::new)
                                .getProperties().get().getValue(JsonFactory.newPointer("geoposition")).get().asObject();
                        JsonObject newGeoposition = JsonFactory.newObjectBuilder()
                                .set("latitude", geoposition.get("latitude").get().asDouble() + (random.nextDouble() - 0.5) / 250)
                                .set("longitude", geoposition.get("longitude").get().asDouble() + (random.nextDouble() - 0.5) / 250).build();
                        changeProperty(thingId, "geolocation", "geoposition", newGeoposition);

                        System.out.print(".");
                        if (random.nextDouble() < 0.01) {
                            System.out.println();
                        }
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        System.out.println("Update thread interrupted");
                        return;
                    } catch (ExecutionException | TimeoutException e) {
                        System.out.println("Updating thing " + thingId + " failed: " + e);
                    }
                }
            }
        }
        );

        thread.start();

        System.out.println("Press enter to terminate");
        System.in.read();

        System.out.println("Shutting down ...");
        thread.interrupt();
        Thread.sleep(5000);
        client.destroy();
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
            return Collections.emptyList();
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

    // ### WORKAROUND: change property using REST until CR-Integration-Client supports it
    private static void changeProperty(String thingId, String feature, String property, JsonObject value) throws InterruptedException {
        final String thingJsonString = value.toString();
        final String path = "/cr/1/things/" + thingId + "/features/" + feature + "/properties/" + property;

        Future<Response> f = asyncHttpClient.preparePut(centralRegistryEndpointUrl + path)
                .addHeader("Content-Type", "application-json")
                .setBody(thingJsonString)
                .execute();

        try {
            Response re = f.get();
            if (re.getStatusCode() < 200 || re.getStatusCode() >= 300) {
                throw new RuntimeException("Updated failed; " + re.getStatusCode() + ": " + re.getResponseBody());
            }
        } catch (IOException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    // ### WORKAROUND: Trust self-signed certificate of BICS until there is a trusted one.
    private static SSLContext setupAcceptingSelfSignedCertificates() {
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
            }
        }};
        final SSLContext sc;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, null);
            return sc;
        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            throw new RuntimeException(ex);
        }
    }
}
