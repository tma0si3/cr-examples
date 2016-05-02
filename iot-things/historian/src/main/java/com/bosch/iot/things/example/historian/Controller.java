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
package com.bosch.iot.things.example.historian;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class Controller
{

   private static final class Param
   {

      String thingId;
      String featureId;
      String propertyPath;

      private Param()
      {
      }

      private static Param createFromRequest()
      {
         HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
         String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
         Matcher matcher = pattern.matcher(fullPath);
         if (!matcher.matches())
         {
            throw new IllegalArgumentException(fullPath);
         }
         Param p = new Param();
         p.thingId = matcher.group(1);
         p.featureId = matcher.group(2);
         p.propertyPath = matcher.group(3);
         return p;
      }
   }

   private static final Pattern pattern = Pattern.compile("/history/.+?/(.+?)/(.+?)/(.+)");

   @Autowired
   private MongoTemplate mongoTemplate;

   @RequestMapping("/abc")
   public String abc()
   {
      return "OK";
   }

   @RequestMapping("/history/data/**")
   public Map getHistory()
   {
      Param p = Param.createFromRequest();
      String id = p.thingId + "/" + p.featureId + "/" + p.propertyPath;
      Map m = mongoTemplate.findById(id, Map.class, "history");
      m.remove("_id");
      return m;
   }

   @RequestMapping("/history/view/**")
   public ModelAndView getViewHistory()
   {
      Map m = getHistory();

      Param p = Param.createFromRequest();

      ModelAndView mav = new ModelAndView();
      mav.addObject("thingId", p.thingId);
      mav.addObject("featureId", p.featureId);
      mav.addObject("propertyPath", p.propertyPath);
      mav.setViewName("historyview");
      mav.addAllObjects(m);
      return mav;
   }

}
