/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
package com.bosch.cr.integration.examples.rest;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Realm;
import org.asynchttpclient.Response;
import org.asynchttpclient.proxy.ProxyServer;

import com.bosch.cr.model.acl.AccessControlListModelFactory;
import com.bosch.cr.model.authorization.AuthorizationContext;
import com.bosch.cr.model.authorization.AuthorizationSubject;

/**
 * Simple REST client which authenticates against Central Registry with CRS authentication.
 */
public class SimpleCrRestClient
{

   private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
   private static final String CONTENT_TYPE_JSON = "application-json";

   private final String clientId;
   private final String apiToken;
   private final URI keystoreUri;
   private final String keystorePassword;
   private final String alias;
   private final String aliasPassword;

   private final String baseUrl;
   private final AsyncHttpClient httpClient;

   private SimpleCrRestClient(final String baseUrl, final String clientId, final String apiToken, final URI keystoreUri,
      final String keystorePassword, final String alias, final String aliasPassword)
   {
      this.baseUrl = baseUrl;
      this.clientId = clientId;
      this.apiToken = apiToken;
      this.keystoreUri = keystoreUri;
      this.keystorePassword = keystorePassword;
      this.alias = alias;
      this.aliasPassword = aliasPassword;

      final Properties props = new Properties(System.getProperties());
      final FileReader r;

      try
      {
         if (Files.exists(Paths.get("config.properties")))
         {
            r = new FileReader(Paths.get("config.properties").toFile());
         }
         else
         {
            final URL resource = SimpleCrRestClient.class.getClassLoader().getResource("config.properties");
            r = new FileReader(resource.getFile());
         }

         props.load(r);
         r.close();
      }
      catch (final IOException e)
      {
         throw new IllegalArgumentException("Error reading config.properties: {}", e);
      }

      httpClient = createHttpClient(props);
   }

   /**
    * Returns a new {@code SimpleCrRestClient} to interact with the Central Registry's REST API.
    * 
    * @return the new SimpleCrRestClient.
    */
   public static SimpleCrRestClient of(final String baseUrl, final String clientId, final String apiToken,
      final URI keystoreUri, final String keystorePassword, final String alias, final String aliasPassword)
   {
      return new SimpleCrRestClient(baseUrl, clientId, apiToken, keystoreUri, keystorePassword, alias, aliasPassword);
   }

   /**
    * Grants all permissions for the specified {@code authorizationContext} on the Thing matching the given
    * {@code thingId}.
    * 
    * @param thingId the identifier of the Thing.
    * @param authorizationContext the authorization context.
    * @throws IllegalArgumentException if {@code authorizationContext} contains no {@code AuthorizationSubject}.
    */
   public CompletableFuture<Response> grantPermissionsFor(final String thingId,
      final AuthorizationContext authorizationContext)
   {
      final AuthorizationSubject authorizationSubject = authorizationContext.getFirstAuthorizationSubject()
         .orElseThrow(() -> new IllegalArgumentException("No AuthorizationSubject present in AuthorizationContext!"));
      final String resource = baseUrl + "/cr/1/things/" + thingId + "/acl/" + authorizationSubject.getId();

      return httpClient.preparePut(resource) //
         .addHeader(HTTP_HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON) //
         .setBody(AccessControlListModelFactory.allPermissions().toJsonString()) //
         .execute() //
         .toCompletableFuture();
   }


   private AsyncHttpClient createHttpClient(final Properties properties)
   {
      final DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
      builder.setAcceptAnyCertificate(true); // WORKAROUND: Trust self-signed certificate of BICS until there is a
      // trusted one.

      final String proxyHost = properties.getProperty("http.proxyHost");
      final String proxyPort = properties.getProperty("http.proxyPort");
      final String proxyPrincipal = properties.getProperty("http.proxyPrincipal");
      final String proxyPassword = properties.getProperty("http.proxyPassword");
      if (proxyHost != null && proxyPort != null)
      {
         final ProxyServer.Builder proxyBuilder = new ProxyServer.Builder(proxyHost, Integer.valueOf(proxyPort));
         if (proxyPrincipal != null && proxyPassword != null)
         {
            // proxy with authentication
            proxyBuilder.setRealm(new Realm.Builder(proxyPrincipal, proxyPassword).setScheme(Realm.AuthScheme.BASIC)
               .setUsePreemptiveAuth(true));
         }
         builder.setProxyServer(proxyBuilder);
      }

      final SignatureFactory signatureFactory =
         SignatureFactory.newInstance(keystoreUri, keystorePassword, alias, aliasPassword);
      final AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient(builder.build());
      asyncHttpClient
         .setSignatureCalculator(new CrAsymmetricalSignatureCalculator(signatureFactory, clientId, apiToken));

      return asyncHttpClient;
   }
}
