// ========================================================================
// $Id: RequestLog.java,v 1.5 2004/05/09 20:31:40 gregwilkins Exp $
// Copyright 2000-2004 Mort Bay Consulting Pty. Ltd.
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

import net.lightbody.bmp.proxy.jetty.util.LifeCycle;

import java.io.Serializable;

/**
 * Abstract HTTP Request Log format.
 *
 * @author Tony Thompson
 * @author Greg Wilkins
 * @version $Id: RequestLog.java,v 1.5 2004/05/09 20:31:40 gregwilkins Exp $
 */
public interface RequestLog extends LifeCycle, Serializable {
    void log(HttpRequest request, HttpResponse response, int responseLength);
}

