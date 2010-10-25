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


import java.util.Collections;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.net.URL;
import java.net.URLDecoder;


/**
 *
 * @version $Id: Message.java,v 1.2 2007/01/26 02:14:00 bondolo Exp $
 *
 * @author james todd [gonzo at jxta dot org]
 */

public class Message {

    private static List sessionIds = null;
    private Map headers = null;
    private String body = null;
    private URL referer = null;

    static {
        sessionIds = new ArrayList();

        sessionIds.add(Constants.Session.ID);
        sessionIds.add(Constants.Session.NS_ID);
    }

    public Message() {
        this(null, null);
    }

    public Message(Map headers) {
        this(headers, null);
    }

    public Message(String body) {
        this(null, body);
    }

    public Message(Map headers, String body) {
        this.headers = headers;
        this.body = body;
    }

    public String getHeader(String key) {
        Iterator i = getHeaders(key);

        return (i.hasNext() ? (String) i.next() : null);
    }

    public Iterator getHeaders(String key) {
        List values = new ArrayList();
        String k = null;
        Object o = null;

        for (Iterator i = getHeaderKeys(); i != null && i.hasNext();) {
            k = (String) i.next();

            if (k.equalsIgnoreCase(key)) {
                o = this.headers.get(k);

                if (o instanceof String) {
                    values.add((String) o);
                } else if (o instanceof Collection) {
                    values.addAll((Collection) o);
                }
            }
        }

        return values.iterator();
    }

    public Iterator getHeaderKeys() {
        return ((this.headers != null) ? this.headers.keySet().iterator() : Collections.EMPTY_LIST.iterator());
    }

    public void setHeader(String key, String value) {
        setHeader(key, value);
    }

    public void setHeader(String key, Object value) {
        if (this.headers == null) {
            this.headers = new HashMap();
        }

        this.headers.put(key, value);
    }

    public void setHeaders(Map headers) {
        String key = null;

        for (Iterator i = headers.keySet().iterator(); i.hasNext();) {
            key = (String) i.next();

            setHeader(key, headers.get(key));
        }
    }

    public void removeHeader(String key) {
        if (this.headers != null) {
            this.headers.remove(key);

            if (this.headers.size() == 0) {
                this.headers = null;
            }
        }
    }

    public void removeHeaders() {
        if (this.headers != null) {
            this.headers.clear();
            this.headers = null;
        }
    }

    public String getSessionId() {
        String s = getHeader(Constants.MIME.Key.COOKIE);

        return (s != null ? parseSessionIdFromHeader(s) : parseSessionIdFromBody(getBody()));
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean hasBody() {
        return (this.body != null && this.body.trim().length() > 0);
    }

    public void setReferer(URL referer) {
        this.referer = referer;
    }

    public URL getReferer() {
        return this.referer;
    }

    @Override
    public String toString() {
        java.lang.Class clazz = getClass();
        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
        java.util.Map map = new java.util.HashMap();
        java.lang.String object = null;
        java.lang.Object value = null;

        for (int i = 0; i < fields.length; i++) {
            try {
                object = fields[i].getName();
                value = fields[i].get(this);

                if (value == null) {
                    value = "null";
                }

                map.put(object, value);
            } catch (IllegalAccessException iae) {
                iae.printStackTrace();
            }
        }

        if (clazz.getSuperclass().getSuperclass() != null) {
            map.put("super", clazz.getSuperclass().toString());
        }

        return clazz.getName() + map;
    }

    private String parseSessionIdFromHeader(String value) {
        final String EQUAL = "=";

        String id = null;
        String prefix = null;

        if (value != null) {
            for (Iterator i = sessionIds.iterator(); i.hasNext();) {
                id = (String) i.next();

                if (value.indexOf(id) > -1) {
                    prefix = id;
                    value = trim(value, id + Constants.Session.PREFIX, Constants.Session.POSTFIX);

                    break;
                }
            }
        }

        return (value != null ? prefix + EQUAL + value : value);
    }

    private String parseSessionIdFromBody(String value) {
        final String SEMI_COLON = ";";
        final String QUOTE = "\"";
        final String QUESTION_MARK = "?";
        final String SINGLE_QUOTE = "'";
        final String GREATER_THAN = ">";
        final String SESSION_ID = SEMI_COLON + Constants.Session.ID.toLowerCase();

        if (value != null && value.indexOf(SESSION_ID) > -1) {
            value = trim(value, SESSION_ID, QUOTE);
            value = trim(value, null, QUESTION_MARK);
            value = trim(value, null, SINGLE_QUOTE);
            value = trim(value, null, GREATER_THAN);
        } else {
            value = null;
        }

        return value;
    }

    private String trim(String value, String prefix, String postfix) {
        StringBuffer sb = null;

        if (value != null && value.length() > 0) {
            sb = new StringBuffer(value);

            int j = (prefix != null ? sb.indexOf(prefix) + prefix.length() : 0);
            int k = (postfix != null ? sb.indexOf(postfix, j + 1) : sb.length() - 1);

            sb = new StringBuffer(sb.substring(j, (k >= 0 ? k : sb.length())));
        }

        return (sb != null ? sb.toString() : null);
    }
}
