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

package net.jxta.ext.config;


import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;


/**
 * @version $Id: UtilTest.java,v 1.6 2006/06/08 04:31:12 gonzo Exp $
 *
 * @author james todd [gonzo at jxta dot org]
 */

public class UtilTest extends TestBase {

    private static final InetAddress ALL_ADDRESSES = Env.ALL_ADDRESSES;
    private static final String PROTOCOL = Protocol.TCP_URI;
    private static final int PORT = 9701;
    private static final int PORT_RANGE = 100;
    private static final String COLON = ":";
    private static final String WINDOWS_URI = "file:/c:/a%20b/c";

    public static void main(String[] argv) {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.TestSuite suite() {
        junit.framework.TestSuite suite = new junit.framework.TestSuite();

        suite.addTest(new junit.framework.TestSuite(UtilTest.class));

        return suite;
    }

    public UtilTest() {
        this(UtilTest.class.getName());
    }

    public UtilTest(String name) {
        super(name);
    }

    public void testNormalize() {
        int port = -1;

        for (int i = PORT; i <= PORT + PORT_RANGE && port == -1; i++) {
            if (Util.isPortAvailable(ALL_ADDRESSES, i)) {
                port = i;
            }
        }

        if (port != -1) {
            String b = PROTOCOL + ALL_ADDRESSES.getHostAddress() + COLON + port;
            URI bu = null;

            try {
                bu = new URI(b);
            } catch (URISyntaxException use) {
                fail("bad uri: " + b + " " + use.getMessage());
            }

            if (bu != null) {
                Address a = new Address();
                
                a.setAddress(bu);
                a.setPortRange(PORT_RANGE - port);
                
                URI u = Util.normalize(a);
                
                assertNotNull(u);
                assertEquals(bu.getPort(), u.getPort());
            }
        } else {
            fail("unable to find open port");
        }
    }
    
    public void testNormalizeWindowsURI() {
        assertNotNull(Util.normalizeURI(WINDOWS_URI));
    }
}
