//
//  ========================================================================
//  Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.rockhill.mitm.jetty.security;

import javax.servlet.ServletContext;

import org.rockhill.mitm.jetty.security.Authenticator.AuthConfiguration;
import org.rockhill.mitm.jetty.security.authentication.BasicAuthenticator;
import org.rockhill.mitm.jetty.security.authentication.ClientCertAuthenticator;
import org.rockhill.mitm.jetty.security.authentication.DigestAuthenticator;
import org.rockhill.mitm.jetty.security.authentication.FormAuthenticator;
import org.rockhill.mitm.jetty.server.Server;
import org.eclipse.jetty.util.security.Constraint;

/**
 * The Default Authenticator Factory.
 * Uses the {@link AuthConfiguration#getAuthMethod()} to select an {@link Authenticator} from: <ul>
 * <li>{@link org.rockhill.mitm.jetty.security.authentication.BasicAuthenticator}</li>
 * <li>{@link org.rockhill.mitm.jetty.security.authentication.DigestAuthenticator}</li>
 * <li>{@link org.rockhill.mitm.jetty.security.authentication.FormAuthenticator}</li>
 * <li>{@link org.rockhill.mitm.jetty.security.authentication.ClientCertAuthenticator}</li>
 * </ul>
 * All authenticators derived from {@link org.rockhill.mitm.jetty.security.authentication.LoginAuthenticator} are
 * wrapped with a {@link org.rockhill.mitm.jetty.security.authentication.DeferredAuthentication}
 * instance, which is used if authentication is not mandatory.
 *
 * The Authentications from the {@link org.rockhill.mitm.jetty.security.authentication.FormAuthenticator} are always wrapped in a
 * {@link org.rockhill.mitm.jetty.security.authentication.SessionAuthentication}
 * <p>
 * If a {@link LoginService} has not been set on this factory, then
 * the service is selected by searching the {@link Server#getBeans(Class)} results for
 * a service that matches the realm name, else the first LoginService found is used.
 */
public class DefaultAuthenticatorFactory implements Authenticator.Factory
{
    LoginService _loginService;

    @Override
    public Authenticator getAuthenticator(Server server, ServletContext context, AuthConfiguration configuration, IdentityService identityService, LoginService loginService)
    {
        String auth = configuration.getAuthMethod();
        Authenticator authenticator = null;

        if (auth == null || Constraint.__BASIC_AUTH.equalsIgnoreCase(auth))
            authenticator = new BasicAuthenticator();
        else if (Constraint.__DIGEST_AUTH.equalsIgnoreCase(auth))
            authenticator = new DigestAuthenticator();
        else if (Constraint.__FORM_AUTH.equalsIgnoreCase(auth))
            authenticator = new FormAuthenticator();
        if (Constraint.__CERT_AUTH.equalsIgnoreCase(auth) || Constraint.__CERT_AUTH2.equalsIgnoreCase(auth))
            authenticator = new ClientCertAuthenticator();

        return authenticator;
    }

    /**
     * @return the loginService
     */
    public LoginService getLoginService()
    {
        return _loginService;
    }

    /**
     * @param loginService the loginService to set
     */
    public void setLoginService(LoginService loginService)
    {
        _loginService = loginService;
    }
}
