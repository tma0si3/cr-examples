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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.SseFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code HttpConsumer} is used for consuming messages from the Bosch IoT Hub
 * service via subscribing for server-sent events through the example HTTP
 * consumer service. {@code HttpConsumer} uses Jersey JAX-RS client in order to
 * subscribe for server-sent events streamed at arbitrary URIs denoting the
 * message topics.
 */
public class HttpConsumer {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpConsumer.class);

	private Client client;
	private List<EventSource> sources = new LinkedList<>();

	/**
	 * Creates new {@link HttpConsumer} instance and initialized a Jersey JAX-RS
	 * client for consuming server-sent events
	 */
	public HttpConsumer() {
		LOGGER.info("Initializing Jersey JAX-RS client");
//		HttpAuthenticationFeature authentication = HttpAuthenticationFeature.basic(username, password);
		client = ClientBuilder.newBuilder().register(SseFeature.class).build();
	}

	public void consumeMessages(String topic) {
		LOGGER.info("Subscribing for server-sent events from source <{}>", topic);
		WebTarget target = client.target("http://localhost:8080/http-consumer").path(topic);
		EventSource source = EventSource.target(target).build();
		source.register(
				message -> LOGGER.info("Received server-sent event for message with topic <{}> and payload <{}>",
						message.getName(), message.readData(String.class)));
		source.open();
		sources.add(source);
	}
	
	public void close() {
		LOGGER.info("Closing server-sent events sources and JAX-RS client");
		sources.forEach(source -> source.close());
		client.close();
	}

	public static void main(String[] args) {

//		String username = "device2";
//		String password = "device2__password!";
		String topic = args[0];

		final HttpConsumer device = new HttpConsumer();
		device.consumeMessages(topic);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		in.lines().anyMatch(line -> line.equals("exit"));
	}

}
