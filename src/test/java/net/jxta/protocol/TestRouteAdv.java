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

package net.jxta.protocol;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.id.IDFactory;
import net.jxta.impl.protocol.RouteQuery;
import net.jxta.impl.protocol.RouteResponse;
import net.jxta.peer.PeerID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *  This is a simple test for Route and AccessPoint advertisement
 */

public class TestRouteAdv {

	@Rule
	public TemporaryFolder tempStorage = new TemporaryFolder();
	
	@Test
    public void testRouteAdv() {
        // create access point advertisment for destination
        System.out.println("Create an access point advertisement");
        AccessPointAdvertisement ap = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        Vector addresses = new Vector();

        addresses.add("TCP:123.123.123.123");
        addresses.add("TCP:134.134.134.134");
        ap.setEndpointAddresses(addresses);

        try {
            // let's print the advertisement as a plain text document
            StructuredTextDocument doc = (StructuredTextDocument)
                    ap.getDocument(MimeMediaType.XMLUTF8);

            System.out.println("AccessPointAdvertisement original : ");
            System.out.println(doc.toString());

            StringWriter out = new StringWriter();

            out.write(doc.toString());
            out.close();

            StringReader in = new StringReader(out.toString());
            XMLDocument advDocument = (XMLDocument) StructuredDocumentFactory.newStructuredDocument( MimeMediaType.XMLUTF8, in);
            AccessPointAdvertisement apAdv = (AccessPointAdvertisement)
                    AdvertisementFactory.newAdvertisement(advDocument);

            in.close();

            doc = (StructuredTextDocument)
                    apAdv.getDocument(MimeMediaType.XMLUTF8);
            System.out.println("AccessPointAdvertisement reconstructed : ");
            System.out.println(doc.toString());

            // verify advertisement
            assertEquals(ap.getPeerID(), apAdv.getPeerID());
            Enumeration e1 = apAdv.getEndpointAddresses();

            for (Enumeration e = Collections.enumeration(addresses); e.hasMoreElements();) {
                assertEquals(e.nextElement(), e1.nextElement());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Error constructing advertisement");
        }

        // create Route advertisment with a single destination
        System.out.println("Create Route with single destination ");
        RouteAdvertisement route = (RouteAdvertisement)
                AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());

        route.setDest(ap);
        try {
            // let's print the advertisement as a plain text document
            StructuredTextDocument doc = (StructuredTextDocument)
                    route.getDocument(MimeMediaType.XMLUTF8);

            System.out.println(doc.toString());

            StringWriter out = new StringWriter();

            out.write(doc.toString());
            out.close();

            StringReader in = new StringReader(out.toString());
            XMLDocument advDocument = (XMLDocument) StructuredDocumentFactory.newStructuredDocument( MimeMediaType.XMLUTF8, in);
            RouteAdvertisement routeAdv = (RouteAdvertisement)
                    AdvertisementFactory.newAdvertisement(advDocument);

            in.close();

            doc = (StructuredTextDocument)
                    routeAdv.getDocument(MimeMediaType.XMLUTF8);
            System.out.println("RouteAdvertisement reconstructed : ");
            System.out.println(doc.toString());

            // verify advertisement
            ap = route.getDest();
            AccessPointAdvertisement ap1 = routeAdv.getDest();

            assertEquals(ap.getPeerID(), ap1.getPeerID());
            Enumeration e1 = ap1.getEndpointAddresses();

            for (Enumeration e = ap.getEndpointAddresses(); e.hasMoreElements();) {
                assertEquals(e.nextElement(), e1.nextElement());
            }

            Enumeration r1 = routeAdv.getHops();

            for (Enumeration e = routeAdv.getHops(); e.hasMoreElements();) {
                ap = (AccessPointAdvertisement) e.nextElement();
                ap1 = (AccessPointAdvertisement) r1.nextElement();
                assertEquals(ap.getPeerID(), ap1.getPeerID());
                e1 = ap1.getEndpointAddresses();
                for (Enumeration e2 = ap.getEndpointAddresses(); e2.hasMoreElements();) {
                    assertEquals(e2.nextElement(), e1.nextElement());
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Error constructing advertisement");
        }

        // create access point advertisment for hops
        AccessPointAdvertisement ap1 = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap1.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        addresses = new Vector();
        addresses.add("TCP:222.222.222.222");
        addresses.add("TCP:244.244.244.244");
        ap1.setEndpointAddresses(addresses);

        AccessPointAdvertisement ap2 = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap2.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        addresses = new Vector();
        addresses.add("TCP:666.666.666.666");
        addresses.add("TCP:777.777.777.777");
        ap2.setEndpointAddresses(addresses);

        // create Route advertisment with a single destination
        System.out.println("Create Route with destination and one hope");
        RouteAdvertisement route1 = (RouteAdvertisement)
                AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());

        route1.setDest(ap);
        Vector hops = new Vector();

        hops.add(ap1);
        hops.add(ap2);
        route1.setHops(hops);
        try {
            // let's print the advertisement as a plain text document
            StructuredTextDocument doc = (StructuredTextDocument)
                    route1.getDocument(MimeMediaType.XMLUTF8);

            StringWriter out = new StringWriter();

            doc.sendToWriter(out);
            System.out.println(out.toString());
            File routeFile = tempStorage.newFile("route1.adv");
            FileOutputStream fp = new FileOutputStream(routeFile);

            fp.write(out.toString().getBytes());
            fp.close();
            out.close();

            FileInputStream is = new FileInputStream(routeFile);
            XMLDocument advDocument = (XMLDocument) StructuredDocumentFactory.newStructuredDocument( MimeMediaType.XMLUTF8, is);
            RouteAdvertisement routeAdv = (RouteAdvertisement)
                    AdvertisementFactory.newAdvertisement(advDocument);

            is.close();

            doc = (StructuredTextDocument)
                    routeAdv.getDocument(MimeMediaType.XMLUTF8);
            System.out.println("RouteAdvertisement reconstructed from file");
            out = new StringWriter();
            doc.sendToWriter(out);
            System.out.println(out.toString());
            out.close();

            // verify advertisement
            ap = route1.getDest();
            ap1 = routeAdv.getDest();
            assertEquals(ap.getPeerID(), ap1.getPeerID());
            Enumeration e1 = ap1.getEndpointAddresses();

            for (Enumeration e = ap.getEndpointAddresses(); e.hasMoreElements();) {
                assertEquals(e.nextElement(), e1.nextElement());
            }

            Enumeration r1 = routeAdv.getHops();

            for (Enumeration e = routeAdv.getHops(); e.hasMoreElements();) {
                ap = (AccessPointAdvertisement) e.nextElement();
                ap1 = (AccessPointAdvertisement) r1.nextElement();
                assertEquals(ap.getPeerID(), ap1.getPeerID());
                e1 = ap1.getEndpointAddresses();
                for (Enumeration e2 = ap.getEndpointAddresses(); e2.hasMoreElements();) {
                    assertEquals(e2.nextElement(), e1.nextElement());
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Error constructing advertisement");
        }

    }

	@Test
    public void testAddDestRoute() {
        // create access point advertisment for destination
        System.out.println("Test add and remove of endpoint addresses");
        AccessPointAdvertisement ap = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        Vector addresses = new Vector();

        addresses.add("TCP://123.123.123.123");
        addresses.add("TCP://134.134.134.134");
        ap.setEndpointAddresses(addresses);

        // create the route
        RouteAdvertisement route = (RouteAdvertisement)
                AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());

        route.setDest(ap);

        // add new addresses to the destination
        List<EndpointAddress> newaddresses = new ArrayList<EndpointAddress>();

        newaddresses.add(new EndpointAddress("TCP://222.123.123.123"));
        newaddresses.add(new EndpointAddress("TCP://222.134.134.134"));
        route.addDestEndpointAddresses(newaddresses);
        addresses.add("TCP://222.123.123.123");
        addresses.add("TCP://222.134.134.134");

        // verify advertisement
        Enumeration e1 = Collections.enumeration(addresses);

        for (Enumeration e = route.getDest().getEndpointAddresses(); e.hasMoreElements();) {
            assertEquals(e.nextElement().toString(), e1.nextElement().toString());
        }

        // test to remove access point
        route.removeDestEndpointAddresses(newaddresses);
        addresses.remove("TCP://222.123.123.123");
        addresses.remove("TCP://222.134.134.134");

        // verify advertisement
        e1 = Collections.enumeration(addresses);
        for (Enumeration e = route.getDest().getEndpointAddresses(); e.hasMoreElements();) {
            assertEquals(e.nextElement().toString(), e1.nextElement().toString());
        }
    }

	@Test
    public void testDestDisplay() {
        // create access point advertisment for destination
        System.out.println("Test debug route display");
        AccessPointAdvertisement ap = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        Vector addresses = new Vector();

        addresses.add("TCP:123.123.123.123");
        addresses.add("TCP:134.134.134.134");
        ap.setEndpointAddresses(addresses);

        // create the route
        RouteAdvertisement route = (RouteAdvertisement)
                AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());

        route.setDest(ap);

        AccessPointAdvertisement ap2 = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap2.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        addresses = new Vector();
        addresses.add("TCP:666.666.666.666");
        addresses.add("TCP:777.777.777.777");
        ap2.setEndpointAddresses(addresses);

        AccessPointAdvertisement ap4 = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap4.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        addresses = new Vector();
        addresses.add("TCP:888.888.888.888");
        addresses.add("TCP:999.999.999.999");
        ap4.setEndpointAddresses(addresses);

        Vector hops = new Vector();

        hops.add(ap2);
        hops.add(ap4);
        route.setHops(hops);
        route.setHops(hops);
        System.out.println(route.display());
    }

	@Test
    public void testHops() {
        System.out.println("Test hops");
        AccessPointAdvertisement ap = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());
        PeerID pid = IDFactory.newPeerID(IDFactory.newPeerGroupID());

        ap.setPeerID(pid);
        // create the route
        RouteAdvertisement route = (RouteAdvertisement)
                AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());

        route.setDest(ap);
        assertEquals(pid, route.getDestPeerID());
        AccessPointAdvertisement ap2 = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap2.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));

        AccessPointAdvertisement ap4 = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap4.setPeerID(ap.getPeerID());
        Vector hops = new Vector();

        hops.add(ap2);
        hops.add(ap4);
        route.setHops(hops);

        assertEquals(true, route.containsHop(ap.getPeerID()));

    }

	@Test
    public void testRouteQuery() {

        AccessPointAdvertisement ap = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        Vector addresses = new Vector();

        addresses.add("TCP:123.123.123.123");
        addresses.add("TCP:134.134.134.134");
        ap.setEndpointAddresses(addresses);

        // create the route
        RouteAdvertisement route = (RouteAdvertisement)
                AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());

        route.setDest(ap);

        AccessPointAdvertisement ap2 = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap2.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        addresses = new Vector();
        addresses.add("TCP:666.666.666.666");
        addresses.add("TCP:777.777.777.777");
        ap2.setEndpointAddresses(addresses);

        AccessPointAdvertisement ap4 = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap4.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        addresses = new Vector();
        addresses.add("TCP:888.888.888.888");
        addresses.add("TCP:999.999.999.999");
        ap4.setEndpointAddresses(addresses);

        Vector hops = new Vector();

        hops.add(ap2);
        hops.add(ap4);
        route.setHops(hops);

        PeerID pid = IDFactory.newPeerID(IDFactory.newPeerGroupID());
        Set badHops = new HashSet();
        RouteQuery query = new RouteQuery();
        query.setDestPeerID(pid);
        query.setSrcRoute(route);
        query.setBadHops(badHops);

        // write to a file
        try {
            ByteArrayOutputStream fp = new ByteArrayOutputStream();

            fp.write(query.toString().getBytes());
            fp.close();

            Reader is = new InputStreamReader(new ByteArrayInputStream(fp.toByteArray()));
            RouteQuery query1 = null;

            XMLDocument doc = (XMLDocument)
                    StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, is);

            query1 = new RouteQuery(doc);
            is.close();
            assertEquals(query.getDestPeerID().toString(), query1.getDestPeerID().toString());
            // verify advertisement
            ap = query.getSrcRoute().getDest();
            AccessPointAdvertisement ap1 = query1.getSrcRoute().getDest();

            assertEquals(ap.getPeerID(), ap1.getPeerID());
            Enumeration e1 = ap1.getEndpointAddresses();

            for (Enumeration e = ap.getEndpointAddresses(); e.hasMoreElements();) {
                assertEquals(e.nextElement(), e1.nextElement());
            }

            Enumeration r1 = query.getSrcRoute().getHops();

            for (Enumeration e = query1.getSrcRoute().getHops(); e.hasMoreElements();) {
                ap = (AccessPointAdvertisement) e.nextElement();
                ap1 = (AccessPointAdvertisement) r1.nextElement();
                assertEquals(ap.getPeerID(), ap1.getPeerID());
                e1 = ap1.getEndpointAddresses();
                for (Enumeration e2 = ap.getEndpointAddresses(); e2.hasMoreElements();) {
                    assertEquals(e2.nextElement(), e1.nextElement());
                }
            }
            System.out.println(query1.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Error constructing advertisement");
        }
    }

	@Test
    public void testRouteResponse() {
        AccessPointAdvertisement ap = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        Vector addresses = new Vector();

        addresses.add("TCP:123.123.123.123");
        addresses.add("TCP:134.134.134.134");
        ap.setEndpointAddresses(addresses);

        // create the route
        RouteAdvertisement route = (RouteAdvertisement)
                AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());

        route.setDest(ap);

        AccessPointAdvertisement ap2 = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap2.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        addresses = new Vector();
        addresses.add("TCP:666.666.666.666");
        addresses.add("TCP:777.777.777.777");
        ap2.setEndpointAddresses(addresses);

        AccessPointAdvertisement ap4 = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap4.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        addresses = new Vector();
        addresses.add("TCP:888.888.888.888");
        addresses.add("TCP:999.999.999.999");
        ap4.setEndpointAddresses(addresses);

        Vector hops = new Vector();

        hops.add(ap2);
        hops.add(ap4);
        route.setHops(hops);

        AccessPointAdvertisement apDst = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        apDst.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        addresses = new Vector();
        addresses.add("TCP:234.234.234.234");
        addresses.add("TCP:256.256.278.256");
        apDst.setEndpointAddresses(addresses);

        // create the route
        RouteAdvertisement routeDst = (RouteAdvertisement)
                AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());

        routeDst.setDest(apDst);

        AccessPointAdvertisement ap2Dst = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap2Dst.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        addresses = new Vector();
        addresses.add("TCP:166.166.166.166");
        addresses.add("TCP:277.277.277.277");
        ap2Dst.setEndpointAddresses(addresses);

        AccessPointAdvertisement ap4Dst = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());

        ap4Dst.setPeerID(IDFactory.newPeerID(IDFactory.newPeerGroupID()));
        addresses = new Vector();
        addresses.add("TCP:188.188.818.818");
        addresses.add("TCP:929.929.929.929");
        ap4Dst.setEndpointAddresses(addresses);

        Vector hopsDst = new Vector();

        hopsDst.add(ap2Dst);
        hopsDst.add(ap4Dst);
        routeDst.setHops(hopsDst);

        RouteResponse response = new RouteResponse();

        response.setDestRoute(routeDst);
        response.setSrcRoute(route);

        // write to a file
        try {
        	File routeResponseFile = tempStorage.newFile("routeresponse.msg");
            FileOutputStream fp = new FileOutputStream(routeResponseFile);

            fp.write(response.toString().getBytes("UTF-8"));
            fp.close();

            FileInputStream is = new FileInputStream(routeResponseFile);
            RouteResponse response1 = null;

            XMLDocument asDoc = (XMLDocument)
                    StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, is);

            is.close();

            response1 = new RouteResponse(asDoc);

            // verify advertisement
            ap = response.getSrcRoute().getDest();
            AccessPointAdvertisement ap1 = response1.getSrcRoute().getDest();

            assertEquals(ap.getPeerID(), ap1.getPeerID());
            Enumeration e1 = ap1.getEndpointAddresses();

            for (Enumeration e = ap.getEndpointAddresses(); e.hasMoreElements();) {
                assertEquals(e.nextElement(), e1.nextElement());
            }
            Enumeration r1 = response.getSrcRoute().getHops();

            for (Enumeration e = response.getSrcRoute().getHops(); e.hasMoreElements();) {
                ap = (AccessPointAdvertisement) e.nextElement();
                ap1 = (AccessPointAdvertisement) r1.nextElement();
                assertEquals(ap.getPeerID(), ap1.getPeerID());
                e1 = ap1.getEndpointAddresses();
                for (Enumeration e2 = ap.getEndpointAddresses(); e2.hasMoreElements();) {
                    assertEquals(e2.nextElement(), e1.nextElement());
                }
            }

            ap = response.getDestRoute().getDest();
            ap1 = response1.getDestRoute().getDest();
            assertEquals(ap.getPeerID(), ap1.getPeerID());
            e1 = ap1.getEndpointAddresses();
            for (Enumeration e = ap.getEndpointAddresses(); e.hasMoreElements();) {
                assertEquals(e.nextElement(), e1.nextElement());
            }
            r1 = response.getDestRoute().getHops();
            for (Enumeration e = response.getDestRoute().getHops(); e.hasMoreElements();) {
                ap = (AccessPointAdvertisement) e.nextElement();
                ap1 = (AccessPointAdvertisement) r1.nextElement();
                assertEquals(ap.getPeerID(), ap1.getPeerID());
                e1 = ap1.getEndpointAddresses();
                for (Enumeration e2 = ap.getEndpointAddresses(); e2.hasMoreElements();) {
                    assertEquals(e2.nextElement(), e1.nextElement());
                }
            }

            System.out.println(response1.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Error constructing advertisement");
        }

    }

}

