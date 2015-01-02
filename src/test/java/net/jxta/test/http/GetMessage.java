/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
 *
 *  The Sun Project JXTA(TM) Software License
 *
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice, 
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *
 *  3. The end-user documentation included with the redistribution, if any, must 
 *     include the following acknowledgment: "This product includes software 
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must 
 *     not be used to endorse or promote products derived from this software 
 *     without prior written permission. For written permission, please contact 
 *     Project JXTA at http://www.jxta.org.
 *
 *  5. Products derived from this software may not be called "JXTA", nor may 
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN 
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United 
 *  States and other countries.
 *
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of 
 *  the license in source files.
 *
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */

package net.jxta.test.http;

import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @version $Id: GetMessage.java,v 1.3 2007/01/26 02:14:00 bondolo Exp $
 *
 * @author james todd [gonzo at jxta dot org]
 */

public class GetMessage
        implements Dispatchable {

    protected static boolean VERBOSE = false;

    protected String method = null;
    protected Message message = null;
    protected URLConnection connection = null;

    private URL url = null;
    private boolean isUnicodeEncoding = false;

    public GetMessage() {
        this(null, null);
    }

    public GetMessage(URL url) {
        this(url, null);
    }

    public GetMessage(URL url, Message message) {
        this.url = url;
        this.method = Constants.HTTP.GET;
        this.message = message;
    }

    public URL getURL() {
        return this.url;
    }

    public void setURL(URL url) {
        this.url = url;
    }

    public String getHeader(String key) {
        return ((this.message != null) ? this.message.getHeader(key) : null);
    }

    public Iterator<String> getHeaderKeys() {
        return ((this.message != null) ? this.message.getHeaderKeys() : Collections.EMPTY_LIST.iterator());
    }

    public void setHeaders(Map<String, Object> headers) {
        Iterator<String> keys = headers.keySet().iterator();

        while (keys.hasNext()) {
            String key = (String) keys.next();
            String value = (String) headers.get(key);

            setHeader(key, value);
        }
    }

    public void setHeader(String key, String value) {
        if (this.message == null) {
            this.message = new Message();
        }

        this.message.setHeader(key, value);
    }

    public void removeHeader(String key) throws NullPointerException {
        if (key == null) {
            throw new NullPointerException("null key");
        }

        if (this.message != null) {
            this.message.removeHeader(key);
        }
    }

    public boolean isUnicodeEncoding() {
        return this.isUnicodeEncoding;
    }

    public void setUnicodeEncoding(boolean isUnicodeEncoding) {
        this.isUnicodeEncoding = isUnicodeEncoding;
    }

    public Message dispatch() throws IOException {
        return dispatch(getURL());
    }

    public void close() {
        closeConnection();
    }

    @Override
    public String toString() {
        java.lang.Class<?> clazz = getClass();
        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
        java.util.HashMap<String, Object> map = new java.util.HashMap<String, Object>();
        java.lang.String object = null;
        java.lang.Object value = null;

        for (int i = 0; i < fields.length; i++) {
            try {
                object = fields[i].getName();
                value = fields[i].get(this);

                map.put(object, (value != null) ? value : "null");
            } catch (IllegalAccessException iae) {
                iae.printStackTrace();
            }
        }

        if (clazz.getSuperclass().getSuperclass() != null) {
            map.put("super", clazz.getSuperclass().toString());
        }

        return clazz.getName() + map;
    }

    protected void setMessage(Message message) {
        this.message = message;
    }

    protected String getBody() {
        return ((this.message != null && this.message.hasBody()) ? this.message.getBody() : null);
    }

    protected void setBody(String body) {
        if (this.message == null) {
            this.message = new Message();
        }

        this.message.setBody((body != null) ? body : "");
    }

    protected Message dispatch(URL u) throws IOException {
        Message response = null;
        URL to = u;
        URL from = null;

        while (to != null) {
            try {
                this.connection = openConnection(to);

                try {
                    doGet();
                } catch (IOException ioe) {
                    throw new IOException(ioe.getMessage());
                }

                response = getResponse(u);

                closeConnection();
            } catch (NullPointerException npe) {
                if (VERBOSE) {
                    npe.printStackTrace();
                }
            }

            from = to;
            to = getLocation(from, response);

            if (to != null) {
                this.message = response;
            }
        }

        response = resolveFrames(response, from);

        return response;
    }

    protected URLConnection openConnection(URL u) throws IllegalStateException, IOException {
        URLConnection connection = null;

        if (u == null) {
            throw new IllegalStateException("null url");
        }

        if (u.getProtocol().equalsIgnoreCase(Constants.Protocol.HTTP)
                || u.getProtocol().equalsIgnoreCase(Constants.Protocol.HTTPS)) {
            connection = openHTTPConnection(u);
        } else {
            connection = openFileConnection(u);
        }

        this.connection = connection;

        return connection;
    }

    protected void closeConnection() {
        if (this.connection instanceof HttpURLConnection) {
            ((HttpURLConnection) this.connection).disconnect();
        }

        this.connection = null;
    }

    protected void doGet() throws IOException {}

    protected Message getResponse(URL u) throws IOException {
        Message m = new Message();

        m.setHeaders(getResponseHeaders());

        String cl = m.getHeader(Constants.MIME.Key.CONTENT_LENGTH);
        int contentLength = -1;

        try {
            contentLength = Integer.valueOf(cl != null ? cl : "-1").intValue();
        } catch (NumberFormatException nfe) {}

        String contentType = m.getHeader(Constants.MIME.Key.CONTENT_TYPE);

        m.setBody((contentLength == -1 && contentType != null) || contentLength > 0 ? getResponseBody() : "");
        m.setReferer(u);

        return m;
    }

    protected URL getLocation(URL base, Message message) {
        URL u = null;
        final String LOCATION_PREFIX = ".location='";
        final String LOCATION_SUFFIX = "'";

        if (base != null && message != null) {
            String location = message.getHeader(Constants.MIME.Key.LOCATION);

            if (location != null) {
                try {
                    u = new URL(base, location);
                } catch (MalformedURLException mue) {
                    if (VERBOSE) {
                        mue.printStackTrace();
                    }
                }
            } else if (message.getReferer() != null) {
                String s = message.getBody();
                int i = s.indexOf(LOCATION_PREFIX);
                int j = s.indexOf(LOCATION_SUFFIX, i + LOCATION_PREFIX.length());

                if (i > -1 && j > i) {
                    try {
                        u = new URL(message.getReferer(), s.substring(i + LOCATION_PREFIX.length(), j));
                    } catch (MalformedURLException mue) {
                        if (VERBOSE) {
                            mue.printStackTrace();
                        }
                    }
                }
            }
        }

        return u;
    }

    // xxx: ugly

    protected Message resolveFrames(Message response, URL base) {
        final String prefix = "<frame ";
        final String prefixCap = prefix.toUpperCase();
        final String postfix = ">";
        final String target = "src";
        final String targetCap = target.toUpperCase();
        final String quote = "\"";
        Message msg = new Message();
        int i = -1;
        int j = -1;
        int k = -1;
        int l = -1;
        int m = -1;
        String s = null;
        String r = null;
        URL u = null;
        Message reply = null;
        String key = null;
        List<String> values = null;
        StringBuilder sb = new StringBuilder();

        if (response != null && response.getBody() != null) {
            sb.append(response.getBody());
        }

        for (Iterator<String> keys = (response != null ? response.getHeaderKeys() : Collections.EMPTY_MAP.keySet().iterator()); keys.hasNext();) {
            key = (String) keys.next();
            values = new ArrayList<String>();

            for (Iterator<String> h = response.getHeaders(key); h.hasNext();) {
                values.add((String) h.next());
            }

            msg.setHeader(key, (String) values.get(0));
        }

        while ((i = sb.indexOf(prefix)) > -1 || (j = sb.indexOf(prefixCap)) > -1) {
            k = (i > j ? i : j);
            i = j = -1;
            i = sb.indexOf(target, k);
            j = sb.indexOf(targetCap, k);
            l = (i > j ? i : j);
            m = sb.indexOf(postfix, k);

            if (k > -1 && l > k && m > l) {
                s = sb.substring(l, m);
                i = s.indexOf(quote);
                j = s.indexOf(quote, i + 1);
                r = s.substring(i + 1, j).trim();

                try {
                    u = new URL(base, r);
                } catch (MalformedURLException mue) {
                    if (VERBOSE) {
                        mue.printStackTrace();
                    }
                }

                try {
                    reply = new GetMessage(u).dispatch();
                } catch (IOException ioe) {
                    if (VERBOSE) {
                        ioe.printStackTrace();
                    }
                }

                sb.replace(k, m + 1, reply.getBody());
            } else {
                break;
            }

            i = j = k = l = m = -1;
        }

        msg.setBody(sb.toString());

        return msg;
    }

    private HttpURLConnection openHTTPConnection(URL u) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) u.openConnection();

        connection.setRequestMethod(this.method);

        String key = null;
        String value = null;

        if (this.message != null) {
            for (Iterator<String> keys = this.message.getHeaderKeys(); keys.hasNext();) {
                key = (String) keys.next();
                value = this.message.getHeader(key);
                connection.setRequestProperty(key, value);
            }
        }

        boolean doOutput = (this.method == Constants.HTTP.POST && this.message != null && this.message.hasBody());
        Map<String, Object> defaultHeaders = ((!doOutput) ? Util.getDefaultGetHeaders() : Util.getDefaultPostHeaders());

        for (Iterator<String> keys = defaultHeaders.keySet().iterator(); keys.hasNext();) {
            key = (String) keys.next();

            if (connection.getRequestProperty(key) == null) {
                value = (String) defaultHeaders.get(key);

                connection.addRequestProperty(key, value);
            }
        }

        if (doOutput) {
            int l = this.message.getBody().getBytes().length;

            connection.setRequestProperty(Constants.MIME.Key.CONTENT_LENGTH, Integer.toString(l));
        }

        boolean followRedirects = (this.method == Constants.HTTP.GET);

        connection.setDoOutput(doOutput);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        HttpURLConnection.setFollowRedirects(followRedirects);
        connection.setInstanceFollowRedirects(followRedirects);

        return connection;
    }

    private URLConnection openFileConnection(URL u) throws IOException {
        return u.openConnection();
    }

    private Map<String, Object> getResponseHeaders() {
        Map<String, Object> headers = new HashMap<String, Object>();
        Map<String, List<String>> m = this.connection.getHeaderFields();

        headers.putAll(m);
        headers.remove(null);

        return headers;
    }

    private String getResponseBody() throws IOException {
        final int BLOCK = 4 * 1024;
        StringBuilder response = new StringBuilder();
        InputStream is = this.connection.getInputStream();
        String contentType = this.connection.getContentType();
        InputStreamReader isr = ((this.isUnicodeEncoding)
                ? new InputStreamReader(is, Util.getCharSet(contentType))
                : new InputStreamReader(is));
        BufferedReader reader = new BufferedReader(isr);
        char[] buf = new char[BLOCK];
        int l = 0;
        CharArrayWriter writer = new CharArrayWriter();

        while ((l = reader.read(buf, 0, BLOCK)) > -1) {
            writer.write(buf, 0, l);
        }

        reader.close();

        return(this.isUnicodeEncoding ? Util.toUnicodeEncoded(writer.toString()) : writer.toString());
    }
}
