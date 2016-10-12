/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.module.oauth2.internal.authorizationcode;

import static org.mule.runtime.module.http.api.HttpHeaders.Names.LOCATION;

import org.mule.extension.module.oauth2.internal.DynamicFlowFactory;
import org.mule.extension.module.oauth2.internal.StateEncoder;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.module.http.api.listener.HttpListener;
import org.mule.runtime.module.http.api.listener.HttpListenerBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the call to the localAuthorizationUrl and redirects the user to the oauth authentication server authorization url so
 * the user can grant access to the resources to the mule application.
 */
public class AuthorizationRequestHandler implements MuleContextAware {

  public static final String REDIRECT_STATUS_CODE = "302";
  public static final String OAUTH_STATE_ID_FLOW_VAR_NAME = "resourceOwnerId";

  private Logger logger = LoggerFactory.getLogger(AuthorizationRequestHandler.class);

  /**
   * Scope required by this application to execute. Scopes define permissions over resources.
   */
  @Parameter
  @Optional
  private String scopes;

  /**
   * State parameter for holding state between the authentication request and the callback done by the oauth authorization server
   * to the redirectUrl.
   */
  @Parameter
  @Optional
  private String state;

  /**
   * If this attribute is provided mule will automatically create and endpoint in the host server that the user can hit to
   * authenticate and grant access to the application for his account.
   */
  @Parameter
  private String localAuthorizationUrl;

  /**
   * The oauth authentication server url to authorize the app for a certain user.
   */
  @Parameter
  private String authorizationUrl;

  /**
   * Custom parameters to send to the authorization request url or the oauth authorization sever.
   */
  @Parameter
  @Optional
  @Alias("custom-parameters")
  private Map<String, String> customParameters = new HashMap<>();

  private HttpListener listener;

  private MuleContext muleContext;
  private AuthorizationCodeGrantType oauthConfig;

  public void init() throws MuleException {
    try {
      final HttpListenerBuilder httpListenerBuilder = new HttpListenerBuilder(muleContext);
      final String flowName = "authorization-request-handler-" + localAuthorizationUrl;
      final Flow flow = DynamicFlowFactory.createDynamicFlow(muleContext, flowName, createLocalAuthorizationUrlListener());
      httpListenerBuilder.setUrl(new URL(localAuthorizationUrl)).setSuccessStatusCode(REDIRECT_STATUS_CODE).setFlow(flow);
      if (oauthConfig.getTlsContext() != null) {
        httpListenerBuilder.setTlsContextFactory(oauthConfig.getTlsContext());
      }
      this.listener = httpListenerBuilder.build();
      this.listener.initialise();
      this.listener.start();
    } catch (MalformedURLException e) {
      logger.warn("Could not parse provided url %s. Validate that the url is correct", localAuthorizationUrl);
      throw new DefaultMuleException(e);
    }
  }

  private List<Processor> createLocalAuthorizationUrlListener() {
    final Processor listenerMessageProcessor = muleEvent -> {
      final Builder builder = Event.builder(muleEvent);

      final String onCompleteRedirectToValue =
          ((Map<String, String>) muleEvent.getMessage().getInboundProperty("http.query.params")).get("onCompleteRedirectTo");
      final String resourceOwnerId =
          getOauthConfig().getLocalAuthorizationUrlResourceOwnerIdEvaluator();
      muleEvent = builder.addVariable(OAUTH_STATE_ID_FLOW_VAR_NAME, resourceOwnerId).build();
      final StateEncoder stateEncoder = new StateEncoder(state);
      if (resourceOwnerId != null) {
        stateEncoder.encodeResourceOwnerIdInState(resourceOwnerId);
      }
      if (onCompleteRedirectToValue != null) {
        stateEncoder.encodeOnCompleteRedirectToInState(onCompleteRedirectToValue);
      }
      final String authorizationUrlWithParams = new AuthorizationRequestUrlBuilder().setAuthorizationUrl(authorizationUrl)
          .setClientId(oauthConfig.getClientId()).setClientSecret(oauthConfig.getClientSecret())
          .setCustomParameters(customParameters).setRedirectUrl(oauthConfig.getExternalCallbackUrl())
          .setState(stateEncoder.getEncodedState()).setScope(scopes).buildUrl();

      return builder
          .message(InternalMessage.builder(muleEvent.getMessage()).addOutboundProperty(LOCATION, authorizationUrlWithParams)
              .build())
          .build();
    };
    return Arrays.asList(listenerMessageProcessor);
  }

  public void setOauthConfig(final AuthorizationCodeGrantType oauthConfig) {
    this.oauthConfig = oauthConfig;
  }

  public AuthorizationCodeGrantType getOauthConfig() {
    return oauthConfig;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}