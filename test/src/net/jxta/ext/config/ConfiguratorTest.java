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

import java.io.File;

import java.util.logging.Level;
import net.jxta.logging.Logging;
import java.util.logging.Logger;


/**
 *
 * @version $Id: ConfiguratorTest.java,v 1.12 2007/01/26 02:13:54 bondolo Exp $
 *
 * @author james todd [gonzo at jxta dot org]
 */

public class ConfiguratorTest extends TestBase {

    public static void main(String[] argv) {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.TestSuite suite() {
        junit.framework.TestSuite suite = new junit.framework.TestSuite();

        suite.addTest(new junit.framework.TestSuite(ConfiguratorTest.class));

        return suite;
    }

    public ConfiguratorTest() {
        this(ConfiguratorTest.class.getName());
    }

    public ConfiguratorTest(String name) {
        super(name);
    }

    public void testWrite() {
        File h = new File(HOME, getClass().getName() + ".testWrite");
        
        h.mkdirs();
        
        try {
            new Configurator(PRINCIPAL, PASSWORD).save(h);
        } catch (ConfiguratorException ce) {
            fail("Configurator.save() exception: " + ce.getMessage());
        }
        
        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }
    
    public void testWriteToFile() {
        File h = new File(HOME, getClass().getName() + ".testWriteToFile" + File.separator + Env.PLATFORM_CONFIG + ".test");
        
        try {
            new Configurator(PRINCIPAL, PASSWORD).save(h);
        } catch (ConfiguratorException ce) {
            fail("Configurator.save() exception: " + ce.getMessage());
        }
        
        assertTrue(exists(h));
    }
    
    public void testWritePrivate() {
        File h = new File(HOME, getClass().getName() + ".testWritePrivate");
        Profile p = new Profile(getClass().getResource(PROFILE_EDGE_PRIVATE));

        h.mkdirs();

        try {
            new Configurator(p).save(h);
        } catch (ConfiguratorException ce) {
            fail("Configurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
        assertTrue(exists(new File(h, Env.CONFIG_PROPERTIES)));
    }
    
    public void testOneShot() {
        try {
            setPlatformConfig(new Configurator(PRINCIPAL, PASSWORD).getPlatformConfig());
        } catch (ConfiguratorException ce) {
            fail("Configurator.save() exception: " + ce.getMessage());
        }
        
        assertNotNull(getPlatformConfig());
    }
    
    public void testNoArg() {
        Configurator c = new Configurator();

        c.setSecurity(PRINCIPAL, PASSWORD);

        try {
            setPlatformConfig(c.getPlatformConfig());
        } catch (ConfiguratorException ce) {
            fail("Configurator.save() exception: " + ce.getMessage());
        }

        assertNotNull(getPlatformConfig());
    }
    
    public void testThreeArg() {
        Configurator c = new Configurator(NAME, PRINCIPAL, PASSWORD);

        try {
            setPlatformConfig(c.getPlatformConfig());
        } catch (ConfiguratorException ce) {
            fail("Configurator.save() exception: " + ce.getMessage());
        }

        assertNotNull(getPlatformConfig());
    }
    
    // provided by John Dickerson

    public void filePathTest() {
        Configurator c = new Configurator(Profile.EDGE);
        File h = new File(HOME, getClass().getName() + ".filePathTest");
        
        h.mkdirs();

        c.setSecurity(PRINCIPAL, PASSWORD);

        try {
            c.save(h);
        } catch (ConfiguratorException ce) {
            fail("Configurator.save() exception: " + ce.getMessage());
        }

        assertTrue(exists(new File(h, Env.PLATFORM_CONFIG)));
    }

    // protected void setUp() {
    // super.setUp();
    //
    // BasicConfigurator.resetConfiguration();
    // BasicConfigurator.configure();
    // logger.debug(" ************* setUp() called");
    // System.setProperty("JXTA_HOME", "tmp");
    // System.setProperty("sun.net.client.defaultConnectTimeout", "20000");
    // System.setProperty("net.jxta.tls.password", "password");
    // }
    
}
