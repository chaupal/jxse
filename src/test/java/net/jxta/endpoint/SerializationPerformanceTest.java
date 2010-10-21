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


import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.*;
import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.test.util.JUnitRuleMockery;
import net.jxta.util.DevNullOutputStream;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.platform.NetworkManager.ConfigMode;


/**
 *
 * @author mike
 */
public class SerializationPerformanceTest extends TestCase {

	@Rule
    public TemporaryFolder tempStorage = new TemporaryFolder();
    
    private NetworkManager aliceManager;

    @Before
    public void setUp() throws Exception {
        aliceManager = new NetworkManager(ConfigMode.ADHOC, "alice", tempStorage.newFolder("alice").toURI());
        configureForHttp(aliceManager, 59901);
        aliceManager.startNetwork();
    }

    private void configureForHttp(NetworkManager manager, int port) throws IOException {
            NetworkConfigurator configurator = manager.getConfigurator();
            configurator.setTcpEnabled(false);
            configurator.setHttp2Enabled(false);

            configurator.setHttpEnabled(true);
            configurator.setHttpIncoming(true);
            configurator.setHttpOutgoing(true);
            configurator.setHttpPort(port);
    }
    
    private static final MimeMediaType appMsg = new MimeMediaType("application/x-jxta-msg");
    
    public SerializationPerformanceTest(java.lang.String testName) {
        super(testName);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
        
        System.err.flush();
        System.out.flush();
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(SerializationPerformanceTest.class);

        return suite;
    }
    
    public void testSerialPerformance() {
        try {
            Message msg = new Message();

            PeerGroup group = aliceManager.getNetPeerGroup();
            
            WireFormatMessage init = WireFormatMessageFactory.toWireExternal(msg, appMsg, null, group);
            
            final int startCount = 1;
            final int endCount = 250;
            final int stride = 10;
            final int elementSize = 256;
            final int repeats = 250;
            
            for (int count = 1; count < endCount; count += stride) {
                
                System.gc();
                System.gc();
                
                MessageElement newElem = new ByteArrayMessageElement(Integer.toString(count), null, new byte[elementSize], null);
                
                long beforeAdd = System.currentTimeMillis();
                long beforeSerial = 0;
                long atEnd = 0;
                
                for (int repeat = 1; repeat <= repeats; repeat++) {
                    msg.addMessageElement(newElem);
                    msg.removeMessageElement(newElem);
                }
                
                msg.addMessageElement(newElem);
                
                if (count >= startCount) {
                    beforeSerial = System.currentTimeMillis();
                    
                    for (int repeat = 1; repeat <= repeats; repeat++) {
                        OutputStream out = new ByteArrayOutputStream();
                        
                        WireFormatMessage serialed = WireFormatMessageFactory.toWireExternal(msg, appMsg, null, group);
                        
                        serialed.sendToStream(out);
                    }
                }
                
                atEnd = System.currentTimeMillis();
                
                long addCost;
                long serialCost;
                
                if (beforeSerial != 0) {
                    addCost = beforeSerial - beforeAdd;
                    serialCost = atEnd - beforeSerial;
                } else {
                    addCost = atEnd - beforeAdd;
                    serialCost = atEnd - atEnd;
                }
                
                System.err.println(count + "," + addCost + "," + serialCost);
            }
        } catch (Throwable caught) {
            fail("exception thrown : " + caught.getMessage());
        }
    }
    
}
