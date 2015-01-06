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

package net.jxta.impl.protocol;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Attributable;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.id.IDFactory;
import net.jxta.impl.membership.pse.PSECredential;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.RouteAdvertisement;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * RouteQuery message used by the Endpoint Routing protocol to
 * query for route
 */
public class RouteQuery  {

	private PeerID destPID = null;
    private RouteAdvertisement srcRoute = null;
    private final Set<PeerID> badHops = new HashSet<PeerID>();

    private static final String destPIDTag = "Dst";
    private static final String srcRouteTag = "Src";
    private static final String badHopTag = "Bad";
    private PeerGroup group = null;

    /**
     * Default Constructor
     */
    public RouteQuery() {}

    public RouteQuery(PeerID dest, RouteAdvertisement srcRoute, Collection<PeerID> badhops, PeerGroup paramGroup) {
        setDestPeerID(dest);
        setSrcRoute(srcRoute);
        setBadHops(badhops);
        this.group=paramGroup;
    }

    /**
     * All messages have a type (in xml this is !doctype)
     * which identifies the message
     *
     * @return String "jxta:ERQ"
     */
    public static String getAdvertisementType() {
        return "jxta:ERQ";
    }
    
    /**
     * set the peergroup
     *
     * @param peerGroup PeerGroup
     */
    public void setPeerGroup(PeerGroup peerGroup) {
        group = peerGroup;
    }

    /**
     * set the destination PeerID we are searching a route for
     *
     * @param pid destination peerID
     */
    public void setDestPeerID(PeerID pid) {
        destPID = pid;
    }

    /**
     * returns the destination peer ID we are looking for
     *
     * @return pid PeerID of the route destination
     */

    public PeerID getDestPeerID() {
        return destPID;
    }

    /**
     * set the Route advertisement of the source peer that is originating
     * the query
     *
     * @param route RouteAdvertisement of the source
     */
    public void setSrcRoute(RouteAdvertisement route) {
        if(null == route.getDestPeerID()) {
            throw new IllegalArgumentException("route lacks destination!");
        }

        srcRoute = route.clone();
    }

    /**
     * returns the route of the src peer that issued the routequery
     *
     * @return route RouteAdvertisement of the source peer
     */
    public RouteAdvertisement getSrcRoute() {
        if(null == srcRoute) {
            return null;
        } else {
            return srcRoute.clone();
        }
    }

    /**
     * Adds a bad hop to the list of those known to be bad for this route.
     *
     * @param badHop The known bad hop for the route.
     */
    public void addBadHop(PeerID badHop) {
        badHops.add(badHop);
    }

    /**
     * Set the bad hops known into that route
     *
     * @param hops RouteAdvertisement of the source
     */
    public void setBadHops(Collection<PeerID> hops) {
        badHops.clear();
        if (null != hops) {
            badHops.addAll(hops);
        }
    }

    /**
     * returns the bad hops know to the route
     *
     * @return The known bad hops for the route
     */
    public Set<PeerID> getBadHops() {
        return new HashSet<PeerID>(badHops);
    }

    /**
     * Construct from an XML document fragment.
     *
     * @param doc the element
     */
    public RouteQuery(XMLElement doc, PeerGroup paramGroup) {
        this.group=paramGroup;
        String doctype = doc.getName();

        if (!doctype.equals(getAdvertisementType())) {
            throw new IllegalArgumentException(
                    "Can not construct : " + getClass().getName() + " from doc containing a " + doctype);
        }

        Enumeration<XMLElement> elements = doc.getChildren();

        while (elements.hasMoreElements()) {
            XMLElement elem = elements.nextElement();

            if (elem.getName().equals(destPIDTag)) {
                try {
                    URI pID = new URI(elem.getTextValue());
                    PeerID pid = (PeerID) IDFactory.fromURI(pID);

                    setDestPeerID(pid);
                } catch (URISyntaxException badID) {
                    throw new IllegalArgumentException("Bad PeerID ID in advertisement");
                } catch (ClassCastException badID) {
                    throw new IllegalArgumentException("Not a peer id");
                }
                continue;
            }

            if (elem.getName().equals(srcRouteTag)) {
                for (Enumeration<XMLElement> eachXpt = elem.getChildren(); eachXpt.hasMoreElements();) {
                    XMLElement aXpt = eachXpt.nextElement();

                    RouteAdvertisement route = (RouteAdvertisement) AdvertisementFactory.newAdvertisement(aXpt);

                    setSrcRoute(route);
                }
                continue;
            }

            if (elem.getName().equals(badHopTag)) {
                try {
                    URI pID = new URI(elem.getTextValue());
                    PeerID pid = (PeerID) IDFactory.fromURI(pID);

                    addBadHop(pid);
                } catch (URISyntaxException badID) {
                    throw new IllegalArgumentException("Bad PeerID ID in advertisement");
                } catch (ClassCastException badID) {
                    throw new IllegalArgumentException("Not a peer id");
                }
            }
        }

        if(null == getDestPeerID()) {
            throw new IllegalArgumentException("Destination peer not initialized");
        }
    }

    public StructuredDocument getDocument(MimeMediaType asMimeType) {
        if(null == getDestPeerID()) {
            throw new IllegalStateException("Destination peer not initialized");
        }

        StructuredDocument adv = StructuredDocumentFactory.newStructuredDocument(asMimeType, getAdvertisementType());

        if (adv instanceof XMLElement) {
            ((Attributable) adv).addAttribute("xmlns:jxta", "http://jxta.org");
            ((Attributable) adv).addAttribute("xml:space", "preserve");
        }

        Element e;

        PeerID dest = getDestPeerID();

        e = adv.createElement(destPIDTag, dest.toString());
        adv.appendChild(e);

        RouteAdvertisement route = getSrcRoute();

        if (route != null) {
            e = adv.createElement(srcRouteTag);
            adv.appendChild(e);
            PSEMembershipService tempPSE = (PSEMembershipService)this.group.getMembershipService();
            PSECredential tempCred = (PSECredential)tempPSE.getDefaultCredential();
            route.sign(tempCred, true, true);
            StructuredDocument xptDoc = (StructuredDocument) route.getSignedDocument();

            StructuredDocumentUtils.copyElements(adv, e, xptDoc);
        }

        for (PeerID eachPeer : getBadHops()) {
            e = adv.createElement(badHopTag, eachPeer.toString());
            adv.appendChild(e);
        }

        return adv;
    }

    /**
     * return a string representation of this RouteQuery doc
     */
    @Override
    public String toString() {
        XMLDocument doc = (XMLDocument) getDocument(MimeMediaType.XMLUTF8);

        doc.addAttribute("xml:space", "default");

        return doc.toString();
    }
}
