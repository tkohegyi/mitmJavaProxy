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

import java.security.Principal;
import javax.security.auth.Subject;

import org.rockhill.mitm.jetty.server.UserIdentity;

/**
 * Default Identity Service implementation.
 * This service handles only role reference maps passed in an
 * associated {@link org.rockhill.mitm.jetty.server.UserIdentity.Scope}.  If there are roles
 * refs present, then associate will wrap the UserIdentity with one
 * that uses the role references in the
 * {@link org.rockhill.mitm.jetty.server.UserIdentity#isUserInRole(String, org.rockhill.mitm.jetty.server.UserIdentity.Scope)}
 * implementation. All other operations are effectively noops.
 */
public class DefaultIdentityService implements IdentityService
{

    public DefaultIdentityService()
    {
    }

    /**
     * If there are roles refs present in the scope, then wrap the UserIdentity
     * with one that uses the role references in the {@link UserIdentity#isUserInRole(String, org.rockhill.mitm.jetty.server.UserIdentity.Scope)}
     */
    @Override
    public Object associate(UserIdentity user)
    {
        return null;
    }

    @Override
    public void disassociate(Object previous)
    {
    }

    @Override
    public Object setRunAs(UserIdentity user, RunAsToken token)
    {
        return token;
    }

    @Override
    public void unsetRunAs(Object lastToken)
    {
    }

    @Override
    public RunAsToken newRunAsToken(String runAsName)
    {
        return new RoleRunAsToken(runAsName);
    }

    @Override
    public UserIdentity getSystemUserIdentity()
    {
        return null;
    }

    @Override
    public UserIdentity newUserIdentity(final Subject subject, final Principal userPrincipal, final String[] roles)
    {
        return new DefaultUserIdentity(subject, userPrincipal, roles);
    }
}
