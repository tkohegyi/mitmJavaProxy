// ========================================================================
// $Id: FormAuthenticator.java,v 1.32 2005/08/13 00:01:27 gregwilkins Exp $
// Copyright 199-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package net.lightbody.bmp.proxy.jetty.jetty.servlet;

import net.lightbody.bmp.proxy.jetty.http.Authenticator;
import net.lightbody.bmp.proxy.jetty.http.HttpRequest;
import net.lightbody.bmp.proxy.jetty.http.HttpResponse;
import net.lightbody.bmp.proxy.jetty.http.SSORealm;
import net.lightbody.bmp.proxy.jetty.http.SecurityConstraint;
import net.lightbody.bmp.proxy.jetty.http.UserRealm;
import net.lightbody.bmp.proxy.jetty.util.Credential;
import net.lightbody.bmp.proxy.jetty.util.Password;
import net.lightbody.bmp.proxy.jetty.util.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;

/**
 * FORM Authentication Authenticator.
 * The HTTP Session is used to store the authentication status of the
 * user, which can be distributed.
 * If the realm implements SSORealm, SSO is supported.
 *
 * @author Greg Wilkins (gregw)
 * @author dan@greening.name
 * @version $Id: FormAuthenticator.java,v 1.32 2005/08/13 00:01:27 gregwilkins Exp $
 */
public class FormAuthenticator implements Authenticator {
    
    public static final String __J_URI = "net.lightbody.bmp.proxy.jetty.jetty.URI";
    public static final String __J_AUTHENTICATED = "net.lightbody.bmp.proxy.jetty.jetty.Auth";
    public static final String __J_SECURITY_CHECK = "j_security_check";
    public static final String __J_USERNAME = "j_username";
    public static final String __J_PASSWORD = "j_password";
    private static final Logger log = LoggerFactory.getLogger(FormAuthenticator.class);
    private String _formErrorPage;
    private String _formErrorPath;
    private String _formLoginPage;
    private String _formLoginPath;

    public String getAuthMethod() {
        return HttpServletRequest.FORM_AUTH;
    }

    public String getLoginPage() {
        return _formLoginPage;
    }

    public void setLoginPage(String path) {
        if (!path.startsWith("/")) {
            log.warn("form-login-page must start with /");
            path = "/" + path;
        }
        _formLoginPage = path;
        _formLoginPath = path;
        if (_formLoginPath.indexOf('?') > 0) {
            _formLoginPath = _formLoginPath.substring(0, _formLoginPath.indexOf('?'));
        }
    }

    public String getErrorPage() {
        return _formErrorPage;
    }

    public void setErrorPage(String path) {
        if (path == null || path.trim().length() == 0) {
            _formErrorPath = null;
            _formErrorPage = null;
        } else {
            if (!path.startsWith("/")) {
                log.warn("form-error-page must start with /");
                path = "/" + path;
            }
            _formErrorPage = path;
            _formErrorPath = path;

            if (_formErrorPath.indexOf('?') > 0) {
                _formErrorPath = _formErrorPath.substring(0, _formErrorPath.indexOf('?'));
            }
        }
    }

    /**
     * Perform form authentication. Called from SecurityHandler.
     *
     * @return UserPrincipal if authenticated else null.
     */
    public Principal authenticate(UserRealm realm, String pathInContext, HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        HttpServletRequest request = (ServletHttpRequest) httpRequest.getWrapper();
        HttpServletResponse response = httpResponse == null ? null : (HttpServletResponse) httpResponse.getWrapper();

        // Handle paths
        String uri = pathInContext;

        // Setup session 
        HttpSession session = request.getSession(response != null);
        if (session == null)
            return null;

        // Handle a request for authentication.
        if (uri.substring(uri.lastIndexOf("/") + 1).startsWith(__J_SECURITY_CHECK)) {
            // Check the session object for login info.
            FormCredential form_cred = new FormCredential();
            form_cred.authenticate(realm,
                    request.getParameter(__J_USERNAME),
                    request.getParameter(__J_PASSWORD),
                    httpRequest);

            String nuri = (String) session.getAttribute(__J_URI);
            if (nuri == null || nuri.length() == 0) {
                nuri = request.getContextPath();
                if (nuri.length() == 0) {
                    nuri = "/";
                }
            }

            if (form_cred._userPrincipal != null) {
                // Authenticated OK
                log.debug("Form authentication OK for {}", form_cred._jUserName);
                session.removeAttribute(__J_URI); // Remove popped return URI.
                httpRequest.setAuthType(SecurityConstraint.__FORM_AUTH);
                httpRequest.setAuthUser(form_cred._jUserName);
                httpRequest.setUserPrincipal(form_cred._userPrincipal);
                session.setAttribute(__J_AUTHENTICATED, form_cred);

                // Sign-on to SSO mechanism
                if (realm instanceof SSORealm) {
                    ((SSORealm) realm).setSingleSignOn(httpRequest,
                            httpResponse,
                            form_cred._userPrincipal,
                            new Password(form_cred._jPassword));
                }

                // Redirect to original request
                if (response != null) {
                    response.setContentLength(0);
                    response.sendRedirect(response.encodeRedirectURL(nuri));
                }
            } else if (response != null) {
                log.debug("Form authentication FAILED for {}", form_cred._jUserName);
                if (_formErrorPage != null) {
                    response.setContentLength(0);
                    response.sendRedirect(response.encodeRedirectURL(URI.addPaths(request.getContextPath(), _formErrorPage)));
                } else {
                    response.sendError(HttpResponse.__403_Forbidden);
                }
            }

            // Security check is always false, only true after final redirection.
            return null;
        }

        // Check if the session is already authenticated.
        FormCredential form_cred = (FormCredential) session.getAttribute(__J_AUTHENTICATED);

        if (form_cred != null) {
            // We have a form credential. Has it been distributed?
            if (form_cred._userPrincipal == null) {
                // This form_cred appears to have been distributed.  Need to reauth
                form_cred.authenticate(realm, httpRequest);

                // Sign-on to SSO mechanism
                if (form_cred._userPrincipal != null && realm instanceof SSORealm) {
                    ((SSORealm) realm).setSingleSignOn(httpRequest,
                            httpResponse,
                            form_cred._userPrincipal,
                            new Password(form_cred._jPassword));
                }
            } else if (!realm.reauthenticate(form_cred._userPrincipal))
                // Else check that it is still authenticated.
                form_cred._userPrincipal = null;

            // If this credential is still authenticated
            if (form_cred._userPrincipal != null) {
                log.debug("FORM Authenticated for {}", form_cred._userPrincipal.getName());
                httpRequest.setAuthType(SecurityConstraint.__FORM_AUTH);
                httpRequest.setAuthUser(form_cred._userPrincipal.getName());
                httpRequest.setUserPrincipal(form_cred._userPrincipal);
                return form_cred._userPrincipal;
            } else {
                session.setAttribute(__J_AUTHENTICATED, null);
            }
        } else if (realm instanceof SSORealm) {
            // Try a single sign on.
            Credential cred = ((SSORealm) realm).getSingleSignOn(httpRequest, httpResponse);

            if (httpRequest.hasUserPrincipal()) {
                form_cred = new FormCredential();
                form_cred._userPrincipal = request.getUserPrincipal();
                form_cred._jUserName = form_cred._userPrincipal.getName();
                if (cred != null) {
                    form_cred._jPassword = cred.toString();
                }
                log.debug("SSO for {}", form_cred._userPrincipal);

                httpRequest.setAuthType(SecurityConstraint.__FORM_AUTH);
                session.setAttribute(__J_AUTHENTICATED, form_cred);
                return form_cred._userPrincipal;
            }
        }

        // Don't authenticate authform or errorpage
        if (isLoginOrErrorPage(pathInContext)) {
            return SecurityConstraint.__NOBODY;
        }

        // redirect to login page
        if (response != null) {
            if (httpRequest.getQuery() != null) {
                uri += "?" + httpRequest.getQuery();
            }
            session.setAttribute(__J_URI,
                    request.getScheme() +
                            "://" + request.getServerName() +
                            ":" + request.getServerPort() +
                            URI.addPaths(request.getContextPath(), uri));
            response.setContentLength(0);
            response.sendRedirect(response.encodeRedirectURL(URI.addPaths(request.getContextPath(), _formLoginPage)));
        }

        return null;
    }

    public boolean isLoginOrErrorPage(String pathInContext) {
        return pathInContext != null && (pathInContext.equals(_formErrorPath) || pathInContext.equals(_formLoginPath));
    }

    /**
     * FORM Authentication credential holder.
     */
    private static class FormCredential implements Serializable, HttpSessionBindingListener {
        String _jUserName;
        String _jPassword;
        transient Principal _userPrincipal;
        transient UserRealm _realm;

        void authenticate(UserRealm realm, String user, String password, HttpRequest request) {
            _jUserName = user;
            _jPassword = password;
            _userPrincipal = realm.authenticate(user, password, request);
            if (_userPrincipal != null) {
                _realm = realm;
            }
        }

        void authenticate(UserRealm realm, HttpRequest request) {
            _userPrincipal = realm.authenticate(_jUserName, _jPassword, request);
            if (_userPrincipal != null) {
                _realm = realm;
            }
        }

        public void valueBound(HttpSessionBindingEvent event) {
        }

        public void valueUnbound(HttpSessionBindingEvent event) {
            log.debug("Logout {}", _jUserName);

            if (_realm instanceof SSORealm) {
                ((SSORealm) _realm).clearSingleSignOn(_jUserName);
            }

            if (_realm != null && _userPrincipal != null) {
                _realm.logout(_userPrincipal);
            }
        }

        public int hashCode() {
            return _jUserName.hashCode() + _jPassword.hashCode();
        }

        public boolean equals(Object o) {
            if (!(o instanceof FormCredential)) {
                return false;
            }
            FormCredential fc = (FormCredential) o;
            return _jUserName.equals(fc._jUserName) && _jPassword.equals(fc._jPassword);
        }

        public String toString() {
            return "Cred[" + _jUserName + "]";
        }

    }
}
