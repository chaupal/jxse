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

package net.jxta.impl.endpoint.servlethttp;

import net.jxta.test.http.GetMessage;
import net.jxta.test.http.PostMessage;
import net.jxta.test.http.Message;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 *
 * @version $Id: HttpMessageReceiverTest.java,v 1.11 2007/01/26 02:13:56 bondolo Exp $
 *
 * @author james todd [gonzo at jxta dot org]
 */

@Ignore("Needs some work, Investigate")
public class HttpMessageReceiverTest {

    private static final String RESOURCE_PREFIX = "/net/jxta/test/resources/";
    private static final String RESOURCE_SUFFIX = ".properties";
    private static final String RELAY_RESOURCE = "relay";
    private static final String PEER_ID_RESOURCE = "peerId";
    private static final String ONE_K_MESSAGE_RESOURCE = "1kMessage";
    private static final String CONTENT_TYPE_RESOURCE = "contentTyep";
    private static final String HELLO_PREFIX = "uuid-";
    private static final char DOT = '.';
    private static final String MS_LABEL = " ms";
    private static final int MS_PER_SEC = 1000;

    private URL relay = null;
    private String peerId = null;
    private String oneKMessage = null;
    private String contentType = null;
    private String sixtyFourKMessage = null;
    private URL relayGet = null;
    private URL relayPost = null;

    @Test
    public void testHello() {
        assertNotNull("relay is null", this.relay);

        String r = null;
        boolean isBogus = false;

        if (this.relay != null) {
            GetMessage gm = new GetMessage(this.relay);
            Message response = null;
            long then = getTime();

            try {
                response = gm.dispatch();
            } catch (IOException ioe) {
                isBogus = true;
            }

            long delta = getTime() - then;

            r = response != null ? response.getBody() : "";

            System.err.println("hello time: " + delta + MS_LABEL);
        }

        assertFalse("bogus", isBogus);
        assertFalse("empty response", r == null || r.trim().length() == 0);
        assertTrue("missing prefix" + HELLO_PREFIX, r != null && r.startsWith(HELLO_PREFIX));
    }

    @Test
    public void testPutOneKMessage() {
        assertNotNull("relay is null", this.relayPost);
        assertNotNull("1k is null", this.oneKMessage);

        String r = null;
        boolean isBogus = false;

        if (this.relayPost != null) {
            PostMessage pm = new PostMessage(this.relayPost, new Message(this.oneKMessage));
            Message response = null;
            boolean exceptionThrown = false;

            pm.setContentType(this.contentType);

            long then = getTime();

            try {
                response = pm.dispatch();
            } catch (IOException ioe) {
                isBogus = true;
            }

            long delta = getTime() - then;

            r = response != null ? response.getBody() : "";

            System.err.println("put 1k time: " + delta + MS_LABEL);
        }

        assertFalse("bogus", isBogus);
        assertTrue("empty response", r == null || r.trim().length() == 0);
    }

    @Test
    public void testGetOneKMessage() {
        assertNotNull("relay is null", this.relayGet);

        String r = null;
        boolean isBogus = false;

        /*
         if (this.relayGet != null) {
         System.out.println(this.relayGet);
         GetMessage gm = new GetMessage(this.relayGet);
         Message response = null;
         long then = getTime();

         try {
         response = gm.dispatch();
         } catch (IOException ioe) {
         isBogus = true;
         }

         long delta = getTime() - then;

         r = response != null ? response.getBody() : "";

         System.err.println("hello time: " + delta + MS_LABEL);
         }

         assertFalse("bogus", isBogus);
         assertFalse("empty response", r == null || r.trim().length() == 0);
         assertTrue("missing prefix" + HELLO_PREFIX,
         r != null && r.startsWith(HELLO_PREFIX));
         */
    }

    @Test
    public void testPutSixtyFourKMessage() {
        assertNotNull("64k is null", this.sixtyFourKMessage);
    }

    @Test
    public void testGetSixtyFourKMessage() {
        assertNotNull("64k is null", this.sixtyFourKMessage);
    }

    @Before
    protected void setUp() {
        Properties props = new Properties();
        String base = this.getClass().getName();
        String resource = RESOURCE_PREFIX + base.substring(base.lastIndexOf(DOT) + 1) + RESOURCE_SUFFIX;

        try {
            props.load(getResource(resource));
        } catch (IOException ioe) {}

        try {
            this.relay = new URL(props.getProperty(RELAY_RESOURCE));
        } catch (MalformedURLException mue) {}

        this.peerId = props.getProperty(PEER_ID_RESOURCE);
        this.oneKMessage = getData(getResource(props.getProperty(ONE_K_MESSAGE_RESOURCE)));
        this.contentType = getData(getResource(props.getProperty(CONTENT_TYPE_RESOURCE)));

        if (this.oneKMessage != null) {
            StringBuilder sb = new StringBuilder(64 * this.oneKMessage.length());

            for (int i = 0; i < 64; i++) {
                sb.append(this.oneKMessage);
            }
            this.sixtyFourKMessage = sb.toString();
        }

        if (this.relay != null) {
            try {
                this.relayGet = new URL(this.relay.toString() + "/" + this.peerId + "?120000,120000," + this.relay.toString());
            } catch (MalformedURLException mue) {}

            this.relayPost = this.relay;
        }
    }

    @Before
    protected void tearDown() {
        System.gc();
    }

    private InputStream getResource(String resource) {
        return resource != null ? this.getClass().getResourceAsStream(resource) : null;
    }

    private String getData(InputStream is) {
        String data = null;

        if (is != null) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int c = -1;

            try {
                while ((c = is.read()) != -1) {
                    os.write(c);
                }
            } catch (IOException ioe) {} finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ioe) {}
                }

                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException ioe) {}
                }
            }

            data = os.toString();
        }

        return data;
    }

    private long getTime() {
        return System.currentTimeMillis();
    }
}
