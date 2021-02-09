/*
 * Copyright (c) 2002-2007 Sun Microsystems, Inc.  All rights reserved.
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.*;
import java.util.*;

import org.junit.Test;

/**
 *
 * @author mike
 */
public class SeedingManagerTest{

	@Test
	public void testPermanentSeeds() {
		URI seeduris[] = { 
				URI.create("tcp://1.2.3.4:1234"), 
				URI.create("tcp://4.3.2.1:4321"), 
				URI.create("http://1.2.3.4:1234"),
				URI.create("http://4.3.2.1:1234")
		};

		URISeedingManager seeder = new URISeedingManager(null, true, null, null );

		seeder.addSeed(seeduris[0]);

		URI seeds[] = seeder.getActiveSeedURIs();

		assertTrue("Incorrect number of seeds returned.", seeds.length == 1);
		assertTrue("Seed not as expected.", seeds[0].equals(seeduris[0]));

		for (URI aSeed : Arrays.asList(seeduris)) {
			seeder.addSeed(aSeed);
		}

		seeds = seeder.getActiveSeedURIs();

		assertTrue("Incorrect number of seeds returned.", seeds.length == seeduris.length);
		for (URI aSeed : Arrays.asList(seeduris)) {
			if (!Arrays.asList(seeduris).contains(aSeed)) {
				fail("Missing permanent seed");
			}
		}
	}

	@SuppressWarnings("unused")
	@Test
	public void testSeeding() {
		URI seedinguris[] = {
				URI.create("file:///home/mike/rendezvous.xml"),
				URI.create("ftp://ftp.duigou.org/jxta/rendezvous.txt")
		};

		URISeedingManager seeder = new URISeedingManager(null, true, null, null);

		seeder.addSeedingURI(seedinguris[0]);

		URI seeds[] = seeder.getActiveSeedURIs();

		seeder.addSeedingURI(seedinguris[1]);

		seeds = seeder.getActiveSeedURIs();
	}
}
