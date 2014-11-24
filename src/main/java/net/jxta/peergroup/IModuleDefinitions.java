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

package net.jxta.peergroup;

import java.net.URI;

import net.jxta.id.ID;
import net.jxta.peergroup.core.ModuleClassID;
import net.jxta.peergroup.core.ModuleSpecID;

/**
 * TODO: keesp: JxtaLoader removed
 * 
 * Peer groups are formed as a collection of peers that have agreed upon a
 * common set of services. Each peer group is assigned a unique peer group ID
 * and a peer group advertisement. The peer group advertisement contains a
 * ModuleSpecID which refers to a module specification for this peer group.
 * <p/>
 * The peer group specification mandates each of the group services (membership,
 * discovery, resolver, etc). Implementations of that specification are
 * described by ModuleImplAdvertisements which are identified by the group's
 * ModuleSpecID. Implementations are responsible for providing the services mandated
 * by the specification.
 * <p/>
 * The java reference implementation achieves this by loading additional Modules
 * which ModuleSpecIDs are listed by the group implementation advertisement.
 * <p/>
 * In order to fully participate in a group, a peer may need to authenticate
 * with the group using the peer group membership service.
 *
 * @see net.jxta.peergroup.PeerGroupID
 * @see net.jxta.service.Service
 * @see net.jxta.peergroup.PeerGroupFactory
 * @see net.jxta.protocol.PeerGroupAdvertisement
 * @see net.jxta.protocol.ModuleImplAdvertisement
 * @see net.jxta.peergroup.core.ModuleSpecID
 * @see net.jxta.peergroup.core.ModuleClassID
 */

public interface IModuleDefinitions{

    /**
     * Well known classes for the basic services.
     *
     * <p/>FIXME: we should make a "well-known ID" encoding implementation that
     * has its own little name space of human readable names...later.
     * To keep their string representation shorter, we put our small spec
     * or role pseudo unique ID at the front of the second UUID string.
     * Base classes do not need an explicit second UUID string because it is
     * all 0.
     *
     * <p/>The type is always the last two characters, no-matter the total length.
     */

    /**
     * Prefix string for all of the Well Known IDs declared in this interface.
     */
    static final String WK_ID_PREFIX = ID.URIEncodingName + ":" + ID.URNNamespace + ":uuid-DeadBeefDeafBabaFeedBabe";

    public enum DefaultModules{
    	PEERGROUP,
    	RESOLVER,
    	DISCOVERY,
    	PIPE,
    	MEMBERSHIP,
    	RENDEZVOUS,
    	ENDPOINT,
    	TCP,
    	MULTICAST,
    	HTTP,
    	ROUTER,
    	APPLICATION,
    	TLS,
    	RELAY,
    	ACCESS,
    	CONTENT;

    	/**
    	 * Get the corresponding module class ids
    	 * @param module
    	 * @return
    	 */
    	public static ModuleClassID getModuleClassID( DefaultModules module ) {
    		String str = WK_ID_PREFIX;
    		switch( module ){
    		case APPLICATION:
    			str += "0000000C05";
    			break;
    		case DISCOVERY:
    			str += "0000000305";	
    			break;
    		case ENDPOINT:
    			str += "0000000805";
    			break;
    		case HTTP:
    			str += "0000000A05";
    			break;
    		case MEMBERSHIP:
    			str += "0000000505";
    			break;
    		case MULTICAST:
    			str += "0000001105";
    			break;
    		case PEERGROUP:
    			str += "0000000705";
    			break;
    		case PIPE:
    			str += "0000000405";
    			break;
    		case RENDEZVOUS:
    			str += "0000000605";
    			break;
    		case RESOLVER:
    			str +=  "0000000205";
    			break;
    		case ROUTER:
    			str +=  "0000000B05";
    			break;
    		case TCP:
    			str += "0000000905";
    			break;
    		case TLS:
    			str += "0000000D05";
    			break;
    		case ACCESS:
    			str += "0000001005";
    			break;
    		case CONTENT:
    			str = "urn:jxta:uuid-DDC5CA55578E4AB99A0AA81D2DC6EF3F05";
    			break;
    		case RELAY:
    			str += "0000000F05";
    			break;
    		}
    		return ModuleClassID.create(URI.create( str ));
    	}

    	/**
    	 * Get the corresponding module class ids
    	 * @param module
    	 * @return
    	 */
    	public static ModuleSpecID getModuleSpecID( DefaultModules module ) {
    		String str = WK_ID_PREFIX;
    		switch( module ){
    		case APPLICATION:
    			str += "0000000C05";
    			break;
    		case DISCOVERY:
    			str += "000000030106";	
    			break;
    		case ENDPOINT:
    			str += "000000080106";
    			break;
    		case HTTP:
    			str += "0000000A0106";
    			break;
    		case MEMBERSHIP:
    			str += "000000050106";
    			break;
    		case MULTICAST:
    			str += "0000001105";/**/
    			break;
    		case PEERGROUP:
    			str +=  "000000010306";
    			break;
    		case PIPE:
    			str += "000000040106";
    			break;
    		case RENDEZVOUS:
    			str += "000000060106";
    			break;
    		case RESOLVER:
    			str +=  "000000020106";
    			break;
    		case ROUTER:
    			str += "0000000B0106";
    			break;
    		case TCP:
    			str += "000000090106";
    			break;
    		case TLS:
    			str += "0000000D0106";
    			break;
    		case ACCESS:
    			str += "000000100106";
    			break;
    		case CONTENT:
    			//str = "urn:jxta:uuid-DDC5CA55578E4AB99A0AA81D2DC6EF3F05";
    			break;
    		case RELAY:
    			str += "0000000F0106";
    			break;
    		}
    		return ModuleSpecID.create(URI.create( str ));
    	}
    }

    
    /**
     * Well known module class identifier: peer group
     */
    public final static ModuleClassID peerGroupClassID =
    		ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000000705"));

    /**
     * Well known module class identifier: resolver service
     */
    public final static ModuleClassID resolverClassID =
            ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000000205"));

    /**
     * Well known module class identifier: discovery service
     */
    public final static ModuleClassID discoveryClassID =
            ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000000305"));

    /**
     * Well known module class identifier: pipe service
     */
    public final static ModuleClassID pipeClassID =
            ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000000405"));

    /**
     * Well known module class identifier: membership service
     */
    public final static ModuleClassID membershipClassID =
            ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000000505"));

    /**
     * Well known module class identifier: rendezvous service
     */
    public final static ModuleClassID rendezvousClassID =
            ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000000605"));

    /**
     * Well known module class identifier: peerinfo service
     */
    public final static ModuleClassID peerinfoClassID =
            ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000000705"));

    /**
     * Well known module class identifier: endpoint service
     */
    public final static ModuleClassID endpointClassID =
            ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000000805"));

    // FIXME: EndpointProtocols should probably all be of the same class
    // and of different specs and roles... But we'll take a shortcut for now.

    /**
     * Well known module class identifier: tcp protocol
     */
    public final static ModuleClassID tcpProtoClassID =
            ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000000905"));

    /**
     * Well known module class identifier: mutlicast protocol
     */
    public final static ModuleClassID multicastProtoClassID =
            ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000001105"));

    /**
     * Well known module class identifier: http protocol
     */
    public final static ModuleClassID httpProtoClassID =
           ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000000A05"));

    /**
     * Well known module class identifier: http2 (netty http tunnel) protocol
     */
    public final static ModuleClassID http2ProtoClassID =
    		ModuleClassID.create(URI.create(ID.URIEncodingName + ":" + ID.URNNamespace + ":uuid-E549DB3BCBCF4789A392B6100B78CC5505"));

    /**
     * Well known module class identifier: router protocol
     */
    public final static ModuleClassID routerProtoClassID =
            ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000000B05"));

    /**
     * Well known module class identifier: application
     */
    public final static ModuleClassID applicationClassID =
            ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000000C05"));

    /**
     * Well known module class identifier: tlsProtocol
     */
    public final static ModuleClassID tlsProtoClassID =
            ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000000D05"));

//    /**
//     * Well known module class identifier: ProxyService
//     */
//    @Deprecated
//    public final static ModuleClassID proxyClassID =
//            ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000000E05"));

    /**
     * Well known module class identifier: RelayProtocol
     */
    public final static ModuleClassID relayProtoClassID =
            ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000000F05"));

    /**
     * Well known module class identifier: AccessService
     */
    public final static ModuleClassID accessClassID =
            ModuleClassID.create(URI.create(WK_ID_PREFIX + "0000001005"));

    /**
     * Well known module class identifier: content service
     */
    public final static ModuleClassID contentClassID =
            ModuleClassID.create(URI.create(
            "urn:jxta:uuid-DDC5CA55578E4AB99A0AA81D2DC6EF3F05"));


    /**
     * Well known group specification identifier: the platform
     */
    public final static ModuleSpecID refPlatformSpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "000000010106"));

    /**
     * Well known group specification identifier: the Network Peer Group
     */
    public final static ModuleSpecID refNetPeerGroupSpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "000000010206"));

    /**
     * Well known service specification identifier: the standard resolver
     */
    public final static ModuleSpecID refResolverSpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "000000020106"));

    /**
     * Well known service specification identifier: the standard discovery
     */
    public final static ModuleSpecID refDiscoverySpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "000000030106"));

    /**
     * Well known service specification identifier: the standard pipe service
     */
    public final static ModuleSpecID refPipeSpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "000000040106"));

    /**
     * Well known service specification identifier: the standard membership
     */
    public final static ModuleSpecID refMembershipSpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "000000050106"));

    /**
     * Well known service specification identifier: the standard rendezvous
     */
    public final static ModuleSpecID refRendezvousSpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "000000060106"));

    /**
     * Well known service specification identifier: the standard peerinfo
     */
    public final static ModuleSpecID refPeerinfoSpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "000000070106"));

    /**
     * Well known service specification identifier: the standard endpoint
     */
    public final static ModuleSpecID refEndpointSpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "000000080106"));

    /**
     * Well known endpoint protocol specification identifier: the standard
     * tcp endpoint protocol
     */
    public final static ModuleSpecID refTcpProtoSpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "000000090106"));

    /**
     * Well known endpoint protocol specification identifier: the standard
     * http endpoint protocol
     */
    public final static ModuleSpecID refHttpProtoSpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "0000000A0106"));

    /**
     * Well known endpoint protocol specification identifier: the new (2.6+)
     * http2 endpoint protocol (netty http tunnel based)
     */
    public final static ModuleSpecID refHttp2ProtoSpecID =
    		ModuleSpecID.create(URI.create(ID.URIEncodingName + ":" + ID.URNNamespace + ":uuid-E549DB3BCBCF4789A392B6100B78CC55F127AD1AADF0443ABF6FBDFD7909876906"));

    /**
     * Well known endpoint protocol specification identifier: the standard
     * router
     */
    public final static ModuleSpecID refRouterProtoSpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "0000000B0106"));

    /**
     * Well known endpoint protocol specification identifier: the standard
     * tls endpoint protocol
     */
    public final static ModuleSpecID refTlsProtoSpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "0000000D0106"));

    /**
     * Well known group specification identifier: an all purpose peer group
     * specification. The java reference implementation implements it with
     * the StdPeerGroup class and all the standard platform services and no
     * endpoint protocols.
     */
    public final static ModuleSpecID allPurposePeerGroupSpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "000000010306"));

//    /**
//     * Well known application: the shell
//     */
//    public final static ModuleSpecID refShellSpecID =
//            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "0000000C0206"));

//    /**
//     * Well known application: the Proxy
//     */
//    public final static ModuleSpecID refProxySpecID =
//            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "0000000E0106"));

    /**
     * Well known endpoint protocol specification identifier: the standard
     * relay endpoint protocol
     */
    public final static ModuleSpecID refRelayProtoSpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "0000000F0106"));

    /**
     * Well known access specification identifier: the standard
     * access service
     */
    public final static ModuleSpecID refAccessSpecID =
            ModuleSpecID.create(URI.create(WK_ID_PREFIX + "000000100106"));
}
