// ========================================================================
// $Id: Version.java,v 1.16 2004/10/20 12:46:20 gregwilkins Exp $
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

package net.lightbody.bmp.proxy.jetty.http;

/**
 * Jetty version.
 * <p>
 * This class sets the version data returned in the Server and Servlet-Container headers.
 * If the java.org.mortbay.http.Version.paranoid System property is set to true, then this information is suppressed.
 *
 * @author Greg Wilkins (gregw)
 * @version $Revision: 1.16 $
 */
public class Version {
    private static final boolean __paranoid = Boolean.getBoolean("net.lightbody.bmp.proxy.jetty.http.Version.paranoid");

    private static final String __Version = "Jetty/5.1";
    private static final String __notice = "This application is using software from the " +
            __Version +
            " HTTP server and servlet container.\nJetty is Copyright (c) Mort Bay Consulting Pty. Ltd. (Australia) and others.\nJetty is distributed under an open source license.\nThe license and standard release of Jetty are available from http://jetty.jetty.org\n";
    private static String __VersionImpl = __Version + ".x";
    private static String __VersionDetail = "Unknown";

    static {
        updateVersion();
    }

    public static String getVersion() {
        return __Version;
    }

    public static String getImplVersion() {
        return __VersionImpl;
    }

    public static String getDetail() {
        return __VersionDetail;
    }

    public static boolean isParanoid() {
        return __paranoid;
    }

    public static void main(String[] arg) {
        System.out.println(__notice);
        System.out.println("net.lightbody.bmp.proxy.jetty.http.Version=" + __Version);
        System.out.println("net.lightbody.bmp.proxy.jetty.http.VersionImpl=" + __VersionImpl);
        System.out.println("net.lightbody.bmp.proxy.jetty.http.VersionDetail=" + __VersionDetail);
    }

    public static void updateVersion() {
        Package p = Version.class.getPackage();
        if (p != null && p.getImplementationVersion() != null) {
            __VersionImpl = "Jetty/" + p.getImplementationVersion();
        }

        if (!__paranoid) {
            __VersionDetail = __VersionImpl +
                    " (" + System.getProperty("os.name") +
                    "/" + System.getProperty("os.version") +
                    " " + System.getProperty("os.arch") +
                    " java/" + System.getProperty("java.version") + ")";
        }
    }
}

