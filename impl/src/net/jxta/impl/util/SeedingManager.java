/*
 * Copyright (c) 2002-2004 Sun Microsystems, Inc.  All rights reserved.
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

package net.jxta.impl.util;

import java.net.URI;

import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.RouteAdvertisement;

/**
 * Manages the location of seed peers.
 */
public interface SeedingManager {
    
    /**
     *  Stop this seeding manager.
     */
    public void stop(); 
    
    /**
     * Returns the route advertisements of the active seed peers. The route
     * advertisements are returned in the order which the consumer should
     * attempt to contact the seed peers. In some cases the returned route 
     * advertisements may omit the destination {@code PeerID} if it is not 
     * known.
     *
     * @return The route advertisements of the active seed peers in the order
     * in which the seed peers should be contacted.
     */
    public RouteAdvertisement[] getActiveSeedRoutes();
    
    /**
     * Returns the {@code URI} of the endpoint addresses of the active seed
     * peers. The {@code URI}s are returned in the order which the consumer
     * should attempt to contact the seed peers. 
     * 
     * <p/>Using the endpoint address {@code URI}s is less optimal than using 
     * the route advertisements as there is no association between the 
     * potentially multiple message transport addresses referring to a single 
     * peer.
     * 
     * @return The {@code URI}s of the active seed peers in the order
     * in which the seed peers should be contacted.
     */
    public URI[] getActiveSeedURIs();
    
    /**
     * Returns {@code true} if the provided peer advertisement is an acceptable
     * peer as determined by the seeding manager.
     *
     * @param peeradv The {@code PeerAdvertisement} of the peer being tested.
     */
    public boolean isAcceptablePeer(PeerAdvertisement peeradv);
    
    /**
     * Returns {@code true} if the provided route advertisement is an acceptable
     * peer as determined by the seeding manager.
     *
     * @param radv The {@code RouteAdvertisement} of the peer being tested.
     */
    public boolean isAcceptablePeer(RouteAdvertisement radv);
}
