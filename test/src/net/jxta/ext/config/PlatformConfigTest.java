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

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;

import net.jxta.impl.protocol.PlatformConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;


/**
 *
 * @version $Id: PlatformConfigTest.java,v 1.3 2004/03/22 20:20:36 gonzo Exp $
 *
 * @author james todd [gonzo at jxta dot org]
 */

public class PlatformConfigTest extends TestBase {

    private PlatformConfig pc = null;
    private static final String PLATFORM_CONFIG_CLASSIC = "/net/jxta/ext/config/resources/PlatformConfig.classic.xml";

    public static void main(String[] argv) {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.TestSuite suite() {
        junit.framework.TestSuite suite = new junit.framework.TestSuite();

        suite.addTest(new junit.framework.TestSuite(PlatformConfigTest.class));

        return suite;
    }

    public PlatformConfigTest() {
        this(PlatformConfigTest.class.getName());
    }

    public PlatformConfigTest(String name) {
        super(name);

        try {
            this.pc = new Configurator(NAME, DESCRIPTION, PRINCIPAL, PASSWORD).getPlatformConfig();
        } catch (ConfiguratorException ce) {}
    }

    public void testName() {
        if (this.pc == null) {
            fail("null PlatformConfig");
        }

        Configurator c = new Configurator(this.pc);

        assertEquals(NAME, c.getName());
    }

    public void testClassic() {
        PlatformConfig pc = null;
        InputStream is = PlatformConfigTest.class.getResourceAsStream(PLATFORM_CONFIG_CLASSIC);

        if (is != null) {
            try {
                pc = (PlatformConfig)
                        AdvertisementFactory.newAdvertisement(MimeMediaType.XMLUTF8, new BufferedReader(new InputStreamReader(is)));
            } catch (IOException ioe) {
                fail("PlatformConfig construction: " + ioe.getMessage());
            }
        } else {
            fail("can't obtain classic PlatformConfig resource");
        }

        if (pc != null) {
            Configurator c = new Configurator(pc);
            
            try {
                c.getPlatformConfig();
            } catch (ConfiguratorException ce) {
                fail("can't get PlatformConfig: " + ce.getMessage());
            }
        }
    }
}
