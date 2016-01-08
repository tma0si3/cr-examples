/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
package com.bosch.cr.integration.examples.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This model class demonstrates how to use a custom serialization (in this case XML-Serialization with JAXB) for
 * Messages.
 */
@XmlRootElement(name = "User" )
public class ExampleUser
{
   public static final String USER_CUSTOM_CONTENT_TYPE = "application/vnd.my-company.user+xml";

   private String userName;
   private String email;

   public ExampleUser()
   {
      super();
   }

   public ExampleUser(String userName, String email)
   {
      this.userName = userName;
      this.email = email;
   }

   public String getUserName()
   {
      return userName;
   }

   public void setUserName(String userName)
   {
      this.userName = userName;
   }

   public String getEmail()
   {
      return email;
   }

   public void setEmail(String email)
   {
      this.email = email;
   }

   @Override
   public String toString()
   {
      return getClass().getSimpleName() + " [" +
         "userName=" + userName +
         ", email=" + email +
         "]";
   }
}
