// ========================================================================
// $Id: MultiPartResponse.java,v 1.12 2006/04/04 22:28:02 gregwilkins Exp $
// Copyright 1996-2004 Mort Bay Consulting Pty. Ltd.
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

import net.lightbody.bmp.proxy.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Handle a multipart MIME response.
 *
 * @author Greg Wilkins
 * @author Jim Crossley
 * @version $Id: MultiPartResponse.java,v 1.12 2006/04/04 22:28:02 gregwilkins Exp $
 */
public class MultiPartResponse {
    private static final Logger log = LoggerFactory.getLogger(MultiPartResponse.class);

    private static byte[] __CRLF;
    private static byte[] __DASHDASH;

    static {
        try {
            __CRLF = "\015\012".getBytes(StringUtil.__ISO_8859_1);
            __DASHDASH = "--".getBytes(StringUtil.__ISO_8859_1);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            System.exit(1);
        }
    }

    private String boundary;
    private byte[] boundaryBytes;
    /**
     * PrintWriter to write content too.
     */
    private OutputStream out = null;
    
    private boolean inPart = false;

    private MultiPartResponse() {
        try {
            boundary = "jetty" + System.identityHashCode(this) + Long.toString(System.currentTimeMillis(), 36);
            boundaryBytes = boundary.getBytes(StringUtil.__ISO_8859_1);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            System.exit(1);
        }
    }

    public MultiPartResponse(OutputStream out) {
        this();
        this.out = out;
        inPart = false;
    }

    /**
     * MultiPartResponse constructor.
     */
    public MultiPartResponse(HttpResponse response) {
        this();
        response.setField(HttpFields.__ContentType, "multipart/mixed;boundary=" + boundary);
        out = response.getOutputStream();
        inPart = false;
    }

    public String getBoundary() {
        return boundary;
    }

    public OutputStream getOut() {
        return out;
    }

    /**
     * Start creation of the next Content.
     */
    public void startPart(String contentType) throws IOException {
        if (inPart) {
            out.write(__CRLF);
        }
        inPart = true;
        out.write(__DASHDASH);
        out.write(boundaryBytes);
        out.write(__CRLF);
        out.write(("Content-Type: " + contentType).getBytes(StringUtil.__ISO_8859_1));
        out.write(__CRLF);
        out.write(__CRLF);
    }

    /**
     * Start creation of the next Content.
     */
    public void startPart(String contentType, String[] headers) throws IOException {
        if (inPart) {
            out.write(__CRLF);
        }
        inPart = true;
        out.write(__DASHDASH);
        out.write(boundaryBytes);
        out.write(__CRLF);
        out.write(("Content-Type: " + contentType).getBytes(StringUtil.__ISO_8859_1));
        out.write(__CRLF);
        for (int i = 0; headers != null && i < headers.length; i++) {
            out.write(headers[i].getBytes(StringUtil.__ISO_8859_1));
            out.write(__CRLF);
        }
        out.write(__CRLF);
    }

    /**
     * End the current part.
     *
     * @throws IOException IOException
     */
    public void close() throws IOException {
        if (inPart) {
            out.write(__CRLF);
        }
        out.write(__DASHDASH);
        out.write(boundaryBytes);
        out.write(__DASHDASH);
        out.write(__CRLF);
        inPart = false;
    }

}
