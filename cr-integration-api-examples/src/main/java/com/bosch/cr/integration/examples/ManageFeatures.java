/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
package com.bosch.cr.integration.examples;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bosch.cr.integration.things.FeatureHandle;
import com.bosch.cr.integration.things.ThingHandle;
import com.bosch.cr.json.JsonFactory;
import com.bosch.cr.json.JsonPointer;
import com.bosch.cr.json.JsonValue;
import com.bosch.cr.model.things.FeatureProperties;
import com.bosch.cr.model.things.Features;
import com.bosch.cr.model.things.Thing;
import com.bosch.cr.model.things.ThingsModelFactory;

/**
 * This example shows how a {@link ThingHandle} and {@link FeatureHandle} can be used to perform CRUD (Create, Read,
 * Update, and Delete) operations on {@link Features} and {@link FeatureProperties}.
 */
public class ManageFeatures extends ExamplesBase
{

   private static final Logger LOGGER = LoggerFactory.getLogger(ManageFeatures.class);

   private static final int TIMEOUT = 5;

   private static final String NAMESPACE = "com.bosch.cr.integration.examples.ManageFeatures:";
   private static final String FEATURE_ID = "smokeDetector";
   private static final JsonPointer PROPERTY_JSON_POINTER = JsonFactory.newPointer("density");
   private static final JsonValue PROPERTY_JSON_VALUE = JsonFactory.newValue(0.7);

   public void crudFeature() throws InterruptedException, ExecutionException, TimeoutException
   {
      LOGGER.info("Starting: {}()", Thread.currentThread().getStackTrace()[1].getMethodName());

      final String thingId = NAMESPACE + UUID.randomUUID().toString();
      final Thing thing = ThingsModelFactory.newThingBuilder() //
         .setId(thingId) //
         .build();

      thingIntegration.create(thing).get(TIMEOUT, SECONDS);

      final ThingHandle thingHandle = thingIntegration.forId(thingId);

      thingHandle.registerForFeatureChange("",
         featureChange -> LOGGER.info("{} Feature '{}'", featureChange.getAction(), featureChange.getFeature()));

      thingHandle.putFeature(ThingsModelFactory.newFeature(FEATURE_ID)) //
         .thenCompose(aVoid -> thingHandle.forFeature(FEATURE_ID).retrieve()) //
         .thenCompose(feature -> {
            LOGGER.info("RETRIEVED Feature '{}'", feature);
            return thingHandle.putFeature(ThingsModelFactory.newFeature(FEATURE_ID) //
               .setProperty(PROPERTY_JSON_POINTER, PROPERTY_JSON_VALUE));
         }).thenCompose(aVoid -> thingHandle.forFeature(FEATURE_ID).delete());
   }

   public void crudFeatureProperty() throws InterruptedException, ExecutionException, TimeoutException
   {
      LOGGER.info("Starting: {}()", Thread.currentThread().getStackTrace()[1].getMethodName());

      final String thingId = NAMESPACE + UUID.randomUUID().toString();
      final Thing thing = ThingsModelFactory.newThingBuilder() //
         .setId(thingId) //
         .setFeature(ThingsModelFactory.newFeature(FEATURE_ID)) //
         .build();

      thingIntegration.create(thing).get(TIMEOUT, SECONDS);

      final FeatureHandle featureHandle = thingIntegration.forFeature(thingId, FEATURE_ID);

      featureHandle.registerForPropertyChange("", PROPERTY_JSON_POINTER,
         featurePropertyChange -> LOGGER.info("{} Property '{}:{}'", featurePropertyChange.getAction(),
            featurePropertyChange.getPath(), featurePropertyChange.getValue()));

      featureHandle.putProperty(PROPERTY_JSON_POINTER, PROPERTY_JSON_VALUE) //
         .thenCompose(aVoid -> featureHandle.retrieve()) //
         .thenCompose(feature -> {
            LOGGER.info("RETRIEVED Property '{}'", feature.getProperty(PROPERTY_JSON_POINTER));
            return featureHandle.putProperty(PROPERTY_JSON_POINTER, 0.9);
         }) //
         .thenCompose(aVoid -> featureHandle.deleteProperty(PROPERTY_JSON_POINTER)) //
         .get(TIMEOUT, SECONDS);
   }

   public void crudFeatureProperties() throws InterruptedException, ExecutionException, TimeoutException
   {
      LOGGER.info("Starting: {}()", Thread.currentThread().getStackTrace()[1].getMethodName());

      final String thingId = NAMESPACE + UUID.randomUUID().toString();
      final Thing thing = ThingsModelFactory.newThingBuilder() //
         .setId(thingId) //
         .setFeature(ThingsModelFactory.newFeature(FEATURE_ID)) //
         .build();

      thingIntegration.create(thing).get(TIMEOUT, SECONDS);

      final FeatureHandle featureHandle = thingIntegration.forFeature(thingId, FEATURE_ID);

      featureHandle.registerForPropertyChange("", featurePropertyChange -> LOGGER.info("{} Properties '{}:{}'",
         featurePropertyChange.getAction(), featurePropertyChange.getPath(), featurePropertyChange.getValue()));

      featureHandle
         .setProperties(ThingsModelFactory.newFeaturePropertiesBuilder() //
            .set(PROPERTY_JSON_POINTER, PROPERTY_JSON_VALUE) //
            .build()) //
         .thenCompose(aVoid -> featureHandle.retrieve()) //
         .thenCompose(feature -> {
            LOGGER.info("RETRIEVED Properties '{}'", feature.getProperties());
            return featureHandle.setProperties(ThingsModelFactory.newFeaturePropertiesBuilder() //
               .set(PROPERTY_JSON_POINTER, 0.9) //
               .build());
         }).thenCompose(aVoid -> featureHandle.deleteProperties()) //
         .get(TIMEOUT, SECONDS);
   }

}
