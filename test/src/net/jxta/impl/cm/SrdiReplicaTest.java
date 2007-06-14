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

package net.jxta.impl.cm;


import java.math.BigInteger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.jxta.impl.util.JxtaHash;


/**
 *  A SrdiGetReplica unit test
 */
public class SrdiReplicaTest extends TestCase {

    private JxtaHash jxtaHash = new JxtaHash();
    private static final int OFFBY = 30;
    private static final int ITERATIONS = 10;
    private static final int SIZE = 100;
    private static final String TSTSTR = "This is only a Test, nothing else";

    /**
     *Constructor for the SrdiIndexTest object
     *
     * @param  testName  test name
     */
    public SrdiReplicaTest(String testName) {
        super(testName);
    }

    /**
     *  A unit test suite for JUnit
     *
     * @return    The test suite
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(SrdiReplicaTest.class);

        return suite;
    }

    /**
     *  The JUnit setup method
     */
    @Override
    protected void setUp() {}
    
    @Override
    public void tearDown() {
        System.gc();
    }

    /**
     *  The main program to test CmCache
     *
     *@param  argv           The command line arguments
     *@exception  Exception  Description of Exception
     */
    public static void main(String[] argv) throws Exception {
        
        junit.textui.TestRunner.run(suite());
        System.err.flush();
        System.out.flush();
    }

    public void testOffBy() {
        for (int i = 0; i < OFFBY; i++) {
            offBy(i);
        }
    }
    
    private void offBy(int size) {
        System.out.println("Test Off By : " + size);
        System.out.println("---------------");
        
        for (int i = 0; i < ITERATIONS; i++) {
            int rp1 = getReplica1(TSTSTR + i, SIZE);
            int rp2 = getReplica2(TSTSTR + i, SIZE);
            int rp11 = getReplica1(TSTSTR + i, SIZE + size);
            int rp21 = getReplica2(TSTSTR + i, SIZE + size);

            System.out.print("Get replica 1 actual pos: " + rp1 + " RPV Off by " + size + " : " + rp11 + " skew :" + (rp1 - rp11));
            System.out.println(
                    " ----   Get replica 2 actual pos: " + rp2 + " Off by " + size + " : " + rp21 + " skew :" + (rp2 - rp21));
        }
    }
    
    /* 2.0 Replica Function */
    private int getReplica1(String expression, int size) {
        jxtaHash.update(expression);
        return jxtaHash.mod(size);
    }

    /* Replica Function  contributed by Shinya*/
    private int getReplica2(String expression, int size) {
        jxtaHash.update(expression);
        BigInteger sizeOfPeerView = java.math.BigInteger.valueOf(size);
        BigInteger digest = jxtaHash.getDigestInteger().abs();
        BigInteger sizeOfHashSpace = BigInteger.ONE.shiftLeft(8 * digest.toByteArray().length);

        return (digest.multiply(sizeOfPeerView)).divide(sizeOfHashSpace).intValue();
    }

}
