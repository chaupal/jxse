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

package net.jxta.endpoint.router;

import java.util.Collection;
import net.jxta.peer.PeerID;
import net.jxta.protocol.RouteAdvertisement;

/**
 * The objects implementing this interface are managing route advertisements to remote peers.
 */
public interface RouteController {

    /**
     * return value for operation
     */

    /**
     * Operation was successful.
     */
    public final static int OK = 0; // operation succeeded

    /**
     * Route already exists.
     */
    public final static int ALREADY_EXIST = 1; // failed route already exists

    /**
     * Failed operation
     */
    public final static int FAILED = -1; // failed operation

    /**
     * This is an 'opened' direct route to a peer. Unsucessful operation.
     */
    public final static int DIRECT_ROUTE = 2; // failed direct route

    /**
     * This is an invalid route.
     */
    public final static int INVALID_ROUTE = 3; // invalid route

    /**
     * Adds a new route. For the route to be useful, we actively verify
     * the route by trying it
     *
     * @param newRoute route to add
     * @return Integer status (OK, FAILED, INVALID_ROUTE or ALREADY_EXIST)
     */
    int addRoute(RouteAdvertisement newRoute);

    /**
     * Returns all knowns routes.
     *
     * @return a collection of known routes
     */
    public Collection<RouteAdvertisement> getAllRoutes();

    /**
     * Returns all knowns outes to a peer using its ID.
     *
     * @return a collection of known routes to a peer.
     */
    public Collection<RouteAdvertisement> getRoutes(PeerID inPID);

    /**
     * Returns a route advertisement to this peer.
     *
     * @return RoutAdvertisement of the local route
     */
    public RouteAdvertisement getLocalPeerRoute();

    /**
     * Determines whether a connection to a specific node exists, or if one can be created.
     * This method can block to ensure a usable connection exists, it does so by sending an empty
     * message.
     *
     * @param pid Node ID
     * @return true, if a connection already exists, or a new was sucessfully created
     */
    public boolean isConnected(PeerID pid);

}
