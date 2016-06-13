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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
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
import com.bosch.iot.hub.model.message.Message;
import com.bosch.iot.hub.model.message.Payload;
import com.bosch.iot.hub.model.topic.TopicPath;

/**
 * Example HTTP connector service which manages:
 * <ul>
 * <li>sending of messages from authenticated HTTP-connected devices to the IoT Hub service via HTTP
 * POST requests against arbitrary URIs denoting the messages topics</li>
 * <li>delivering messages form the IoT Hub service to authenticated HTTP-connected devices via
 * server-sent events (SSE) streamed at arbitrary URIs denoting the message topics</li>
 * </ul>
 * 
 * <p>
 * The example HTTP connector service incorporates two IoT Hub integration clients in order to
 * communicate with the IoT Hub service. Settings needed for authentication and establishing a
 * connection with the IoT Hub service are loaded from the {@code config.properties} file - please
 * make sure that you have properly configured your solution id and key store settings before
 * starting the HTTP connector application.
 */
@RestController
public class HttpConnectorController {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpConnectorController.class);
	private static final long DEFAULT_TIMEOUT = 5; // 5 seconds

	private String solutionId; // solution id used to authenticate to the iot hub service
	private String senderId; // id of the client which will be used for sending messages to the iot hub service
	private String consumerId; // id of the client which will be used for consuming messages from th eiot hub service

	private IotHubClient sender; // client instance which will be used for sending messages to iot hub service
	private IotHubClient consumer; // client instance which will be used for consuming messages from the iot hub service

	private PathMatcher topicPathMatcher = new AntPathMatcher();
	private Map<String, SubscriptionData> subscriptions = new ConcurrentHashMap<>();

	/**
	 * Loads the HTTP consumer configuration form the {@code config.properties} file and creates the
	 * IoT Hub integration clients used for exchanging messages with the IoT Hub service.
	 */
	@PostConstruct
	public void postConstruct() {

		// load the configuration for the http connector example
		Properties configuration = new Properties(System.getProperties());
		try {
			configuration.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
		} catch (IOException e) {
			LOGGER.warn("Error while loading IoT Hub integration cleint configuration");
			throw new RuntimeException(e);
		}

		LOGGER.info("Loaded IoT Hub integration cleint configuration : {}", configuration);

		solutionId = configuration.getProperty("solutionId");
		senderId = configuration.getProperty("senderId", solutionId + ":http-connector-sender");
		consumerId = configuration.getProperty("consumerId", solutionId + ":http-connector-consumer");

		// create the clients used for exchanging messages with the iot hub service
		sender = constructIotHubClient(senderId, configuration);
		consumer = constructIotHubClient(consumerId, configuration);
	}

	/**
	 * Closes the currently opened HTTP connections used for delivering server-sent events, removes
	 * all active consumer registrations and destroys the IoT Hub integration clients used for
	 * exchanging messages with the IoT Hub service.
	 */
	@PreDestroy
	public void preDestroy() {

		// close all active sse connections
		// completing the sse emitters will remove the corresponding consumer registrations
		subscriptions.forEach((subscribtionId, consumer) -> consumer.emitter().complete());

		// destroy the hub integration clients used for exchanging messages
		// close the connection to back-end and clean up allocated resources
		destroyIotHubClient(consumerId, consumer);
		destroyIotHubClient(senderId, sender);

	}

	/**
	 * Authenticated HTTP-connected devices can send messages to the IoT Hub service by initiating
	 * an HTTP POST requests against arbitrary URIs. The message topic is derived from the HTTP
	 * request URI and the message payload is extracted from the HTTP request body.
	 * 
	 * <p>
	 * For example, an authenticated HTTP-connected device can send messages to the IoT Things
	 * service using HTTP requests:
	 * 
	 * <p>
	 * POST /http-connector/&lt;namespace&gt;/things/commands/modify/&lt;device-id& gt; <br/>
	 * will send hub message for topic &lt;namespace&gt;/things/commands/modify/&lt;device-id&gt;
	 * 
	 * <p>
	 * POST /http-connector/&lt;namespace&gt;/things/commands/delete/&lt;device-id& gt; <br/>
	 * will send hub message for topic &lt;namespace&gt;/things/commands/delete/&lt;device-id&gt;
	 * 
	 * <p>
	 * POST /http-connector/&lt;namespace&gt;/things/commands/create/&lt;device-id&gt; <br/>
	 * will send hub message for topic &&lt;namespace&gt;/things/commands/create/&lt;device-id&gt;
	 *
	 * <p>
	 * POST /http-connector/&lt;namespace&gt;/things/messages/toggle/&lt;device-id& gt;<br/>
	 * will send hub messages for topic &lt;namespace&gt;/things/messages/toggle/&lt;device-id&gt;
	 * 
	 * @param requestEntity {@link HttpEntity} encapsulating the HTTP request body and headers, it
	 *            is used to access the request body.
	 * @param request {@link HttpServletRequest} encapsulating the HTTP client request information,
	 *            it is used to access the request path and session.
	 * @return {@link ResponseEntity} including the HTTP status code for the operation. Possible
	 *         values are 201 Created - if the messages has been successfully sent to the IoT Hub
	 *         service, or 400 Bad Request - if any error occurs while sending the message.
	 */
	@RequestMapping(value = "/http-connector/messages/**", method = RequestMethod.POST)
	public ResponseEntity<Void> sendMessage(HttpEntity<byte[]> requestEntity, HttpServletRequest request) {
		final String topic = extractTopicPath(request, "http-connector/messages/**");
		LOGGER.info("Sending message at topic <{}> using client <{}>", topic, senderId);
		try {
			// TODO add support for polling the status of an asynchronously sent message via the REST API
			Payload payload = requestEntity.hasBody() ? Payload.of(requestEntity.getBody()) : Payload.empty();
			getMessageSender().send(Message.of(topic, payload)).get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
			LOGGER.warn("Error while sending message at topic <{}> using client <{}>", topic, senderId, e);
			return ResponseEntity.badRequest().build();
		}
		// URI included in the Location HTTP response header would point to the SSE source
		return ResponseEntity.created(URI.create(request.getRequestURI())).build();
	}

	/**
	 * Authenticated HTTP-connected devices can consume messages form the IoT Hub service by
	 * subscribing for server-sent events on arbitrary URIs. The topic of interest is derived from
	 * the HTTP request URI, each event in the stream contains {@code name} - specifying the message
	 * topic, and {@code data} - carrying the message payload. The media type of the {@code data}
	 * content may be defined by including {@code x-payload-media-type} header in the HTTP request.
	 * The value of the header must represent a valid media type definition.
	 * <p>
	 * For example, an authenticated HTTP-connected device can consume messages from the IoT Things
	 * service using HTTP requests:
	 * 
	 * <p>
	 * GET /http-connector/&lt;namespace&gt;/things/events/modified/&lt;device-id&gt; <br/>
	 * will consume messages with topic &lt;namespace&gt;/things/events/modified/&lt;device-id&gt;
	 * via SSE
	 * 
	 * <p>
	 * GET /http-connector/&lt;namespace&gt;/things/events/created/&lt;device-id&gt; <br/>
	 * will consume messages with topic &lt;namespace&gt;/things/events/created/&lt;device-id&gt;
	 * via SSE
	 * 
	 * <p>
	 * GET /http-connector/&lt;namespace&gt;/things/events/deleted/&lt;device-id&gt; <br/>
	 * will consume messages with topic &lt;namespace&gt;/things/events/deleted/&lt;device-id&gt;
	 * via SSE
	 * 
	 * 
	 * @param request {@link HttpServletRequest} encapsulating the HTTP client request information,
	 *            it is used to access the request path, session and headers.
	 * @return {@link ResponseEntity} including the HTTP status code for the operation. Possible
	 *         values are 200 OK - if the messages has been successfully sent to the IoT Hub
	 *         service, or 400 Bad Request - if any error occurs while consuming the message.
	 */
	@RequestMapping(value = "/http-connector/messages/**", method = RequestMethod.GET, produces = "text/event-stream")
	public SseEmitter consumeMessages(HttpServletRequest request) {
		final String topic = extractTopicPath(request, "http-connector/messages/**");
		final String subscriptionId = topic.toString() + ':' + request.getSession().getId();

		LOGGER.info("Subscribing for server-sent events for messages with topic <{}> using client <{}>", topic,
				consumerId);
		SseEmitter emitter = new SseEmitter(120000L); // 2 minutes
		emitter.onCompletion(() -> {
			LOGGER.info("Closing server-sent events connection for messages with topic <{}>", topic);
			SubscriptionData consumer = subscriptions.remove(subscriptionId);
			LOGGER.info("Removing consumer registration for topic <{}> created by client <{}>", topic, consumerId);
			consumer.registration().unregister();

		});

		try {
			ConsumerRegistration registration = getMessageConsumer().consume(message -> {
				TopicPath messageTopicPath = message.getTopicPath();
				Optional<Payload> payload = message.getPayload();
				LOGGER.info("Consuming message with topic <{}> using client <{}>", messageTopicPath, consumerId);

				// drop messages which topic path does not match the topic of interest or any of its sub-topics
				// currently iot hub client version does not support consuming messages for a specific topic 
				// as dispatching is based only on client identifiers and access control lists
				if (messageTopicPath.toString().startsWith(topic)) {
					SseEventBuilder sseBuilder = SseEmitter.event().name(messageTopicPath.toString());
					MediaType payloadMediaType = extractPayloadMediaType(request);
					payload.ifPresent(data -> sseBuilder.data(payload.get().getContentAsByteArray(), payloadMediaType));
					LOGGER.info("Sending server event for consumed message with topic <{}>", messageTopicPath);
					try {
						emitter.send(sseBuilder);
					} catch (IOException e) {
						LOGGER.warn("Error while sending server event for message with topic <{}>", messageTopicPath,
								e);
						emitter.completeWithError(e);
					}
				} else {
					LOGGER.info(
							"Dropping message with topic <{}>, topic path does not match the subscription path <{}>",
							messageTopicPath, topic);
				}

			});

			SubscriptionData consumer = SubscriptionData.of(emitter, registration);
			subscriptions.put(subscriptionId, consumer);

		} catch (NullPointerException | IllegalStateException e) {
			LOGGER.warn("Error while subscribing for messages with topic <{}> using client <{}>", topic, consumerId, e);
		}
		return emitter;

	}

	/**
	 * Subscribes for messages form the IoT Hub service via server-sent events on arbitrary URIs and
	 * displays the consumed server-sent events. The topic of interest is part of the HTTP request
	 * URI, each event in the stream contains {@code name} - specifying the message topic, and
	 * {@code data} - carrying the message payload.
	 * 
	 * @param request {@link HttpServletRequest} encapsulating the HTTP client request information,
	 *            it is used to access the request path
	 * @return {@link ModelAndView} managing subscription for SEE and displaying the message log
	 */
	@RequestMapping(value = "/http-connector/messagelog/**", method = RequestMethod.GET)
	public ModelAndView consumedMessagesLog(HttpServletRequest request) {
		String source = request.getRequestURL().toString().replaceFirst("/http-connector/messagelog/",
				"/http-connector/messages/");
		String topic = extractTopicPath(request, "/http-connector/messagelog/**");

		LOGGER.info("Displaying server-sent events log for messages with topic <{}> using source <{}>", topic, source);

		ModelAndView mav = new ModelAndView("messagelog");

		mav.addObject("source", source);
		mav.addObject("topic", topic);
		return mav;
	}

	/**
	 * Extracts the topic path from the HTTP request path, the provided {@link HttpServletRequest}
	 * object is used to access the HTTP request path.
	 * 
	 * @param request {@link HttpServletRequest} encapsulating the HTTP client request information,
	 *            it is used to access the HTTP request path
	 * @param pattern pattern to match
	 * 
	 * @return the topic path of interest
	 */
	private String extractTopicPath(HttpServletRequest request, String pattern) {
		String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		String topicPath = topicPathMatcher.extractPathWithinPattern(pattern, fullPath);
		return topicPath;
	}

	/**
	 * Extracts the requested media type for the message payload from the value of
	 * {@code x-payload-media-type} HTTP request header, the provided {@link HttpServletRequest}
	 * object is used to access the HTTP request headers. If this header is missing or its value
	 * does not represent a valid {@link MimeType} then {@link MediaType#TEXT_PLAIN} is returned.
	 * 
	 * @param request {@link HttpServletRequest} encapsulating the HTTP client request information,
	 *            it is used to access the HTTP request headers.
	 * @return {@link MimeType} representing the message payload mime type, used for serializing the
	 *         content of {@code data} fields in server-sent events.
	 */
	private MediaType extractPayloadMediaType(HttpServletRequest request) {
		String value = request.getHeader("x-payload-media-type");
		if (value != null) {
			try {
				return MediaType.valueOf(value);
			} catch (InvalidMediaTypeException e) {
				LOGGER.warn("Value of header x-payload-media-type does not represent a valid media type : {}", value,
						e);
			}
		}
		return MediaType.TEXT_PLAIN;
	}

	private IotHubClient constructIotHubClient(String clientId, Properties configuration) {
		LOGGER.info("Constructing IoT Hub integration client for client ID <{}>.", clientId);

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

		try {
			// provide required configuration (authentication configuration and iot hub endpoint)
			// proxy configuration is optional and can be added if the proxy configuration properties exist
			final IotHubClientBuilder.OptionalPropertiesSettable builder = DefaultIotHubClient.newBuilder()
					.endPoint(iotHubEndpoint)
					.keyStore(Thread.currentThread().getContextClassLoader().getResource(keystoreLocation).toURI(),
							keystorePassword)
					.alias(keyAlias, keyAliasPassword).clientId(clientId);

			// http proxy settings, optional
			String httpProxyHost = configuration.getProperty("httpProxyHost");
			String httpProxyPort = configuration.getProperty("httpProxyPort");
			String httpProxyPrincipal = configuration.getProperty("httpProxyPrincipal");
			String httpProxyPassword = configuration.getProperty("httpProxyPassword");

			// configure http proxy for the client, if provided
			if (httpProxyHost != null && httpProxyPort != null) {
				IotHubClientBuilder.OptionalProxyPropertiesSettable proxy = builder
						.proxy(URI.create("http://" + httpProxyHost + ':' + httpProxyPort));

				if (httpProxyPrincipal != null && httpProxyPassword != null) {
					proxy.proxyAuthentication(httpProxyPrincipal, httpProxyPassword);
				}
			}

			IotHubClient client = builder.build(); // build the client
			client.connect(); // establish a connection with the iot hub service

			return client;

		} catch (URISyntaxException e) {
			LOGGER.info("Error while constructing IoT Hub integration client for client ID <{}>.", clientId);
			throw new RuntimeException(e);
		}
	}

	private void destroyIotHubClient(String clientId, IotHubClient client) {
		LOGGER.info("Destroying IoT Hub integration client for client ID <{}>.", clientId);
		client.destroy();

	}

	private IotHubClient getMessageSender() {
		// temporally, before invoking operations on the client, we need to check the client's connection and reconnect, if needed
		// as currently the web socket connection opened by the hub client is silently closed on client inactivity
		// which causes consequent attempts to communicate with the hub to fail
		if (!sender.isConnected()) {
			sender.connect();
		}
		return sender;
	}

	private IotHubClient getMessageConsumer() {
		// temporally, before invoking operations on the client, we need to check the client's connection and reconnect, if needed
		// as currently the web socket connection opened by the hub client is silently closed on client inactivity
		// which causes consequent attempts to communicate with the hub to fail
		if (!consumer.isConnected()) {
			consumer.connect();
		}
		return consumer;
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

}
