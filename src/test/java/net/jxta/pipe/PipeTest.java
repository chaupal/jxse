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

package net.jxta.pipe;


import java.net.URI;
import java.util.Collections;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Messenger;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.peergroup.WorldPeerGroupFactory;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.peer.PeerID;

import net.jxta.impl.endpoint.tls.TlsTransport;
import org.junit.Ignore;

@Ignore("JXTA Configurator required")
public class PipeTest extends TestCase {
    
    static final String pipeid = "urn:jxta:uuid-59616261646162614E504720503250330171AB9DD280488AA6429589D17FC95404";
    
    static final String peerid = "urn:jxta:uuid-59616261646162614E504720503250330171AB9DD280488AA6429589D17FC95403";
    
    static PeerGroup pg;
    
    public PipeTest(java.lang.String testName) throws Exception {
        super(testName);
        
        System.setProperty("net.jxta.tls.password", "password");
        System.setProperty("net.jxta.tls.principal", "password");
        
        synchronized (PipeTest.class) {
            if (null == pg) {
                pg = PeerGroupFactory.newNetPeerGroup(PeerGroupFactory.newPlatform());
            }
        }
    }
    
    @Override
    protected void finalize() {
        
        synchronized (PipeTest.class) {
            if (null != pg) {
                pg.stopApp();
                pg.unref();
                pg = null;
            }
        }
    }
    
    public static void main(java.lang.String[] args) {
        try {
            TestRunner.run(suite());
        } finally {
            synchronized (PipeTest.class) {
                if (null != pg) {
                    pg.stopApp();
                    pg.unref();
                    pg = null;
                }
            }
            
            System.err.flush();
            System.out.flush();
        }
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(PipeTest.class);

        return suite;
    }
    
    public void testLocalResolution() {
        try {
            PipeID pipeID = PipeID.create(URI.create(pipeid));
            
            PipeAdvertisement uniPipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(
                    PipeAdvertisement.getAdvertisementType());

            uniPipeAdv.setName("Test Unicast");
            uniPipeAdv.setPipeID(pipeID);
            uniPipeAdv.setType(PipeService.UnicastType);
            
            PipeAdvertisement secPipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(
                    PipeAdvertisement.getAdvertisementType());

            secPipeAdv.setName("Test Secure");
            secPipeAdv.setPipeID(pipeID);
            secPipeAdv.setType(PipeService.UnicastSecureType);
            
            PipeService ps = pg.getPipeService();
            
            InputPipe ip = ps.createInputPipe(secPipeAdv);
            
            OutputPipe op = null;

            try {
                op = ps.createOutputPipe(uniPipeAdv, 10000);
                if (null == op) {
                    fail("null is not a valid result");
                }
            } catch (IOException failed) {
                ;
            }
            
            assertTrue("output pipe should not be resolved (wrong type)", op == null);
            
            op = ps.createOutputPipe(secPipeAdv, 10000);
            
            assertTrue("output pipe should be resolved", op != null);
            
            ip.close();
            op.close();
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
    
    public void testTlsFiltering() {
        
        try {
            PipeID pipeID = PipeID.create(URI.create(pipeid));
            
            PipeAdvertisement uniPipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(
                    PipeAdvertisement.getAdvertisementType());

            uniPipeAdv.setName("Test Unicast");
            uniPipeAdv.setPipeID(pipeID);
            uniPipeAdv.setType(PipeService.UnicastType);
            
            PipeAdvertisement secPipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(
                    PipeAdvertisement.getAdvertisementType());

            secPipeAdv.setName("Test Secure");
            secPipeAdv.setPipeID(pipeID);
            secPipeAdv.setType(PipeService.UnicastSecureType);
            
            PipeService ps = pg.getPipeService();
            
            InputPipe ip = ps.createInputPipe(secPipeAdv);
            
            OutputPipe op = null;
            
            op = ps.createOutputPipe(secPipeAdv, 10000);
            
            assertTrue("output pipe should be resolved", op != null);
            
            Message realmsg = new Message();
            
            op.send(realmsg);
            
            Thread.sleep(1000);
            
            assertTrue("should be a message", realmsg == ip.poll(1));
            
            EndpointAddress dest = mkAddress(pg.getPeerID().toString(), pipeid);
            
            Messenger direct = pg.getEndpointService().getMessenger(dest);
            
            Message fakemsg = new Message();
            
            direct.sendMessage(fakemsg);
            
            Thread.sleep(1000);
            
            assertTrue("shouldnt be a message", null == ip.poll(1));
            
            Message simmsg = new Message();

            simmsg.setMessageProperty(TlsTransport.class, this);
            
            direct.sendMessage(simmsg);
            
            Thread.sleep(1000);
            
            assertTrue("should be a message", simmsg == ip.poll(1000));
            
            ip.close();
            op.close();
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
    
    public void testOutputResolution() {
        
        try {
            PipeID pipeID = PipeID.create(URI.create(pipeid));
            PeerID peerID = PeerID.create(URI.create(peerid));
            
            PipeAdvertisement uniPipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(
                    PipeAdvertisement.getAdvertisementType());

            uniPipeAdv.setName("Test Unicast");
            uniPipeAdv.setPipeID(pipeID);
            uniPipeAdv.setType(PipeService.UnicastType);
            
            PipeAdvertisement secPipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(
                    PipeAdvertisement.getAdvertisementType());

            secPipeAdv.setName("Test Secure");
            secPipeAdv.setPipeID(pipeID);
            secPipeAdv.setType(PipeService.UnicastSecureType);
            
            PipeService ps = pg.getPipeService();
            
            InputPipe ip = ps.createInputPipe(secPipeAdv);
            
            OutputPipe op = null;
            
            try {
                op = ps.createOutputPipe(secPipeAdv, Collections.singleton(peerID), 10000);
                
                if (null == op) {
                    fail("null is not a valid result");
                }
            } catch (Exception ignored) {
                ;
            }
            
            assertTrue("output pipe should not be resolved", op == null);
            
            op = ps.createOutputPipe(secPipeAdv, 10000);
            
            assertTrue("output pipe should be resolved", op != null);
            
            op.close();
            op = null;
            
            op = ps.createOutputPipe(secPipeAdv, Collections.singleton(pg.getPeerID()), 10000);
            
            assertTrue("output pipe should be resolved", op != null);
            
            ip.close();
            op.close();
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
    
    public void testInputClose() {
        
        try {
            PipeID pipeID = PipeID.create(URI.create(pipeid));
            
            PipeAdvertisement uniPipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(
                    PipeAdvertisement.getAdvertisementType());

            uniPipeAdv.setName("Test Unicast");
            uniPipeAdv.setPipeID(pipeID);
            uniPipeAdv.setType(PipeService.UnicastType);
           
            PipeService ps = pg.getPipeService();
            
            InputPipe ip = ps.createInputPipe(uniPipeAdv);
            
            assertTrue("Shouldn't be a message", (null == ip.poll(1)));
            
            ip.close();
             
            assertTrue("Shouldn't be a message", (null == ip.poll(1)));
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
            
    /**
     *  Convenience method for constructing an endpoint address from an id
     *
     *  @param destPeer peer id
     *  @param serv the service name (if any)
     *  @param parm the service param (if any)
     *  @param endpointAddress for this peer id.
     **/
    private static EndpointAddress mkAddress(String destPeer, String pipeID) {
        
        ID asID = ID.create(URI.create(destPeer));
        
        return mkAddress(asID, pipeID);
    }
    
    /**
     *  Convenience method for constructing an endpoint address from an id
     *
     *  @param destPeer peer id
     *  @param parm the service param (if any)
     *  @return endpointAddress for this peer id.
     **/
    protected static EndpointAddress mkAddress(ID destPeer, String paramID) {
        
        EndpointAddress addr = new EndpointAddress("jxta", destPeer.getUniqueValue().toString(), "PipeService", paramID);
        
        return addr;
    }
}
