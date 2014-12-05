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
package net.jxta.impl.endpoint.msgframing;


import java.io.*;

import junit.framework.*;

import net.jxta.impl.endpoint.msgframing.WelcomeMessage;


public class WelcomeMessageTest extends TestCase {
    
    final static String valid11 = "JXTAHELLO tcp://64.81.53.91:36815 tcp://209.128.126.120:9701 urn:jxta:uuid-59616 261646162614A787461503250333C85E78DB99A4BDD837FD8A233CAD3D803 0 1.1";
    final static String valid20 = "JXTAHELLO tcp://64.81.53.91:36815 tcp://209.128.126.120:9701 urn:jxta:uuid-59616 261646162614A787461503250333C85E78DB99A4BDD837FD8A233CAD3D803 0 2.0";
    
    public WelcomeMessageTest(java.lang.String testName) {
        super(testName);
    }
    
    public void testGarbage() {
        String atest = "Lorem Ipsum Dolor";
        
        try { 
            WelcomeMessage welcome = new WelcomeMessage(new ByteArrayInputStream(atest.getBytes("UTF-8")));
            
            fail("Should have failed to accept input as a Welcome Message");
        } catch (IOException failed) {
            ;
        } catch (Throwable unexpected) {
            fail("An unexpected exception occurred.");
        }
    }
    
    public void testTruncated() {
        String atest = valid11.substring(0, 20);
        
        try { 
            WelcomeMessage welcome = new WelcomeMessage(new ByteArrayInputStream(atest.getBytes("UTF-8")));
            
            fail("Should have failed to accept input as a Welcome Message");
        } catch (IOException failed) {
            ;
        } catch (Throwable unexpected) {
            fail("An unexpected exception occurred.");
        }
    }
    
    public void testVersionMissing() {
        String atest = valid11.substring(0, valid11.lastIndexOf(' '));
        
        try { 
            WelcomeMessage welcome = new WelcomeMessage(new ByteArrayInputStream(atest.getBytes("UTF-8")));
            
            fail("Should have failed to accept input as a Welcome Message");
        } catch (IOException failed) {
            ;
        } catch (Throwable unexpected) {
            fail("An unexpected exception occurred.");
        }
    }
    
    public void testValid11() {
        
        try { 
            WelcomeMessage welcome = new WelcomeMessage(new ByteArrayInputStream(valid11.getBytes("UTF-8")));
            
            fail("Should have failed to accept input as a Welcome Message");
        } catch (IOException failed) {
            ;
        } catch (Throwable unexpected) {
            fail("An unexpected exception occurred.");
        }
    }
    
    public void testValid20() {
        
        try { 
            WelcomeMessage welcome = new WelcomeMessage(new ByteArrayInputStream(valid20.getBytes("UTF-8")));
            
            fail("Should have failed to accept input as a Welcome Message");
        } catch (IOException failed) {
            ;
        } catch (Throwable unexpected) {
            fail("An unexpected exception occurred.");
        }
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(WelcomeMessageTest.class);

        return suite;
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
        
        System.out.flush();
        System.err.flush();
    }
}
