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

package org.rockhill.mitm.jetty.security.authentication;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.rockhill.mitm.jetty.security.Authenticator;
import org.rockhill.mitm.jetty.security.IdentityService;
import org.rockhill.mitm.jetty.security.LoginService;
import org.rockhill.mitm.jetty.server.Request;
import org.rockhill.mitm.jetty.server.Response;
import org.rockhill.mitm.jetty.server.UserIdentity;
import org.rockhill.mitm.jetty.server.session.Session;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public abstract class LoginAuthenticator implements Authenticator
{
    private static final Logger LOG = Log.getLogger(LoginAuthenticator.class);

    protected LoginService _loginService;
    protected IdentityService _identityService;
    private boolean _renewSession;

    protected LoginAuthenticator()
    {
    }

    @Override
    public void prepareRequest(ServletRequest request)
    {
        //empty implementation as the default
    }

    /**
     * If the UserIdentity is not null after this method calls {@link LoginService#login(String, Object, ServletRequest)}, it
     * is assumed that the user is fully authenticated and we need to change the session id to prevent
     * session fixation vulnerability. If the UserIdentity is not necessarily fully
     * authenticated, then subclasses must override this method and
     * determine when the UserIdentity IS fully authenticated and renew the session id.
     *
     * @param username the username of the client to be authenticated
     * @param password the user's credential
     * @param servletRequest the inbound request that needs authentication
     */
    public UserIdentity login(String username, Object password, ServletRequest servletRequest)
    {
        UserIdentity user = _loginService.login(username, password, servletRequest);
        if (user != null)
        {
            Request request = Request.getBaseRequest(servletRequest);
            renewSession(request, request == null ? null : request.getResponse());
            return user;
        }
        return null;
    }

    public void logout(ServletRequest request)
    {
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpSession session = httpRequest.getSession(false);
        if (session == null)
            return;

        session.removeAttribute(Session.SESSION_CREATED_SECURE);
    }

    @Override
    public void setConfiguration(AuthConfiguration configuration)
    {
        _loginService = configuration.getLoginService();
        if (_loginService == null)
            throw new IllegalStateException("No LoginService for " + this + " in " + configuration);
        _identityService = configuration.getIdentityService();
        if (_identityService == null)
            throw new IllegalStateException("No IdentityService for " + this + " in " + configuration);
        _renewSession = configuration.isSessionRenewedOnAuthentication();
    }

    public LoginService getLoginService()
    {
        return _loginService;
    }

    /**
     * Change the session id.
     * The session is changed to a new instance with a new ID if and only if:<ul>
     * <li>A session exists.
     * <li>The {@link org.rockhill.mitm.jetty.security.Authenticator.AuthConfiguration#isSessionRenewedOnAuthentication()} returns true.
     * <li>The session ID has been given to unauthenticated responses
     * </ul>
     *
     * @param request the request
     * @param response the response
     * @return The new session.
     */
    protected HttpSession renewSession(HttpServletRequest request, HttpServletResponse response)
    {
        HttpSession httpSession = request.getSession(false);

        if (_renewSession && httpSession != null)
        {
            synchronized (httpSession)
            {
                //if we should renew sessions, and there is an existing session that may have been seen by non-authenticated users
                //(indicated by SESSION_SECURED not being set on the session) then we should change id
                if (httpSession.getAttribute(Session.SESSION_CREATED_SECURE) != Boolean.TRUE)
                {
                    if (httpSession instanceof Session)
                    {
                        Session s = (Session)httpSession;
                        String oldId = s.getId();
                        s.renewId(request);
                        s.setAttribute(Session.SESSION_CREATED_SECURE, Boolean.TRUE);
                        if (s.isIdChanged() && (response instanceof Response))
                            ((Response)response).replaceCookie(s.getSessionHandler().getSessionCookie(s, request.getContextPath(), request.isSecure()));
                        if (LOG.isDebugEnabled())
                            LOG.debug("renew {}->{}", oldId, s.getId());
                    }
                    else
                    {
                        LOG.warn("Unable to renew session " + httpSession);
                    }
                    return httpSession;
                }
            }
        }
        return httpSession;
    }
}
