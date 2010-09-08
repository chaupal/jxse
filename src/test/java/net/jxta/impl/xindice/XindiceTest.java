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

package net.jxta.impl.xindice;


import java.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.jxta.impl.xindice.core.data.Key;
import net.jxta.impl.xindice.core.data.Value;
import net.jxta.impl.xindice.core.DBException;
import net.jxta.impl.xindice.core.filer.BTreeFiler;



public class XindiceTest extends TestCase {

    private static final int ITERATIONS = 4096;
    private static final int MAX_VALUE_SIZE = 4096;

    private BTreeFiler filer = null;
    private Random random = new Random();
    
    public XindiceTest(java.lang.String testName) {
        super(testName);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
        
        System.err.flush();
        System.out.flush();
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(XindiceTest.class);

        return suite;
    }
    
    @Override
    public void setUp() {
        filer = new BTreeFiler();
        filer.setLocation(".", getName() + "-db");
        try {
            if (!filer.open()) {
                filer.create();
            }
        } catch (DBException ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }

    @Override
    public void tearDown() {
        try {
            filer.close();
            // filer.drop();
        } catch (DBException ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        filer = null;
        System.gc();
    }

    // Add test methods here, they have to start with 'test' name.
    // for example:
    // public void testHello() {}

    public void testRecords() {
        Key key = new Key("a key");
        Value value = new Value("a value");

        try {
            assertTrue("cannot open db", filer.open());
            assertTrue("recordCount == 0", filer.getRecordCount() == 0);
            filer.writeRecord(key, value);
            assertTrue("recordCount == 1", filer.getRecordCount() == 1);
            Value rval = filer.readRecord(key).getValue();

            assertTrue("val != rval", value.equals(rval));
            assertTrue("delete record", filer.deleteRecord(key));
            assertTrue("delete deleted record", !filer.deleteRecord(key));
            assertTrue("recordCount == 0", filer.getRecordCount() == 0);
        } catch (DBException ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }

    public void testStress() {
        Key[] keys = new Key[ITERATIONS];
        Value[] values = new Value[ITERATIONS];

        for (int i = 0; i < ITERATIONS; i++) {
            keys[i] = new Key("k" + Integer.toString(i));

            int valsize = random.nextInt(MAX_VALUE_SIZE);
            byte[] val = new byte[valsize];

            random.nextBytes(val);
            values[i] = new Value(val);
        }

        try {
            assertTrue("cannot open db", filer.open());
            assertTrue("recordCount == 0", filer.getRecordCount() == 0);

            for (int i = 0; i < ITERATIONS; i++) {
                filer.writeRecord(keys[i], values[i]);
            }

            filer.flush();
            assertTrue("recordCount == " + ITERATIONS, filer.getRecordCount() == ITERATIONS);

            for (int i = 0; i < ITERATIONS; i++) {
                Value rval = filer.readRecord(keys[i]).getValue();

                assertTrue("val != rval", values[i].equals(rval));

                // enable the following for some thorough testing, but
                // be warned, it can take a *really* long time

                /*
                 for (int j=0; j < ITERATIONS; j++) {
                 if (j == i) {
                 continue;
                 }
                 Value xval = filer.readRecord(keys[j]).getValue();
                 assertFalse("val == xval", values[i].equals(xval));
                 }
                 */
            }

            for (int i = 0; i < ITERATIONS; i++) {
                assertTrue("delete record", filer.deleteRecord(keys[i]));
            }

            filer.flush();
            assertTrue("recordCount == 0", filer.getRecordCount() == 0);
        } catch (DBException ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }

    public void testBenchmark() {
        Key[] keys = new Key[ITERATIONS];

        for (int i = 0; i < ITERATIONS; i++) {
            keys[i] = new Key("k" + Integer.toString(i));
        }

        try {
            assertTrue("cannot open db", filer.open());
            assertTrue("recordCount == 0", filer.getRecordCount() == 0);

            long size = 0;
            long start = System.currentTimeMillis();

            for (int i = 0; i < ITERATIONS; i++) {
                int valsize = random.nextInt(MAX_VALUE_SIZE);

                size += valsize;
                byte[] val = new byte[valsize];

                random.nextBytes(val);
                Value value = new Value(val);

                filer.writeRecord(keys[i], value);
            }
            long end = System.currentTimeMillis();

            System.out.println();
            System.out.println(
                    "WRITE: " + ITERATIONS + " records in " + (end - start) + " ms (" + ((1000 * ITERATIONS) / (end - start))
                    + " records/s " + (((1000 * size) / (end - start)) / 1024) + " kB/s)");

            filer.flush();
            assertTrue("recordCount == " + ITERATIONS, filer.getRecordCount() == ITERATIONS);

            size = 0;
            start = System.currentTimeMillis();
            for (int i = 0; i < ITERATIONS; i++) {
                Value rval = filer.readRecord(keys[i]).getValue();

                size += rval.getLength();
            }
            end = System.currentTimeMillis();
            System.out.println(
                    "READ: " + ITERATIONS + " records in " + (end - start) + " ms (" + ((1000 * ITERATIONS) / (end - start))
                    + " records/s " + (((1000 * size) / (end - start)) / 1024) + " kB/s)");

            start = System.currentTimeMillis();
            for (int i = 0; i < ITERATIONS; i++) {
                assertTrue("delete record", filer.deleteRecord(keys[i]));
            }
            end = System.currentTimeMillis();
            System.out.println(
                    "DELETE: " + ITERATIONS + " records in " + (end - start) + " ms (" + ((1000 * ITERATIONS) / (end - start))
                    + " records/s)");

            filer.flush();
            assertTrue("recordCount == 0", filer.getRecordCount() == 0);
        } catch (DBException ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }
}
