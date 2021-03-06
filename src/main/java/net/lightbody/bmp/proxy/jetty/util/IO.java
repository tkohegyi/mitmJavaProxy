// ========================================================================
// $Id: IO.java,v 1.13 2005/08/13 00:01:28 gregwilkins Exp $
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

package net.lightbody.bmp.proxy.jetty.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * IO Utilities.
 * Provides stream handling utilities in singleton Threadpool implementation accessed by static members.
 */
public class IO extends ThreadPool {
    
    public final static String CRLF = "\015\012";
    
    public final static byte[] CRLF_BYTES = {(byte) '\015', (byte) '\012'};
    
    public static int bufferSize = Integer.getInteger("net.lightbody.bmp.proxy.jetty.util.IO.bufferSize", 8192);
    private static final Logger log = LoggerFactory.getLogger(IO.class);
    private static final NullOS __nullStream = new NullOS();
    private static final NullWrite __nullWriter = new NullWrite();

    public static IO instance() {
        return Singleton.__instance;
    }

    /**
     * Copy Stream in to Stream out until EOF or exception. in own thread
     */
    public static void copyThread(InputStream in, OutputStream out) {
        try {
            instance().run(new Job(in, out));
        } catch (InterruptedException e) {
            log.warn(LogSupport.EXCEPTION, e);
        }
    }

    /**
     * Copy Stream in to Stream out until EOF or exception.
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        copy(in, out, -1);
    }

    /**
     * Copy Stream in to Stream out until EOF or exception in own thread
     */
    public static void copyThread(Reader in, Writer out) {
        try {
            instance().run(new Job(in, out));
        } catch (InterruptedException e) {
            log.warn(LogSupport.EXCEPTION, e);
        }
    }

    /**
     * Copy Reader to Writer out until EOF or exception.
     */
    public static void copy(Reader in, Writer out) throws IOException {
        copy(in, out, -1);
    }

    /**
     * Copy Stream in to Stream for byteCount bytes or until EOF or exception.
     */
    public static void copy(InputStream in, OutputStream out, long byteCount) throws IOException {
        byte[] buffer = new byte[bufferSize];
        int len = bufferSize;

        if (byteCount >= 0) {
            while (byteCount > 0) {
                if (byteCount < bufferSize) {
                    len = in.read(buffer, 0, (int) byteCount);
                } else {
                    len = in.read(buffer, 0, bufferSize);
                }

                if (len == -1) {
                    break;
                }

                byteCount -= len;
                out.write(buffer, 0, len);
            }
        } else {
            while (true) {
                len = in.read(buffer, 0, bufferSize);
                if (len < 0) {
                    break;
                }
                out.write(buffer, 0, len);
            }
        }
    }
    
    /**
     * Copy Reader to Writer for byteCount bytes or until EOF or exception.
     */
    public static void copy(Reader in, Writer out, long byteCount) throws IOException {
        char[] buffer = new char[bufferSize];
        int len = bufferSize;

        if (byteCount >= 0) {
            while (byteCount > 0) {
                if (byteCount < bufferSize) {
                    len = in.read(buffer, 0, (int) byteCount);
                } else {
                    len = in.read(buffer, 0, bufferSize);
                }

                if (len == -1) {
                    break;
                }

                byteCount -= len;
                out.write(buffer, 0, len);
            }
        } else {
            while (true) {
                len = in.read(buffer, 0, bufferSize);
                if (len == -1) {
                    break;
                }
                out.write(buffer, 0, len);
            }
        }
    }

    /**
     * Read input stream to string.
     */
    public static String toString(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return new String(out.toByteArray());
    }

    /**
     * Delete File. This delete will recursively delete directories - BE CAREFULL.
     *
     * @param file The file to be deleted.
     */
    public static boolean delete(File file) {
        if (!file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; files != null && i < files.length; i++) {
                delete(files[i]);
            }
        }
        return file.delete();
    }

    /**
     * @return An outputstream to nowhere.
     */
    public static OutputStream getNullStream() {
        return __nullStream;
    }

    /**
     * closes an input stream, and (do not) logs exceptions.
     *
     * @param is the input stream to close
     */
    public static void close(InputStream is) {
        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            //
        }
    }

    /**
     * closes an output stream, and (do not) logs exceptions.
     *
     * @param os the output stream to close
     */
    public static void close(OutputStream os) {
        try {
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            //
        }
    }

    /**
     * @return An writer to nowhere.
     */
    public static Writer getNullWriter() {
        return __nullWriter;
    }

    /**
     * Run copy for copyThread()
     */
    public void handle(Object o) {
        Job job = (Job) o;
        try {
            if (job.in != null) {
                copy(job.in, job.out, -1);
            } else {
                copy(job.read, job.write, -1);
            }
        } catch (IOException e) {
            try {
                if (job.out != null) {
                    job.out.close();
                }
                if (job.write != null) {
                    job.write.close();
                }
            } catch (IOException e2) {
                //
            }
        }
    }

    private static class Singleton {
        static final IO __instance = new IO();

        static {
            try {
                __instance.start();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                System.exit(1);
            }
        }
    }

    static class Job {
        InputStream in;
        OutputStream out;
        Reader read;
        Writer write;

        Job(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
            this.read = null;
            this.write = null;
        }

        Job(Reader read, Writer write) {
            this.in = null;
            this.out = null;
            this.read = read;
            this.write = write;
        }
    }

    private static class NullOS extends OutputStream {
        public void close() {
        }

        public void flush() {
        }

        public void write(byte[] b) {
        }

        public void write(byte[] b, int i, int l) {
        }

        public void write(int b) {
        }
    }

    private static class NullWrite extends Writer {
        public void close() {
        }

        public void flush() {
        }

        public void write(char[] b) {
        }

        public void write(char[] b, int o, int l) {
        }

        public void write(int b) {
        }

        public void write(String s) {
        }

        public void write(String s, int o, int l) {
        }
    }
}
