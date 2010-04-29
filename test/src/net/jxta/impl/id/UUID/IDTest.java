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

package net.jxta.impl.id.UUID;

import java.net.URI;
import java.security.MessageDigest;
import java.security.ProviderException;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.jxta.codat.CodatID;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;

/**
 * @author  mike
 */
public final class IDTest extends TestCase {
    
    /** Creates new DocTest */
    public IDTest(String name) {
        super(name);
    }
    
    public void testCodatID() {
        try {
            PeerGroupID seedGroup = IDFactory.newPeerGroupID("uuid");
            CodatID first = IDFactory.newCodatID(seedGroup);
            CodatID second = IDFactory.newCodatID(seedGroup);
            CodatID third;
            ID interloper = IDFactory.newPeerID(IDFactory.newPeerGroupID("uuid"));
            String  asString;
            ID myPeerGroup;
            boolean isStatic;
            
            assertTrue("comparison of a CodatID against itself failed", first.equals(first));
            
            assertTrue("comparison of two different CodatIDs should have failed", !first.equals(second));
            
            assertTrue("comparison of different types should have failed", !first.equals(interloper));
            
            assertTrue("comparison of different types should have failed", !interloper.equals(first));
            
            assertTrue("zero hashcodereturned", 0 != first.hashCode());
            
            URI asURI = first.toURI();

            asString = first.toString();
            
            assertTrue("comparison of ID string and string of URI was not the same", asString.equals(asURI.toString()));
            
            third = (CodatID) IDFactory.fromURI(asURI);
            
            assertTrue("result of conversion to URI and back to ID was not equal to original", first.equals(third));
            
            myPeerGroup = first.getPeerGroupID();
            
            assertTrue("clone of ID is not of same peergroup.", first.getPeerGroupID().equals(third.getPeerGroupID()));
            
            assertTrue("dynamic CodatID did not test as such.", !first.isStatic());
            
            asURI = first.toURI();
            
            third = (CodatID) IDFactory.fromURI(asURI);
            
            assertTrue("result of conversion to URI and back to ID was not equal to original", first.equals(third));
        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
            
        }
        
    }
    
    public void testPeerGroupID() {
        try {
            byte[] seed = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
            PeerGroupID first = IDFactory.newPeerGroupID("uuid");
            PeerGroupID second = IDFactory.newPeerGroupID("uuid");
            PeerGroupID third = IDFactory.newPeerGroupID(second);
            PeerGroupID fourth;
            PeerGroupID fifth = IDFactory.newPeerGroupID(seed);
            PeerGroupID sixth = IDFactory.newPeerGroupID(fifth, seed);
            ID interloper = IDFactory.newPeerID(IDFactory.newPeerGroupID("uuid"));
            String  asString;
            URI     asURI;
            ID myPeerGroup;
            boolean isStatic;
            
            assertTrue("comparison of a PeerGroupID against itself failed", first.equals(first));
            
            assertTrue("comparison against worldPeerGroup should have failed", !first.equals(PeerGroupID.worldPeerGroupID));
            
            assertTrue("comparison of two different PeerGroupIDs should have failed", !first.equals(second));
            
            assertTrue("comparison of different types should have failed", !first.equals(interloper));
            
            assertTrue("comparison of different types should have failed", !interloper.equals(first));
            
            assertTrue("zero hashcodereturned", 0 != first.hashCode());
            
            assertTrue("hashcode for world group should not have matched."
                    ,
                    PeerGroupID.worldPeerGroupID.hashCode() != first.hashCode());
            
            asURI = first.toURI();
            asString = first.toString();
            
            assertTrue("comparison of ID string and string of URI was not the same", asString.equals(asURI.toString()));
            
            fourth = (PeerGroupID) IDFactory.fromURI(asURI);
            
            assertTrue("result of conversion to URI and back to ID was not equal to original", first.equals(fourth));
            
            Object fromFirst = first.getUniqueValue();
            Object fromSecond = second.getUniqueValue();
            
            assertTrue("comparison of UUIDs from an ID and a clone failed", fromFirst.equals(fourth.getUniqueValue()));
            
            assertTrue("comparison of UUIDs from an ID and a different ID should have failed."
                    ,
                    !fromFirst.equals(second.getUniqueValue()));
            
            assertTrue("simple group shouldnt have had a parent", null == first.getParentPeerGroupID());
            
            assertTrue("parent group didnt match expected.", third.getParentPeerGroupID().equals(second));
            
            third = IDFactory.newPeerGroupID(PeerGroupID.worldPeerGroupID);
            
            assertTrue("parent group wasnt world group", third.getParentPeerGroupID().equals(PeerGroupID.worldPeerGroupID));
            
            assertTrue("parent didnt match", fifth.equals(sixth.getParentPeerGroupID()));
            
            asURI = first.toURI();
            
            third = (PeerGroupID) IDFactory.fromURI(asURI);
            
            assertTrue("result of conversion to URI and back to ID was not equal to original", first.equals(third));
        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
        }
        
    }
    
    public void testPeerID() {
        try {
            PeerID first = IDFactory.newPeerID(IDFactory.newPeerGroupID("uuid"));
            PeerID second = IDFactory.newPeerID(IDFactory.newPeerGroupID("uuid"));
            PeerID third;
            ID interloper = IDFactory.newPeerGroupID("uuid");
            String  asString;
            URI     asURI;
            ID myPeerGroup;
            boolean isStatic;
            
            assertTrue("comparison of a PeerID against itself failed", first.equals(first));
            
            assertTrue("comparison of two different PeerIDs should have failed", !first.equals(second));
            
            assertTrue("comparison of different types should have failed", !first.equals(interloper));
            
            assertTrue("comparison of different types should have failed", !interloper.equals(first));
            
            assertTrue("zero hashcodereturned", 0 != first.hashCode());
            
            asURI = first.toURI();
            asString = first.toString();
            
            assertTrue("comparison of ID string and string of URI was not the same", asString.equals(asURI.toString()));
            
            third = (PeerID) IDFactory.fromURI(asURI);
            
            assertTrue("result of conversion to URI and back to ID was not equal to original", first.equals(third));
            
            assertTrue("clone of ID is not of same peergroup.", first.getPeerGroupID().equals(third.getPeerGroupID()));
            
            asURI = first.toURI();
            
            third = (PeerID) IDFactory.fromURI(asURI);
            
            assertTrue("result of conversion to URI and back to ID was not equal to original", first.equals(third));
        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
        }        
    }
    
    public void testSeededPipeID() {
        MessageDigest dig = null;

        try {
            dig = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException caught) {
            dig = null;
        }
        
        if (dig == null) {
            throw new ProviderException("SHA-1 digest algorithm not found");
        }
        
        dig.reset();
        
        String pipeSeed = "JXTA_PIPE for user #012494345676";
        
        // Must use UTF-8 because platform encoding does vary.
        try {
            dig.update(pipeSeed.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException never) {
            // UTF-8 is builtin
            ;
        }

        byte[] result = dig.digest();
        
        PipeID first = IDFactory.newPipeID(IDFactory.newPeerGroupID("uuid"), result);
    }
    
    public void testPipeID() {
        try {
            PipeID first = IDFactory.newPipeID(IDFactory.newPeerGroupID("uuid"));
            PipeID second = IDFactory.newPipeID(IDFactory.newPeerGroupID("uuid"));
            PipeID third;
            ID interloper = IDFactory.newPeerGroupID("uuid");
            String  asString;
            URI     asURI;
            ID myPeerGroup;
            boolean isStatic;
            
            assertTrue("comparison of a PipeID against itself failed", first.equals(first));
            
            assertTrue("comparison of two different PipeIDs should have failed", !first.equals(second));
            
            assertTrue("comparison of different types should have failed", !first.equals(interloper));
            
            assertTrue("comparison of different types should have failed", !interloper.equals(first));
            
            assertTrue("zero hashcode returned", 0 != first.hashCode());
            
            asURI = first.toURI();
            asString = first.toString();
            
            assertTrue("comparison of ID string and string of URI was not the same", asString.equals(asURI.toString()));
            
            third = (PipeID) IDFactory.fromURI(asURI);
            
            assertTrue("result of conversion to URI and back to ID was not equal to original", first.equals(third));
            
            assertTrue("clone of ID is not of same peergroup.", first.getPeerGroupID().equals(third.getPeerGroupID()));
            
            asURI = first.toURI();
            
            third = (PipeID) IDFactory.fromURI(asURI);
            
            assertTrue("result of conversion to URI and back to ID was not equal to original", first.equals(third));
        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
        }
    }
    
    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
        System.err.flush();
        System.out.flush();
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(IDTest.class);

        return suite;
    }
}
