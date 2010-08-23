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


/**
 *
 * @version $Id: Constants.java,v 1.1 2003/08/01 17:41:01 gonzo Exp $
 *
 * @author james todd [gonzo at jxta dot org]
 */

public class Constants {
    public static class Package {
        public static final String NAME = Constants.class.getPackage().getName();
    }


    public static class HTTP {
        public static final String GET = "GET";
        public static final String POST = "POST";
        public static final String HEAD = "HEAD";
        public static final String OPTIONS = "OPTIONS";
        public static final String PUT = "PUT";
        public static final String DELETE = "DELETE";
        public static final String TRACE = "TRACE";

        // xxx
        // required as java.net.HttpURLConnection.getHeaderFieldKey(int)
        // appears to prematurely return a null - bummer

        public static final int MAX_HEADERS = 25;
    }


    public static class Protocol {
        public static final String HTTP = "http";
        public static final String HTTPS = "https";
    }


    public static class MIME {
        public static class Key {
            public static final String ACCEPT = "Accept";
            public static final String ACCEPT_CHARSET = "Accept-Charset";
            public static final String CONNECTION = "Connection";
            public static final String CONTENT_LENGTH = "Content-Length";
            public static final String CONTENT_TYPE = "Content-Type";
            public static final String COOKIE = "Cookie";
            public static final String SET_COOKIE = "Set-Cookie";
            public static final String USER_AGENT = "User-Agent";
            public static final String LOCATION = "Location";
            public static final String AUTHORIZATION = "Authorization";
        }


        public static class Value {
            public static final String ACCEPT_ALL = "*/*";
            public static final String CHARSET = "iso-8859-1,*,utf-8";
            public static final String TROLL = "Troll/1.0 (! KeepItDark) Troll/1.0";
            // public static final String TROLL =
            // "Mozilla/5.0 (Windows; N; WinNT4.0; en-US; m14) Netscape6/6.0b1";
            public static final String KEEP_ALIVE = "Keep-Alive";
            public static final String PLAIN = "text/plain";
            public static final String URL_FORM_ENCODED = "application/x-www-form-urlencoded";
            public static final String XML = "text/xml";
        }
    }


    public static class Session {
        public static final String ID = "JSESSIONID";
        public static final String NS_ID = "NSES40Session";
        public static final String PREFIX = "=";
        public static final String POSTFIX = ";";
    }
}
