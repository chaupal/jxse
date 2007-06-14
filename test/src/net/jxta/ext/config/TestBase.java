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


import net.jxta.ext.config.Configurator;
import net.jxta.impl.endpoint.IPUtils;
import net.jxta.impl.protocol.PlatformConfig;

import java.io.File;
import java.io.InputStream;


/**
 *
 * @version $Id: TestBase.java,v 1.20 2007/01/26 02:13:54 bondolo Exp $
 *
 * @author james todd [gonzo at jxta dot org]
 */

public class TestBase extends junit.framework.TestCase {

    protected final static String PRINCIPAL = "principal";
    protected final static String PASSWORD = "password";
    protected final static String NAME = "name";
    protected final static String DESCRIPTION = "description";
    protected final static String ANY_ADDRESS = IPUtils.ANYADDRESS.getHostName();
    protected final static String TCP_ADDRESS = "1.2.3.4";
    protected final static int TCP_PORT = 4321;
    protected final static int HTTP_PORT = 1234;
    protected final static String IPV6_ADDRESS = "[1111:2222:3333:4444:5555:6666:7777:8888]";
    protected final long RENDEZVOUS_AUTO_START = 120000L;
    protected final static String MULTICAST_ADDRESS = Default.MULTICAST_IP;
    protected final static int MULTICAST_PORT = Default.MULTICAST_PORT;
    protected final static int MULTICAST_SIZE = Default.MULTICAST_SIZE;
    protected final static String RESOURCE_KEY = "resourceKey";
    protected final static String RESOURCE_VALUE = "resourceValue";
    protected final static File HOME = Env.JXTA_HOME;
    protected final static String PROFILE_EDGE_NULL_RENDEZVOUS_BOOTSTRAP = "/net/jxta/ext/config/resources/edgeNullRdvBootstrap.xml";
    protected final static String PROFILE_EDGE_NULL_RELAYS_BOOTSTRAP = "/net/jxta/ext/config/resources/edgeNullRlyBootstrap.xml";
    protected final static String PROFILE_SUPER_NULL_RELAYS_BOOTSTRAP = "/net/jxta/ext/config/resources/superNullRlyBootstrap.xml";
    protected final static String PROFILE_EDGE_TCP_ANY_ADDRESS = "/net/jxta/ext/config/resources/edgeTcpAnyAddress.xml";
    protected final static String PROFILE_EDGE_HTTP_ANY_ADDRESS = "/net/jxta/ext/config/resources/edgeHttpAnyAddress.xml";
    protected final static String PROFILE_EDGE_TCP_IPV6_ADDRESS = "/net/jxta/ext/config/resources/edgeTcpIpv6Address.xml";
    protected final static String PROFILE_EDGE_PRIVATE_NETWORK_PARTIAL_ID = "/net/jxta/ext/config/resources/edgePrivateNetworkPartialId.xml";
    protected final static String PROFILE_SUPER_RENDEZVOUS_AUTO_START = "/net/jxta/ext/config/resources/superRdvAutoStart.xml";
    protected final static String PROFILE_EDGE_MULTICAST = "/net/jxta/ext/config/resources/edgeMulticast.xml";
    protected final static String PROFILE_EDGE_NO_MULTICAST = "/net/jxta/ext/config/resources/edgeNoMulticast.xml";
    protected final static String PROFILE_EDGE_MULTICAST_DISABLED = "/net/jxta/ext/config/resources/edgeMulticastDisabled.xml";
    protected final static String PROFILE_EDGE_MULTICAST_UNSPECIFIED_ADDRESS = "/net/jxta/ext/config/resources/edgeMulticastUnspecifiedAddress.xml";
    protected final static String PROFILE_EDGE_NAT_ANYALL = "/net/jxta/ext/config/resources/edgeNatAnyAll.xml";
    protected final static String PROFILE_EDGE_NAT = "/net/jxta/ext/config/resources/edgeNat.xml";
    protected final static String PROFILE_EDGE_NO_RELAY_NAT = "/net/jxta/ext/config/resources/edgeNoRelayNat.xml";
    protected final static String PROFILE_EDGE_NO_RELAY_PRIVATE = "/net/jxta/ext/config/resources/edgeNoRelayPrivate.xml";
    protected final static String PROFILE_EDGE_NO_RELAY = "/net/jxta/ext/config/resources/edgeNoRelay.xml";
    protected final static String PROFILE_EDGE_PRIVATE = "/net/jxta/ext/config/resources/edgePrivate.xml";
    protected final static String PROFILE_SUPER_NAT = "/net/jxta/ext/config/resources/superNat.xml";
    protected final static String PROFILE_SUPER_PRIVATE = "/net/jxta/ext/config/resources/superPrivate.xml";
    protected final static String PROFILE_LOCAL_PRIVATE = "/net/jxta/ext/config/resources/localPrivate.xml";
    protected final static String PROFILE_ADHOC_PRIVATE = "/net/jxta/ext/config/resources/adhocPrivate.xml";
               
    protected PlatformConfig pc = null;
    
    static {
        HOME.delete();
    }

    public TestBase(String s) {
        super(s);
    }

    @Override
    protected void setUp() {
        clean();
    }

    @Override
    protected void tearDown() {
        clean();

        System.gc();
        System.out.flush();
    }

    protected boolean exists(File f) {
        return f != null && f.exists();
    }

    protected void delete(File f) {
        if (f != null) {
            f.delete();
        }
    }

    protected PlatformConfig getPlatformConfig() {
        return this.pc;
    }

    protected void setPlatformConfig(PlatformConfig pc) {
        this.pc = pc;
    }

    protected void clean() {
        setPlatformConfig(null);
    }
    
    protected Configurator configuratorFromResource(String r) {
        Configurator c = new Configurator(new Profile(getResourceAsStream(r)));
        
        return c;
    }
        
    protected InputStream getResourceAsStream(String r) {
        return getClass().getResourceAsStream(r);
    }
}
