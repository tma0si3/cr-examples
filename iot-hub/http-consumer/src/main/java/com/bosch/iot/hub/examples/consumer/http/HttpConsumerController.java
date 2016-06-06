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
package com.bosch.iot.hub.examples.consumer.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import com.bosch.iot.hub.client.ConsumerRegistration;
import com.bosch.iot.hub.client.DefaultIotHubClient;
import com.bosch.iot.hub.client.IotHubClient;
import com.bosch.iot.hub.client.IotHubClientBuilder;
import com.bosch.iot.hub.model.message.Payload;
import com.bosch.iot.hub.model.topic.TopicPath;

/**
 * FIXME rework based on final idea for teh example
 * 
 * 
 * Example implementation an HTTP consumer which manages delivering messages
 * form the IoT Hub service to authenticated HTTP-connected devices via
 * server-sent events streamed at arbitrary URIs denoting the message topics.
 */
@RestController
public class HttpConsumerController {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpConsumerController.class);

	private String solutionId;// solution id used to authenticate to the iot hub service
	private String consumerClientId;// http consumer client id, format solution-id:consumer

	private IotHubClient iotHubClient;

	private PathMatcher topicPathMatcher = new AntPathMatcher();
	private Map<String, SubscriptionData> subscriptions = new ConcurrentHashMap<>();

	/**
	 * Loads the HTTP consumer configuration form the
	 * {@code configuration.properties} file and constructs the IoT Hub
	 * integration client used for consuming messages from the IoT Hub service.
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
		consumerClientId = solutionId + ":http:consumer";

		// create the client which will be used for consuming messages from the iot hub service
		constructIotHubClient(configuration);
	}

	/**
	 * Closes the HTTP connections currently opened for delivering server-sent
	 * events, removes all active consumer registrations and destroys the IoT
	 * Hub integration client used for consuming messages from the IoT Hub
	 * service.
	 */
	@PreDestroy
	public void preDestroy() {

		// close all active sse connections
		// completing the sse emitters will remove the corresponding consumer registrations
		subscriptions.forEach((consumerId, consumer) -> consumer.emitter().complete());

		// destroy the hub integration clients used for consuming messages
		// close the connection to back-end and clean up allocated resources
		destroyIotHubClient(consumerClientId, iotHubClient);

	}

	/**
	 * FIXME rework based on final idea for the example concept
	 * 
	 * 
	 * 
	 * Authenticated HTTP-connected devices can consume messages form the IoT
	 * Hub service by subscribing for server-sent events on arbitrary URIs. The
	 * topic of interest is part of the the HTTP request URI, each event in the
	 * stream contains {@code name} - specifying the message topic, and
	 * {@code data} - carrying the message payload.
	 * 
	 * <p>
	 * For example, an authenticated HTTP-connected device can consume messages
	 * from the IoT Things service using HTTP requests:
	 * 
	 * <p>
	 * GET /http-consumer/things/events/modified/&lt;device-id&gt; <br/>
	 * will consume messages with topic
	 * &lt;solution-id&gt;/things/events/modified/&lt;device-id&gt; via SSE
	 * 
	 * <p>
	 * GET /http-consumer/things/events/created/&lt;device-id&gt; <br/>
	 * will consume messages with topic
	 * &lt;solution-id&gt;/things/events/created/&lt;device-id&gt; via SSE
	 * 
	 * <p>
	 * GET /http-consumer/things/events/deleted/&lt;device-id&gt; <br/>
	 * will consume messages with topic
	 * &lt;solution-id&gt;/things/events/deleted/&lt;device-id&gt; via SSE
	 * 
	 * 
	 * @param request {@link HttpServletRequest} encapsulating the HTTP client
	 *            request information, it is used to access the request path and
	 *            session.
	 * @return {@link ResponseEntity} including the HTTP status code for the
	 *         operation. Possible values are 200 OK - if the messages has been
	 *         successfully sent to the IoT Hub services, or 400 Bad Request -
	 *         if any error occurs while sending the message.
	 */
	@RequestMapping(value = "/http-consumer/**", method = RequestMethod.GET, produces = "text/event-stream")
	public SseEmitter consumeMessages(HttpServletRequest request) {
		final TopicPath topic = extractTopicPath(request, "http-consumer/**");
		final String subscriptionId = topic.toString() + ':' + request.getSession().getId();

		LOGGER.info("Subscribing for server-sent events for messages with topic <{}> using client <{}>", topic,
				consumerClientId);
		SseEmitter emitter = new SseEmitter(120000L); // 2 minutes
		emitter.onCompletion(() -> {
			LOGGER.info("Closing server-sent events connection for messages with topic <{}>", topic);
			SubscriptionData consumer = subscriptions.remove(subscriptionId);
			LOGGER.info("Removing consumer registration for topic <{}> created by client <{}>", topic,
					consumerClientId);
			consumer.registration().unregister();

		});

		try {
			ConsumerRegistration registration = getIotHubClient().consume(message -> {
				TopicPath messageTopicPath = message.getTopicPath();
				Optional<Payload> payload = message.getPayload();
				LOGGER.info("Consuming message with topic <{}> using client <{}>", messageTopicPath, consumerClientId);

				// filter out messages which topic path does not match the topic of interest
				// currently iot hub client version does not support consuming messages for a specific topic 
				// as dispatching is based only on client identifiers and access control lists
				if (messageTopicPath.equals(topic)) {
					SseEventBuilder sseBuilder = SseEmitter.event().name(messageTopicPath.toString());
					payload.ifPresent(data -> sseBuilder.data(payload.get().getContentAsByteArray()));
					LOGGER.info("Sending server event for consumed message with topic <{}>", messageTopicPath);
					try {
						emitter.send(sseBuilder);
					} catch (IOException e) {
						LOGGER.info("Error while sending server event for message with topic <{}>", messageTopicPath,
								e);
						emitter.completeWithError(e);
					}
				} else {
					LOGGER.warn(
							"Dropping message with topic <{}>, topic path does not match the subscription path <{}>",
							messageTopicPath, topic);
				}

			});

			SubscriptionData consumer = SubscriptionData.of(emitter, registration);
			subscriptions.put(subscriptionId, consumer);

		} catch (NullPointerException | IllegalStateException e) {
			LOGGER.info("Error while subscribing for messages with topic <{}> using client <{}>", topic,
					consumerClientId, e);
		}
		return emitter;

	}

	/**
	 * FIXME
	 * 
	 * Subscribes for messages form the IoT Hub service via server-sent events
	 * on arbitrary URIs and displays the consumed server-sent events. The topic
	 * of interest is part of the HTTP request URI, each event in the stream
	 * contains {@code name} - specifying the message topic, and {@code data} -
	 * carrying the message payload.
	 * 
	 * @param request {@link HttpServletRequest} encapsulating the HTTP client
	 *            request information, it is used to access the request path
	 * @return
	 */
	@RequestMapping(value = "/http-consumer/sselog/**", method = RequestMethod.GET)
	public ModelAndView consumedMessagesLog(HttpServletRequest request) {
		String source = extractEventSource(request);
		String topic = extractTopicPath(request, "/http-consumer/sselog/**").toString();

		ModelAndView mav = new ModelAndView("sselog");

		mav.addObject("source", source);
		mav.addObject("topic", topic);
		return mav;
	}

	/**
	 * Extracts the topic path from the HTTP request path, the provided
	 * {@link HttpServletRequest} object is used to access the request path. The
	 * solution id (example namespace) is prepended to the topic path in order
	 * to construct the target message topic.
	 * 
	 * @param request {@link HttpServletRequest} encapsulating the HTTP client
	 *            request information, it is used to access the request path
	 * @param pattern pattern to match
	 * 
	 * @return the topic path of interest, solution id is prepended to the path
	 *         extracted form the HTTP request path
	 */
	private TopicPath extractTopicPath(HttpServletRequest request, String pattern) {
		String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		String topicPath = topicPathMatcher.extractPathWithinPattern(pattern, fullPath);
		return TopicPath.of(topicPath).prepend(solutionId);
	}

	private static final Pattern SOURCE_PATTERN = Pattern.compile("/http-consumer/sselog");

	private String extractEventSource(HttpServletRequest request) { // FIXME improve
		final String source = SOURCE_PATTERN.matcher(request.getRequestURL().toString()).replaceAll("/http-consumer");
		return source;
	}

	private void constructIotHubClient(Properties configuration) throws URISyntaxException {//FIXME exceptions?
		LOGGER.info("Creating IoT Hub integration client for client ID <{}>.", consumerClientId);

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
				.alias(keyAlias, keyAliasPassword).clientId(consumerClientId)
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
		LOGGER.info("Destroying Hub Integration Client for client ID <{}>.", iotHubClientId);
		iotHubclient.destroy();

	}

	private URI resourceURI(String name) throws URISyntaxException {
		return Thread.currentThread().getContextClassLoader().getResource(name).toURI();
	}

	private static class SubscriptionData {

		private SseEmitter emitter;
		private ConsumerRegistration registration;

		public static SubscriptionData of(SseEmitter emitter, ConsumerRegistration registration) {
			return new SubscriptionData(emitter, registration);
		}

		private SubscriptionData(SseEmitter emitter, ConsumerRegistration registration) {
			this.emitter = emitter;
			this.registration = registration;
		}

		public SseEmitter emitter() {
			return emitter;
		}

		public ConsumerRegistration registration() {
			return registration;
		}

	}

	private IotHubClient getIotHubClient() {
		if (!iotHubClient.isConnected()) {
			iotHubClient.connect();
		}
		return iotHubClient;
	}

}
