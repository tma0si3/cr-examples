/*
 *                                            Bosch SI Example Code License
 *                                              Version 1.0, January 2016
 *
 * Copyright 2016 Bosch Software Innovations GmbH ("Bosch SI"). All rights reserved.
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
 * BOSCH SI PROVIDES THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF 
 * ALL NECESSARY SERVICING, REPAIR OR CORRECTION. THIS SHALL NOT APPLY TO MATERIAL DEFECTS AND DEFECTS OF TITLE WHICH 
 * BOSCH SI HAS FRAUDULENTLY CONCEALED. APART FROM THE CASES STIPULATED ABOVE, BOSCH SI SHALL BE LIABLE WITHOUT
 * LIMITATION FOR INTENT OR GROSS NEGLIGENCE, FOR INJURIES TO LIFE, BODY OR HEALTH AND ACCORDING TO THE PROVISIONS OF
 * THE GERMAN PRODUCT LIABILITY ACT (PRODUKTHAFTUNGSGESETZ). THE SCOPE OF A GUARANTEE GRANTED BY BOSCH SI SHALL REMAIN
 * UNAFFECTED BY LIMITATIONS OF LIABILITY. IN ALL OTHER CASES, LIABILITY OF BOSCH SI IS EXCLUDED. THESE LIMITATIONS OF 
 * LIABILITY ALSO APPLY IN REGARD TO THE FAULT OF VICARIOUS AGENTS OF BOSCH SI AND THE PERSONAL LIABILITY OF BOSCH SI'S
 * EMPLOYEES, REPRESENTATIVES AND ORGANS.
 */
package com.bosch.cr.examples.inventorybrowser.server;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.ssl.SSLContextBuilder;
import org.mitre.dsmiley.httpproxy.ProxyServlet;

/**
 * ProxyServlet to forward all requests to target service required as replacement for for cross domain requests.
 */
@WebServlet("/api/*")
public class CustomProxyServlet extends ProxyServlet
{

   private CloseableHttpClient httpClient;
   private Properties config;

   @Override
   public void init(ServletConfig config) throws ServletException
   {
      super.init(config);
   }

   @Override
   protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
   {
      // enforce BASIC auth on all requests
      String auth = req.getHeader("Authorization");
      if (auth == null) {
         resp.setHeader("WWW-Authenticate", "BASIC realm=\"Proxy for Bosch IoT Things\"");
         resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
         return;
      }

      super.service(req, resp);
   }

   @Override
   protected void copyRequestHeaders(HttpServletRequest req, HttpRequest proxyRequest)
   {
      super.copyRequestHeaders(req, proxyRequest);

      // submit API Token
      proxyRequest.addHeader("x-cr-api-token", getConfig().getProperty("apiToken"));
   }

   @Override
   protected String getConfigParam(String key)
   {
      switch (key) {
         case "targetUri":
            return getConfig().getProperty("thingsServiceEndpointUrl", "https://things.apps.bosch-iot-cloud.com") + "/api";
         case "log":
            return getConfig().getProperty("logProxyRequests", "true");
      }
      return super.getConfigParam(key);
   }

   @Override
   protected HttpClient createHttpClient(HttpParams hcParams)
   {
      // use custom httpClient with http-proxy support
      return getHttpClient();
   }

   private synchronized Properties getConfig()
   {
      if (config == null) {
         try {
            config = new Properties(System.getProperties());
            if (new File("config.properties").exists()) {
               config.load(new FileReader("config.properties"));
            } else {
               InputStream i = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");
               config.load(i);
               i.close();
            }
            System.out.println("Config: " + config);
         } catch (IOException ex) {
            throw new RuntimeException(ex);
         }
      }
      return config;
   }

   private synchronized CloseableHttpClient getHttpClient()
   {
      if (httpClient == null) {
         try {
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

            // #### ONLY FOR TEST: Trust ANY certificate (self certified, any chain, ...)
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, (chain, authType) -> true).build();
            httpClientBuilder.setSSLContext(sslContext);

            // #### ONLY FOR TEST: Do NOT verify hostname
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslConnectionSocketFactory)
                    .build();
            PoolingHttpClientConnectionManager httpClientConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            httpClientBuilder.setConnectionManager(httpClientConnectionManager);

            if (getConfig().getProperty("http.proxyHost") != null) {
               httpClientBuilder.setProxy(new HttpHost(getConfig().getProperty("http.proxyHost"),
                       Integer.parseInt(getConfig().getProperty("http.proxyPort"))));
            }

            httpClient = httpClientBuilder.build();
         } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException ex) {
            throw new RuntimeException(ex);
         }
      }

      return httpClient;
   }

}
