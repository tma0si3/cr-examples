/*
 * Bosch SI Example Code License Version 1.0, January 2016
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
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE
 * QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL
 * NECESSARY SERVICING, REPAIR OR CORRECTION. THIS SHALL NOT APPLY TO MATERIAL DEFECTS AND DEFECTS OF TITLE WHICH BOSCH
 * SI HAS FRAUDULENTLY CONCEALED. APART FROM THE CASES STIPULATED ABOVE, BOSCH SI SHALL BE LIABLE WITHOUT LIMITATION FOR
 * INTENT OR GROSS NEGLIGENCE, FOR INJURIES TO LIFE, BODY OR HEALTH AND ACCORDING TO THE PROVISIONS OF THE GERMAN
 * PRODUCT LIABILITY ACT (PRODUKTHAFTUNGSGESETZ). THE SCOPE OF A GUARANTEE GRANTED BY BOSCH SI SHALL REMAIN UNAFFECTED
 * BY LIMITATIONS OF LIABILITY. IN ALL OTHER CASES, LIABILITY OF BOSCH SI IS EXCLUDED. THESE LIMITATIONS OF LIABILITY
 * ALSO APPLY IN REGARD TO THE FAULT OF VICARIOUS AGENTS OF BOSCH SI AND THE PERSONAL LIABILITY OF BOSCH SI'S EMPLOYEES,
 * REPRESENTATIVES AND ORGANS.
 */
package com.bosch.iot.hub.examples.connector.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import com.bosch.iot.hub.client.DefaultIotHubClient;
import com.bosch.iot.hub.client.IotHubClient;
import com.bosch.iot.hub.client.IotHubClientBuilder;
import com.bosch.iot.hub.model.acl.AccessControlList;
import com.bosch.iot.hub.model.acl.AclEntry;
import com.bosch.iot.hub.model.acl.AuthorizationSubject;
import com.bosch.iot.hub.model.acl.Permission;
import com.bosch.iot.hub.model.message.Message;
import com.bosch.iot.hub.model.message.Payload;
import com.bosch.iot.hub.model.topic.TopicPath;

/**
 * Example HTTP connector service which manages sending of messages from
 * authenticated HTTP-connected devices to the IoT Hub service via accepting
 * HTTP PUT requests against arbitrary URIs denoting the message topics.
 * 
 * TODO example
 * <p>
 * The example HTTP connector service incorporates the IoT Hub integration
 * client in order to communicate with the IoT Hub service. Settings needed for
 * authentication and establishing a connection with the IoT Hub service are
 * loaded from the {@code configuration.properties} file - please make sure that
 * you have properly configured your solution id and key store settings before
 * starting the HTTP connector application. The {@code configuration.properties}
 * also contains a list of topics that the example HTTP connector creates using
 * the provided solution id as root topic.
 */
@RestController
public class HttpConnectorController {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpConnectorController.class);
	private static final long DEFAULT_TIMEOUT = 5; // 5 seconds

	private String solutionId;// solution id used to authenticate to the iot hub service
	private String connectorClientId;// http connector client id,  format <solution-id>:connector
	private String consumerClientId;// http consumer client id,  format <solution-id>:consumer

	private IotHubClient iotHubClient;

	private PathMatcher topicPathMatcher = new AntPathMatcher();
	private List<TopicPath> topics;

	/**
	 * Loads the HTTP consumer configuration form the
	 * {@code configuration.properties} file, creates the IoT Hub integration
	 * clients used for sending messages to the IoT Hub service. Creates the
	 * example topics and configures their access control lists.
	 * 
	 * @throws Exception
	 */
	@PostConstruct
	public void postConstruct() throws Exception {//FIXME exceptions?

		// load the configuration for the http connector example
		Properties configuration = new Properties(System.getProperties());
		configuration
				.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("configuration.properties"));

		solutionId = configuration.getProperty("solutionId");
		connectorClientId = solutionId + ":http:connector";
		consumerClientId = solutionId + ":http:consumer";

		// create the hub client which will be used for sending messages to iot hub service
		constructIotHubClient(configuration);
		
		// create the access control list used to define permissions for the name space of the example
		// the solution id is used as name space
		final AccessControlList namespaceACL = AccessControlList
				.of(AclEntry.of(AuthorizationSubject.of(connectorClientId), Permission.ADMINISTRATE));
		final TopicPath namespace = TopicPath.of(solutionId);
		// create the name space for the http connector example
		createTopic(namespace, namespaceACL);

		// create the access control list used to define permissions on the example topics
		final AccessControlList topicsACL = AccessControlList.of(
				AclEntry.of(AuthorizationSubject.of(consumerClientId), Permission.RECEIVE),
				AclEntry.of(AuthorizationSubject.of(connectorClientId), Permission.ADMINISTRATE, Permission.RECEIVE,
						Permission.SEND));
		// create the configured topics in the name space of the example
		String exampleTopicPaths = configuration.getProperty("topics");
		topics = Stream.of(exampleTopicPaths.split(",")).map(String::trim)
				.map(topicPath -> createTopic(namespace.append(topicPath), topicsACL)).collect(Collectors.toList());

	}

	/**
	 * Deletes the example topics and destroys the IoT Hub integration client
	 * used for sending messages to the IoT Hub service.
	 */
	@PreDestroy
	public void preDestroy() {

		// delete the created topics in reversed order
		Collections.reverse(topics);
		topics.forEach(topic -> deleteTopic(topic));

		// delete the root topic
		deleteTopic(solutionId);

		// destroy the hub integration client used for sending messages
		// close the connection to back-end and clean up allocated resources
		destroyIotHubClient(connectorClientId, iotHubClient);

	}

	/**
	 * Authenticated HTTP-connected devices can send messages to the IoT Hub
	 * service by initiating an HTTP PUT requests against arbitrary URIs. The
	 * message topic is derived from the HTTP request URI and the message
	 * payload is extracted from the HTTP request body.
	 * 
	 * <p>
	 * For example, an authenticated HTTP-connected device can send messages to
	 * the IoT Things service using HTTP requests:
	 * 
	 * <p>
	 * PUT /http-connector/things/commands/modify/&lt;device-id&gt; <br/>
	 * will send hub message for topic
	 * &lt;solution-id&gt;/things/commands/modify/&lt;device-id&gt;
	 * 
	 * <p>
	 * PUT /http-connector/things/commands/delete/&lt;device-id&gt; <br/>
	 * will send hub message for topic
	 * &lt;solution-id&gt;/things/commands/delete/&lt;device-id&gt;
	 * 
	 * <p>
	 * PUT /http-connector/things/commands/create/&lt;device-id&gt; <br/>
	 * will send hub message for topic
	 * &lt;solution-id&gt;/things/commands/create/&lt;device-id&gt;
	 *
	 * <p>
	 * PUT /http-connector/things/messages/toggle/&lt;device-id&gt;<br/>
	 * will send hub messages for topic
	 * &lt;solution-id&gt;/things/messages/toggle/&lt;device-id&gt;
	 * 
	 * @param requestEntity {@link HttpEntity} encapsulating the HTTP request
	 *            body and headers, it is used to access the request body.
	 * @param request {@link HttpServletRequest} encapsulating the HTTP client
	 *            request information, it is used to access the request path and
	 *            session.
	 * @return {@link ResponseEntity} including the HTTP status code for the
	 *         operation. Possible values are 200 OK - if the messages has been
	 *         successfully sent to the IoT Hub services, or 400 Bad Request -
	 *         if any error occurs while sending the message.
	 */
	@RequestMapping(value = "/http-connector/**", method = RequestMethod.PUT)
	public ResponseEntity<Void> sendMessage(HttpEntity<byte[]> requestEntity, HttpServletRequest request) {
		final TopicPath topic = extractTopicPath(request);
		LOGGER.info("Sending message for topic <{}> using client <{}>", topic, connectorClientId);
		try {
			Payload payload = requestEntity.hasBody() ? Payload.of(requestEntity.getBody()) : Payload.empty();
			getIoTHubClient().send(Message.of(topic, payload)).get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
			LOGGER.warn("Error while sending message for topic <{}> using client <{}>", topic, connectorClientId, e);
			return ResponseEntity.badRequest().build();
		}
		return ResponseEntity.ok().build();
	}

	/**
	 * Extracts the topic path from the HTTP request path, the provided
	 * {@link HttpServletRequest} object is used to access the request path. The
	 * solution id (example namespace) is prepended to the topic path in order
	 * to construct the target message topic.
	 * 
	 * @param request {@link HttpServletRequest} encapsulating the HTTP client
	 *            request information, it is used to access the request path
	 * @return the topic path of interest, solution id is prepended to the path
	 *         extracted form the HTTP request path
	 */
	private TopicPath extractTopicPath(HttpServletRequest request) {
		String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		String topicPath = topicPathMatcher.extractPathWithinPattern("http-connector/**", fullPath);
		return TopicPath.of(topicPath).prepend(solutionId);
	}

	private TopicPath createTopic(CharSequence topicPath, AccessControlList acl) {
		LOGGER.info("Creating topic <{}> using client <{}>", topicPath, connectorClientId);
		try {
			return getIoTHubClient().createTopic(topicPath, acl).get().getPath();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.warn("Error wahile creating topic <{}> using client <{}>", topicPath, connectorClientId, e);
			return null; // FIXME throw exception?
		}

	}

	private void deleteTopic(CharSequence topicPath) {
		LOGGER.info("Deleting topic <{}> using client <{}>", topicPath, connectorClientId);
		try {
			getIoTHubClient().deleteTopic(topicPath).get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.warn("Error wahile deleting topic <{}> using client <{}>", topicPath, connectorClientId, e);
		}

	}

	private void constructIotHubClient(Properties configuration) throws URISyntaxException {//FIXME exceptions?
		LOGGER.info("Creating IoT Hub integration client for client ID <{}>.", connectorClientId);

		// endpoint of the bosch iot cloud service
		URI iotHubEndpoint = URI
				.create(configuration.getProperty("iotHubEndpoint", "wss://hub.apps.bosch-iot-cloud.com"));

		// location and password for the key store holding your private key 
		// client authentication is implemented by a handshake using an asymmetric key-pair
		String keystoreLocation = configuration.getProperty("keystoreLocation", "HubClient.jks");
		String keystorePassword = configuration.getProperty("keystorePassword");

		// key alias and password
		String keyAlias = configuration.getProperty("keyAlias", "Hub");
		String keyAliasPassword = configuration.getProperty("keyAliasPassword");

		// location and password for key store to be used as basis 
		// when the client makes trust-related decisions while verifying the remote endpoint's certificate
		// it is currently necessary for accepting bosch self signed certificates
		String truststoreLocation = configuration.getProperty("truststoreLocation", "bosch-iot-cloud.jks");
		String truststorePassword = configuration.getProperty("truststorePassword", "jks");

		// http proxy settings
		String httpProxyHost = configuration.getProperty("httpProxyHost");
		String httpProxyPort = configuration.getProperty("httpProxyPort");
		String httpProxyPrincipal = configuration.getProperty("httpProxyPrincipal");
		String httpProxyPassword = configuration.getProperty("httpProxyPassword");

		// provide required configuration (authentication configuration and iot hub endpoint)
		// proxy configuration is optional and can be added if the proxy configuration properties exist
		final IotHubClientBuilder.OptionalPropertiesSettable builder = DefaultIotHubClient.newBuilder()
				.endPoint(iotHubEndpoint).keyStore(resourceURI(keystoreLocation), keystorePassword)
				.alias(keyAlias, keyAliasPassword).clientId(connectorClientId)
				.sslTrustStore(resourceURI(truststoreLocation), truststorePassword);

		// configure http proxy for the client, if provided
		if (httpProxyHost != null && httpProxyPort != null) {
			builder.proxy(URI.create("http://" + httpProxyHost + ':' + httpProxyPort)) //
					.proxyAuthentication(httpProxyPrincipal, httpProxyPassword);
		}

		iotHubClient = builder.build(); // build the client
		iotHubClient.connect(); // establish a connection with the iot hub service
	}

	private void destroyIotHubClient(String iotHubClientId, IotHubClient iotHubclient) {
		LOGGER.info("Destroying IoT Hub integration client for client ID <{}>.", iotHubClientId);
		iotHubclient.destroy();

	}

	private URI resourceURI(String name) throws URISyntaxException {
		return Thread.currentThread().getContextClassLoader().getResource(name).toURI();
	}

	private IotHubClient getIoTHubClient() {
		// temporally, before invoking operations on the client, we need to check the client's connection and reconnect, if needed
		// as currently the web socket connection opened by the hub client is silently closed on client inactivity
		// which causes consequent attempts to communicate with the hub to fail
		if (!iotHubClient.isConnected()) {
			iotHubClient.connect();
		}
		return iotHubClient;
	}

}
