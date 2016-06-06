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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.StatusType;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME
 * 
 * 
 * {@code HttpDevice} is used for simulating HTTP devices that connect to the
 * Bosch IoT Hub service via HTTP through the example HTTP connector service.
 * {@code HttpDevice} uses Jersey JAX-RS client in order to send data by
 * initiating HTTP PUT request against arbitrary URIs denoting the topics.
 * 
 * The HTTP devices use basic authentication to authenticate to the HTTP
 * connector, thus user name and password must be provided as arguments when
 * running HttpDevice as Java application. In order for the device to be
 * authorized to communicate with the connector service, the user name and
 * password must be configured to the HTTP connector's
 * {@code credentials.properties} file.
 * 
 */
public class HttpDevice {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpDevice.class);

	private Client client;

	/**
	 * Creates new {@link HttpDevice} instance and configures a Jersey JAX-RS
	 * client for sending HTTP PUT requests.
	 * 
	 * @param username username used to authenticate to the HTTP connector using
	 *            basic authentication
	 * @param password password used to authenticate to the HTTP connector using
	 *            basic authentication
	 */
	public HttpDevice(String username, String password) {
		LOGGER.info("Initializing Jersey JAX-RS client");
		HttpAuthenticationFeature authentication = HttpAuthenticationFeature.basic(username, password);
		client = ClientBuilder.newBuilder().register(authentication).build();
	}

	public void sendMessage(String topic, String payload) {
		LOGGER.info("Sending request at http://localhost:8081/http-connector/<{}> and body <{}>", topic, payload);
		WebTarget target = client.target("http://localhost:8081/http-connector").path(topic);
		try {
			StatusType status = target.request().put(Entity.entity(payload, MediaType.TEXT_PLAIN)).getStatusInfo();
			if (status.getStatusCode() != HttpURLConnection.HTTP_OK) {
				LOGGER.error("Error while sending message with topic <{}>: {} {}", topic, status.getStatusCode(),
						status.getReasonPhrase());
			}
		} catch (IllegalArgumentException | ProcessingException e) {
			LOGGER.warn("Error while sending message with topic <{}>", topic, e);
		}
	}

	public void close() {
		LOGGER.info("Closing JAX-RS client");
		client.close();
	}

	public static void main(String[] args) {
		//		String username = args[0];
		//		String password = args[1];

		String username = "device2";
		String password = "device2__password!";

		final HttpDevice device = new HttpDevice(username, password);

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		in.lines().map(line -> line.split("\\s+")).forEach(command -> device.sendMessage(command[0], command[1]));
	}

}
