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


import net.jxta.exception.ConfiguratorException;
import net.jxta.impl.protocol.PlatformConfig;

import java.io.File;
import java.net.URI;
import java.util.Iterator;


/**
 *
 * @version $Id: ProfileTest.java,v 1.24 2007/01/26 02:13:54 bondolo Exp $
 *
 * @author james todd [gonzo at jxta dot org]
 */

public class ProfileTest extends TestBase {

    public static void main(String[] argv) {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.TestSuite suite() {
        junit.framework.TestSuite suite = new junit.framework.TestSuite();

        suite.addTest(new junit.framework.TestSuite(ProfileTest.class));

        return suite;
    }

    public ProfileTest() {
        this(ProfileTest.class.getName());
    }

    public ProfileTest(String name) {
        super(name);
    }

    public void testWrite() {
        Configurator c = new Configurator(Profile.EDGE);
        File f = new File(HOME, getClass().getName() + ".testWrite");

        f.mkdirs();
       
        c.setSecurity(PRINCIPAL, PASSWORD);

        try {
            c.save(f);
        } catch (ConfiguratorException ce) {
            fail("Configurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(f, Env.PLATFORM_CONFIG)));
    }

    public void testWriteToFile() {
        Configurator c = new Configurator(Profile.EDGE);
        File f = new File(HOME, getClass().getName() + ".testWriteToFile" + File.separator + Env.PLATFORM_CONFIG + ".test");

        c.setSecurity(PRINCIPAL, PASSWORD);

        try {
            c.save(f);
        } catch (ConfiguratorException ce) {
            fail("Configurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(f));
    }
    
    public void testEdge() {
        Configurator c = new Configurator(Profile.EDGE);

        c.setSecurity(PRINCIPAL, PASSWORD);
        
        try {
            setPlatformConfig(c.getPlatformConfig());
        } catch (ConfiguratorException ce) {
            fail("Configurator.save() exception: " + ce.getMessage());
        }

        assertNotNull(getPlatformConfig());
    }

    public void testSuper() {
        Configurator c = new Configurator(Profile.SUPER);

        c.setSecurity(PRINCIPAL, PASSWORD);
        
        try {
            setPlatformConfig(c.getPlatformConfig());
        } catch (ConfiguratorException ce) {
            fail("Configurator.save() exception: " + ce.getMessage());
        }

        assertNotNull(getPlatformConfig());
    }
    
    public void testTcpAnyAddress() {
        Configurator c = configuratorFromResource(PROFILE_EDGE_TCP_ANY_ADDRESS);
        
        c.setSecurity(PRINCIPAL, PASSWORD);
        
        URI address = null;
        
        for (Iterator ti = c.getTransports().iterator(); ti.hasNext();) {
            Transport t = (Transport) ti.next();
            
            for (Iterator ai = t.getAddresses().iterator(); ai.hasNext();) {
                Address a = (Address) ai.next();
                
                if (a instanceof TcpTransportAddress) {
                    address = a.getAddress();
                }
            }
        }
        
        assertNotNull(address);
        assertEquals(ANY_ADDRESS, address != null ? address.getHost() : null);
        assertEquals(TCP_PORT, address != null ? address.getPort() : -1);
    }
    
    public void testHttpAnyAddress() {
        Configurator c = configuratorFromResource(PROFILE_EDGE_HTTP_ANY_ADDRESS);
        
        c.setSecurity(PRINCIPAL, PASSWORD);
        
        URI address = null;
        
        for (Iterator ti = c.getTransports().iterator(); ti.hasNext();) {
            Transport t = (Transport) ti.next();
            
            for (Iterator ai = t.getAddresses().iterator(); ai.hasNext();) {
                Address a = (Address) ai.next();
                
                if (a instanceof TcpTransportAddress) {} else if (a instanceof Address) {
                    address = a.getAddress();
                }
            }
        }
        
        assertNotNull(address);
        assertEquals(ANY_ADDRESS, address != null ? address.getHost() : null);
        assertEquals(HTTP_PORT, address != null ? address.getPort() : -1);
    }
    
    public void testTcpIpv6Address() {
        Configurator c = configuratorFromResource(PROFILE_EDGE_TCP_IPV6_ADDRESS);
        
        c.setSecurity(PRINCIPAL, PASSWORD);
        
        URI address = null;
        
        for (Iterator ti = c.getTransports().iterator(); ti.hasNext();) {
            Transport t = (Transport) ti.next();
            
            for (Iterator ai = t.getAddresses().iterator(); ai.hasNext();) {
                Address a = (Address) ai.next();
                
                if (a instanceof TcpTransportAddress) {
                    address = a.getAddress();
                }
            }
        }
        
        assertNotNull(address);
        assertEquals(IPV6_ADDRESS, address != null ? address.getHost() : null);
        assertEquals(TCP_PORT, address != null ? address.getPort() : -1);
    }

    public void testProfileParitialId() {
        Configurator c = configuratorFromResource(PROFILE_EDGE_PRIVATE_NETWORK_PARTIAL_ID);
        
        c.setSecurity(PRINCIPAL, PASSWORD);

        try {
            setPlatformConfig(c.getPlatformConfig());
        } catch (ConfiguratorException ce) {
            fail("Configurator.save() exception: " + ce.getMessage());
        }

        assertNotNull(getPlatformConfig());
    }

    public void testRendezVousAutoStart() {
        Configurator c = configuratorFromResource(PROFILE_SUPER_RENDEZVOUS_AUTO_START);

        assertEquals(c.getRendezVousAutoStart(), RENDEZVOUS_AUTO_START);
    }

    public void testProfileAdhoc() {
        File h = new File(HOME, getClass().getName() + ".testProfileAdhoc");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), Profile.ADHOC) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };

        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }
        
    public void testProfileEdge() {
        File h = new File(HOME, getClass().getName() + ".testProfileEdge");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), Profile.EDGE) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }
    
    public void testProfileLocal() {
        File h = new File(HOME, getClass().getName() + ".testProfileLocal");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), Profile.LOCAL) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }
    
    public void testProfileRelay() {
        File h = new File(HOME, getClass().getName() + ".testProfileRelay");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), Profile.RELAY) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }
    
    public void testProfileRendezVous() {
        File h = new File(HOME, getClass().getName() + ".testProfileRendezVous");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), Profile.RENDEZVOUS) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }
    
    public void testProfileSuper() {
        File h = new File(HOME, getClass().getName() + ".testProfileSuper");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), Profile.SUPER) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }
    
    public void testProfileDefault() {
        File h = new File(HOME, getClass().getName() + ".testProfileDefault");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), Profile.DEFAULT) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }
    
    public void testAdhocPrivate() {
        File h = new File(HOME, getClass().getName() + ".testAdhocPrivate");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), new Profile(getClass().getResource(PROFILE_ADHOC_PRIVATE))) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
        assertTrue(exists(new File(h, Env.CONFIG_PROPERTIES)));
    }
    
    public void testEdgeNatAnyAll() {
        File h = new File(HOME, getClass().getName() + ".testEdgeNatAnyAll");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), new Profile(getClass().getResource(PROFILE_EDGE_NAT_ANYALL))) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }
    
    public void testEdgeNat() {
        File h = new File(HOME, getClass().getName() + ".testEdgeNat");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), new Profile(getClass().getResource(PROFILE_EDGE_NAT))) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }
    
    public void testEdgeNoRelayNat() {
        File h = new File(HOME, getClass().getName() + ".testEdgeNoRelayNat");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI()
                ,
                new Profile(getClass().getResource(PROFILE_EDGE_NO_RELAY_NAT))) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }
    
    public void testEdgeNoRelayPrivate() {
        File h = new File(HOME, getClass().getName() + ".testEdgeNoRelayPrivate");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI()
                ,
                new Profile(getClass().getResource(PROFILE_EDGE_NO_RELAY_PRIVATE))) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
        assertTrue(exists(new File(h, Env.CONFIG_PROPERTIES)));
    }
    
    public void testEdgeNoRelay() {
        File h = new File(HOME, getClass().getName() + ".testEdgeNoRelay");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), new Profile(getClass().getResource(PROFILE_EDGE_NO_RELAY))) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }
    
    public void testEdgePrivate() {
        File h = new File(HOME, getClass().getName() + ".testEdgePrivate");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), new Profile(getClass().getResource(PROFILE_EDGE_PRIVATE))) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
        assertTrue(exists(new File(h, Env.CONFIG_PROPERTIES)));
    }
    
    public void testLocalPrivate() {
        File h = new File(HOME, getClass().getName() + ".testLocalPrivate");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), new Profile(getClass().getResource(PROFILE_LOCAL_PRIVATE))) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
        assertTrue(exists(new File(h, Env.CONFIG_PROPERTIES)));
    }
    
    public void testSuperNat() {
        File h = new File(HOME, getClass().getName() + ".testSuperNat");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), new Profile(getClass().getResource(PROFILE_SUPER_NAT))) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }
    
    public void testSuperPrivate() {
        File h = new File(HOME, getClass().getName() + ".testSuperPrivate");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), new Profile(getClass().getResource(PROFILE_SUPER_PRIVATE))) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
        assertTrue(exists(new File(h, Env.CONFIG_PROPERTIES)));
    }
    
    public void testEdgeMulticast() {
        File h = new File(HOME, getClass().getName() + ".testEdgeMulticast");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI(), new Profile(getClass().getResource(PROFILE_EDGE_MULTICAST))) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }

    public void testEdgeNoMulticast() {
        File h = new File(HOME, getClass().getName() + ".testEdgeNoMulticast");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI()
                ,
                new Profile(getClass().getResource(PROFILE_EDGE_NO_MULTICAST))) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }

    public void testEdgeMulticastDisabled() {
        File h = new File(HOME, getClass().getName() + ".testEdgeMulticastDisabled");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI()
                ,
                new Profile(getClass().getResource(PROFILE_EDGE_MULTICAST_DISABLED))) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }

    public void testEdgeMulticastUnspecifiedAddress() {
        File h = new File(HOME, getClass().getName() + ".testEdgeMulticastUnspecifiedAddress");
        AbstractConfigurator ac = new AbstractConfigurator(h.toURI()
                ,
                new Profile(getClass().getResource(PROFILE_EDGE_MULTICAST_UNSPECIFIED_ADDRESS))) {

            @Override
            public PlatformConfig createPlatformConfig(Configurator c) throws ConfiguratorException {
                return c.getPlatformConfig();
            }
        };
        
        try {
            ac.save();
        } catch (ConfiguratorException ce) {
            fail("AbstractConfigurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }
    
    public void testDefaultSave() {
        File h = new File(HOME, getClass().getName() + ".testDefaultSave");
        
        h.mkdirs();
        
        try {
            new Configurator().save(h);
        } catch (ConfiguratorException ce) {
            fail("Configurator.save() exception: " + ce.getMessage());
        }
        
        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }
    
    public void testWhiteSpaceSave() {
        File f = new File(HOME, getClass().getName() + ".testWhiteSpace" + File.separator + Env.PLATFORM_CONFIG + ".test");
        
        try {
            new Configurator().save(f);
        } catch (ConfiguratorException ce) {
            fail("Configurator.save() exception: " + ce.getMessage());
        }
        
        assertTrue(exists(f));
    }
}
