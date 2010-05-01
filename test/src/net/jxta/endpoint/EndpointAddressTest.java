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

package net.jxta.endpoint;


import net.jxta.endpoint.EndpointAddress;

import junit.framework.*;


public class EndpointAddressTest extends TestCase {
    
    public EndpointAddressTest(java.lang.String testName) {
        super(testName);
    }
    
    public void testParseURL() {
        String[] goodtests = {
            "foo://happy", "foo://happy/", "foo://happy/birthday", "foo://happy/birthday/", "foo://happy/birthday/2u"
                    ,
            "urn:foo:uuid-5", "urn:foo:uuid-5#", "urn:foo:uuid-5#3", "urn:foo:uuid-5#3/", "urn:foo:uuid-5#3/2"
        };
        
        String[] badtests = {
            "urn:whatever:happy?sdf/sdf", // slash in address portion.
            "xyz:jsdf/sdf/sdf", // bad uri
            "jxta://", // null address
            "jxta:///" // empty address
        };
        
        for (int eachGood = 0; eachGood < goodtests.length; eachGood++) {
            try {
                EndpointAddress check = new EndpointAddress(goodtests[eachGood]);

                assertEquals(check.toString(), goodtests[eachGood]);
            } catch (Throwable failed) {
                failed.printStackTrace();
                fail("Failed on '" + goodtests[eachGood] + "'");
            }
        }
        
        for (int eachBad = 0; eachBad < badtests.length; eachBad++) {
            try {
                EndpointAddress check = new EndpointAddress(badtests[eachBad]);
            } catch (Throwable failed) {
                continue; // they should fail
            }
            fail("Failed on '" + badtests[eachBad] + "'");
        }
        
    }
    
    public void testEquals() {
        EndpointAddress first = new EndpointAddress("jxta", "test", "service", "param");
        EndpointAddress second = new EndpointAddress("urn:jxta", "test", "service", "param");
        EndpointAddress third = new EndpointAddress("jxta", "test", "service", null);
        EndpointAddress fourth = new EndpointAddress("jxta", "test", null, null);
        EndpointAddress fifth = new EndpointAddress("jxta://test/service/param");
        EndpointAddress sixth = new EndpointAddress("urn:jxta:test#service/param");
        
        assertTrue("should have been equal", first.equals(first));
        
        assertTrue("should have been equal", second.equals(second));
        
        assertTrue("should have been equal", third.equals(third));
        
        assertTrue("should have been equal", fourth.equals(fourth));
        
        assertTrue("should have been equal", fifth.equals(fifth));
        
        assertTrue("should have been equal", sixth.equals(sixth));
        
        assertTrue("should have been equal", first.equals(second));
        
        assertTrue("should not have been equal", !first.equals(third));
        
        assertTrue("should not have been equal", !first.equals(fourth));
        
        assertTrue("should have been equal", first.equals(fifth));
        
        assertTrue("should have been equal", first.equals(sixth));
        
        assertTrue("should have been equal", second.equals(sixth));
        
        assertTrue("should have been equal", second.equals(first));
        
        assertTrue("should not have been equal", !third.equals(first));
        
        assertTrue("should not have been equal", !fourth.equals(first));
        
        assertTrue("should have been equal", fifth.equals(first));
        
        assertTrue("should have been equal", sixth.equals(first));
        
        assertTrue("should have been equal", sixth.equals(second));
    }
    
    public void testEqualsIdentity() {
        EndpointAddress first = new EndpointAddress("jxta://test");
        EndpointAddress second = new EndpointAddress("jxta://test");
        
        assertTrue("should have been equal", first.equals(first));
        
        assertTrue("should have been equal", first.equals(second));
        
        assertTrue("should have been equal", second.equals(first));
        
        assertTrue("should have been equal", second.equals(second));
    }
    
    public void testNotEquals() {
        EndpointAddress first = new EndpointAddress("jxta://test");
        EndpointAddress second = new EndpointAddress("jxta://test/");
        EndpointAddress third = new EndpointAddress("jxta://test/service/");
        EndpointAddress fourth = new EndpointAddress("jxta://test/service");
    
        assertTrue("should have been equal", first.equals(first));
    
        assertTrue("should not have been equal", !first.equals(second));
    
        assertTrue("should not have been equal", !first.equals(third));
    
        assertTrue("should not have been equal", !first.equals(fourth));
    
        assertTrue("should not have been equal", !second.equals(third));
    
        assertTrue("should not have been equal", !second.equals(fourth));
    
        assertTrue("should not have been equal", !third.equals(fourth));
    }
    
    public void testToString() {
        EndpointAddress first = new EndpointAddress("jxta", "test", "service", "param");
        
        assertNotNull("should have had a value", first.toString());
        
        EndpointAddress second = new EndpointAddress("jxta", "test", null, null);
        
        assertNotNull("should have had a value", second.toString());
        
        EndpointAddress third = new EndpointAddress("jxta", "test", "service", null);
        
        assertNotNull("should have had a value", third.toString());
    }
    
    public void testParse() {
        String atest = "tcp://192.18.37.36:9701/EndpointService:uuid-E91967CEE3E54E9F97F6A8732F0FA38902/PeerView/uuid-E91967CEE3E54E9F97F6A8732F0FA38902";
        
        EndpointAddress ea = new EndpointAddress(atest);
        
        assertTrue("wrong protocol name", "tcp".equals(ea.getProtocolName()));
        
        assertTrue("wrong protocol address", "192.18.37.36:9701".equals(ea.getProtocolAddress()));
        
        assertTrue("wrong service name", "EndpointService:uuid-E91967CEE3E54E9F97F6A8732F0FA38902".equals(ea.getServiceName()));
        
        assertTrue("wrong service param", "PeerView/uuid-E91967CEE3E54E9F97F6A8732F0FA38902".equals(ea.getServiceParameter()));
    }
    
    public void testEqualsSynonyms() {
        EndpointAddress first = new EndpointAddress("jxta://uuid-E91967CEE3E54E9F97F6A8732F0FA38902");
        EndpointAddress second = new EndpointAddress("urn:jxta:uuid-E91967CEE3E54E9F97F6A8732F0FA38902");
        
        assertTrue("should have been equal", first.equals(first));
        
        assertTrue("should have been equal", first.equals(second));
        
        assertTrue("should have been equal", second.equals(first));
        
        assertTrue("should have been equal", second.equals(second));
        
        EndpointAddress third = new EndpointAddress(second.toString());
        
        assertTrue("should have been equal", third.equals(first));
        assertTrue("should have been equal", third.equals(second));
        assertTrue("should have been equal", third.equals(third));
        
        assertTrue("should have been equal", first.equals(third));
        assertTrue("should have been equal", second.equals(third));
       
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(EndpointAddressTest.class);

        return suite;
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
        
        System.out.flush();
        System.err.flush();
    }
}
