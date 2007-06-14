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


import net.jxta.peergroup.PeerGroup;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.StructuredDocument;
import net.jxta.document.TextElement;
import net.jxta.protocol.TransportAdvertisement;
import net.jxta.exception.ConfiguratorException;
import net.jxta.impl.protocol.PlatformConfig;
import net.jxta.impl.protocol.TCPAdv;
import net.jxta.ext.config.TcpTransport;
import net.jxta.ext.config.TcpTransportAddress;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;


/**
 *
 * @version $Id: TransportTest.java,v 1.7 2005/05/26 22:04:47 gonzo Exp $
 *
 * @author james todd [gonzo at jxta dot org]
 */

public class TransportTest extends TestBase {

    public static void main(String[] argv) {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.TestSuite suite() {
        junit.framework.TestSuite suite = new junit.framework.TestSuite();

        suite.addTest(new junit.framework.TestSuite(TransportTest.class));

        return suite;
    }

    public TransportTest() {
        this(TransportTest.class.getName());
    }

    public TransportTest(String name) {
        super(name);
    }

    public void testTcp() {
        Configurator c = new Configurator();

        c.setName(NAME);
        c.setSecurity(PRINCIPAL, PASSWORD);

        // c.clearRendezVous();
        // c.clearRelays();
        // c.setRelayOutgoing(false);

        TcpTransportAddress ta = new TcpTransportAddress();

        try {
            URI u = new URI("tcp://" + TCP_ADDRESS + ":" + TCP_PORT);

            ta.setAddress(u);
        } catch (URISyntaxException use) {
            fail("TcpTransportAddress.setAddress() exception: " + use.getMessage());
        }

        MulticastAddress ma = new MulticastAddress();

        ma.setMulticast(true);
        ma.setSize(MULTICAST_SIZE);

        try {
            ma.setAddress(new URI("udp://" + MULTICAST_ADDRESS + ":" + MULTICAST_PORT));
        } catch (URISyntaxException use) {
            fail("MulticastAddress.setAddress() exception: " + use.getMessage());
        } catch (IllegalArgumentException iae) {
            fail("MulitcastAddress.setAddress() exception: " + iae.getMessage());
        }

        ta.setMulticastAddress(ma);

        TcpTransport tt = new TcpTransport();

        tt.setAddress(ta);
        c.setTransport(tt);

        PlatformConfig pc = null;

        try {
            pc = c.getPlatformConfig();
        } catch (ConfiguratorException ce) {
            ce.printStackTrace();
            fail("Configurator.getPlatformConfig() exception: " + ce.getMessage());
        }

        assertNotNull(pc);

        StructuredDocument sd = pc.getServiceParam(PeerGroup.tcpProtoClassID);

        assertNotNull(sd);

        if (sd != null) {
            TCPAdv tcp = null;

            for (Enumeration e = sd.getChildren(TransportAdvertisement.getAdvertisementType()); e != null && e.hasMoreElements();) {
                tcp = (TCPAdv) AdvertisementFactory.newAdvertisement((TextElement) e.nextElement());

                assertTrue(tcp != null);

                if (tcp != null) {
                    assertEquals(TCP_ADDRESS, tcp.getInterfaceAddress());
                    assertEquals(TCP_PORT, tcp.getPort());
                }
            }
        }
    }
}
