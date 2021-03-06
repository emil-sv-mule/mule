/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.security.config;

import static org.apache.ws.security.components.crypto.Merlin.TRUSTSTORE_FILE;
import static org.apache.ws.security.components.crypto.Merlin.TRUSTSTORE_PASSWORD;
import static org.apache.ws.security.components.crypto.Merlin.TRUSTSTORE_TYPE;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.Properties;

import org.apache.ws.security.components.crypto.Merlin;

/**
 * Default {@link WssStoreConfiguration} implementation for Trust Stores, used for signature verification.
 *
 * @since 4.0
 */
public class WssTrustStoreConfiguration implements WssStoreConfiguration {

  @Parameter
  @Summary("The location of the TrsutStore file")
  private String trustStorePath;

  @Parameter
  @Summary("The password to access the store.")
  @Password
  private String password;

  @Parameter
  @Optional(defaultValue = "jks")
  @Summary("The type of store (jks, pkcs12, jceks, or any other)")
  private String type;

  /**
   * {@inheritDoc}
   */
  @Override
  public String getStorePath() {
    return trustStorePath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPassword() {
    return password;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Properties getConfigurationProperties() {
    Properties properties = new Properties();
    properties.setProperty(WS_CRYPTO_PROVIDER_KEY, Merlin.class.getCanonicalName());
    properties.setProperty(TRUSTSTORE_FILE, trustStorePath);
    properties.setProperty(TRUSTSTORE_TYPE, type);
    properties.setProperty(TRUSTSTORE_PASSWORD, password);
    return properties;
  }
}
