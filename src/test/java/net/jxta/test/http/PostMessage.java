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
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @version $Id: PostMessage.java,v 1.2 2007/01/26 02:14:00 bondolo Exp $
 *
 * @author james todd [gonzo at jxta dot org]
 */

public class PostMessage extends GetMessage {

    public PostMessage() {
        this(null, "");
    }

    public PostMessage(URL url) {
        this(url, "");
    }

    public PostMessage(URL url, String message) {
        this(url, ((message != null) ? new Message(message) : new Message()));
    }

    public PostMessage(URL url, Message message) {
        super(url, message);
        super.method = Constants.HTTP.POST;

        if (getHeader(Constants.MIME.Key.CONTENT_TYPE) == null) {
            setContentType(Constants.MIME.Value.XML);
        }
    }

    public void setContentType(String contentType) {
        setHeader(Constants.MIME.Key.CONTENT_TYPE, ((contentType != null) ? contentType : Constants.MIME.Value.XML));
    }

    public String getContentType() {
        return getHeader(Constants.MIME.Key.CONTENT_TYPE);
    }

    @Override
    public void setBody(String body) {
        super.setBody(body);
    }

    @Override
    public String getBody() {
        return super.getBody();
    }

    @Override
    public Message dispatch() throws IOException {
        Message response = null;
        URL to = getURL();

        try {
            this.connection = openConnection(to);

            try {
                doGet();
                doPost();
            } catch (IOException ioe) {
                throw new IOException(ioe.getMessage());
            }

            response = getResponse(to);

            closeConnection();
        } catch (NullPointerException npe) {
            if (VERBOSE) {
                npe.printStackTrace();
            }
        }

        // xxx : what about redirecting a post-to-post ... vs post-to-get

        URL from = to;

        to = getLocation(from, response);

        if (to != null) {
            from = to;

            List<String> cookies = new ArrayList<String>();

            for (Iterator<String> h = response.getHeaders(Constants.MIME.Key.COOKIE); h.hasNext();) {
                cookies.add((String) h.next());
            }

            for (Iterator<String> h = response.getHeaders(Constants.MIME.Key.SET_COOKIE); h.hasNext();) {
                cookies.add((String) h.next());
            }

            response.removeHeaders();

            for (Iterator<String> c = cookies.iterator(); c.hasNext();) {
                response.setHeader(Constants.MIME.Key.COOKIE, (String) c.next());
            }

            setMessage(response);
            setURL(to);

            try {
                response = dispatch(getURL());
            } catch (IOException ioe) {
                if (VERBOSE) {
                    ioe.printStackTrace();
                }
            }
        }

        response = resolveFrames(response, from);

        return response;
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

    protected void doPost() throws IOException {
        final int BLOCK = 4 * 1024;

        if (super.method == Constants.HTTP.POST && super.message != null && super.message.hasBody()) {
            char[] message = super.message.getBody().toCharArray();
            OutputStreamWriter osw = new OutputStreamWriter(this.connection.getOutputStream());
            BufferedWriter writer = new BufferedWriter(osw);
            int l = BLOCK;
            int c = 0;
            int m = message.length;

            while ((c * BLOCK) < m) {
                if (((c + 1) * BLOCK) > m) {
                    l = m - c * BLOCK;
                }

                writer.write(message, c++ * BLOCK, l);
            }

            writer.flush();
            writer.close();
        }
    }
}
