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
package net.jxta.document;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import junit.framework.*;


public class MimeMediaTypeTest extends TestCase {
    
	private boolean exceptionOnThread = false;
	
    private final class ExceptionCatchingThreadFactory implements ThreadFactory {
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
				
				public void uncaughtException(Thread t, Throwable exception) {
					System.err.println("Exception occurred on test thread");
					exception.printStackTrace();
					exceptionOnThread = true;
				}
			});
			
			return t;
		}
	}

	public MimeMediaTypeTest(java.lang.String testName) {
        super(testName);
    }
	
	@Override
	protected void tearDown() throws Exception {
		MimeMediaType.clearInternMap();
		exceptionOnThread = false;
	}
    
    public void testConstructors() {
        MimeMediaType plainXMLString = new MimeMediaType("text/xml");
        MimeMediaType plainXML2params = new MimeMediaType("text", "xml");
        MimeMediaType plaintext2params = new MimeMediaType("text", "plain");
        
        assertTrue(plainXMLString.equals(plainXMLString));
        assertTrue(plainXMLString.equals(plainXML2params));
        assertTrue(!plainXMLString.equals(plaintext2params));
        
        MimeMediaType utf8xmlstring = new MimeMediaType("Text/Xml;charset=UTF-8");
        MimeMediaType utf8xmlparams = new MimeMediaType("text", "xml", "cHarset=\"UTF-8\"");
        MimeMediaType utf8xmlstringcomments = new MimeMediaType("text/xml;(ha)chArset=\"UTF-8\"(ha)");
        
        assertTrue(!plainXMLString.equals(utf8xmlstring));
        assertTrue(utf8xmlstring.equals(utf8xmlparams));
        assertTrue(utf8xmlparams.equals(utf8xmlstringcomments));
        
        MimeMediaType xmlmanyparamsstring = new MimeMediaType(
                "text/xml;chArset=\"UTF-8\";encoding=\"BASE64\";synopsis=\"boring\";priority=\"junk\"");
        MimeMediaType xmlmanyparamsparams = new MimeMediaType("text", "xml"
                ,
                "Charset=\"UTF-8\";Encoding=\"BASE64\";Synopsis=\"boring\";Priority=\"junk\"");
        
        assertTrue(!plainXMLString.equals(xmlmanyparamsstring));
        assertTrue(!utf8xmlstring.equals(xmlmanyparamsstring));
        assertTrue(xmlmanyparamsstring.equals(xmlmanyparamsparams));
    }
    
    public void testObjectMethods() {
        MimeMediaType plainXMLString = new MimeMediaType("text/xml");
        
        String roundtrip = new MimeMediaType(plainXMLString.toString()).toString();
        
        assertTrue(roundtrip.equals(plainXMLString.toString()));
    }
    
    public void testAccessors() {
        String type = "text/xml";
        MimeMediaType plainXMLString = new MimeMediaType(type);
        
        assertTrue(type.equals(plainXMLString.getMimeMediaType()));
        
        assertTrue("text".equals(plainXMLString.getType()));
        
        assertTrue("xml".equals(plainXMLString.getSubtype()));
        
        assertTrue(!plainXMLString.isExperimentalType());
        assertTrue(!plainXMLString.isExperimentalSubtype());
        
        MimeMediaType experimentalXMLString = new MimeMediaType("x-text", "x-xml");
        MimeMediaType nonexperimentalXMLString = new MimeMediaType("text", "xml");
        
        assertTrue(experimentalXMLString.isExperimentalType());
        assertTrue(experimentalXMLString.isExperimentalSubtype());
        assertTrue(!nonexperimentalXMLString.isExperimentalType());
        assertTrue(!nonexperimentalXMLString.isExperimentalSubtype());
    }
    
    public void testParams() {
        MimeMediaType xmlmanyparamsstring = new MimeMediaType(
                "text/xml;chArset=\"UTF-8\";encoding=\"BASE64\";synopsis=\"boring\";priority=\"junk\"");
        
        assertTrue("UTF-8".equals(xmlmanyparamsstring.getParameter("charset")));
        assertTrue("boring".equals(xmlmanyparamsstring.getParameter("synopsis")));
        assertTrue("junk".equals(xmlmanyparamsstring.getParameter("priority")));
        assertNull(xmlmanyparamsstring.getParameter("destination"));
        
        MimeMediaType roundtrippedmany = new MimeMediaType(xmlmanyparamsstring.toString());
        
        assertTrue(roundtrippedmany.equals(xmlmanyparamsstring));
        
        MimeMediaType xmllowercharset = new MimeMediaType("text/xml;charset=\"UTF-8\"");
        MimeMediaType xmlUPPERcharset = new MimeMediaType("text/xml;CHARSET=\"UTF-8\"");
        
        assertTrue(xmllowercharset.equals(xmlUPPERcharset));
        
        MimeMediaType spaceparam = new MimeMediaType("text/xml;charset=\"x y z\"");
        
        String spacedparam = spaceparam.getParameter("charset");
        
        assertTrue(spacedparam.equals("x y z"));
        
        MimeMediaType roundtrippedspace = new MimeMediaType(spaceparam.toString());
        
        assertTrue(roundtrippedspace.equals(spaceparam));
        
        spacedparam = spaceparam.getParameter("ChArSeT");
        
        assertTrue("attributes should be case insensitive", spacedparam.equals("x y z"));
    }
    
    public void testValueOf() {
        MimeMediaType newed = new MimeMediaType("text/xml");
        MimeMediaType valued = MimeMediaType.valueOf("text/xml");
        
        assertTrue(newed.equals(valued));
        MimeMediaType newInterned = newed.intern();
        MimeMediaType valueInterned = valued.intern();
        
        assertTrue(valued == valueInterned);
        
        assertTrue(newInterned.equals(valueInterned));
        assertTrue(newInterned == valueInterned);
    }
    
    public void testInternPerformance() {
        int trials = 10000;
        String [] strings = new String[trials];
        MimeMediaType [] mimes = new MimeMediaType[trials];
        
        for(int each = 0; each < trials; each++) {
            strings[each] = new String("text/xml; charset=\"UTF-87\"");
        }
        
        long start;
        long stop;
        
        System.runFinalization();
        System.gc();
        System.gc();
        
        start = System.currentTimeMillis();
        for(int each = 0; each < trials; each++) {
            mimes[each] = new MimeMediaType(strings[each]);
        }
        stop = System.currentTimeMillis();
        System.out.println( stop - start + "ms for " + trials + " identical types.");
        
        System.runFinalization();
        System.gc();
        System.gc();
        
        start = System.currentTimeMillis();
        for(int each = 0; each < trials; each++) {
            mimes[each] = new MimeMediaType(strings[each]).intern();
        }
        stop = System.currentTimeMillis();
        System.out.println( stop - start + "ms for " + trials + " identical interned types.");
        
        Arrays.fill(strings, null);
        Arrays.fill(mimes, null);
        
        for(int each = 0; each < trials; each++) {
            strings[each] = new String("text/xml; charset=\"UTF-XX" + each + "\"");
        }
        
        System.runFinalization();
        System.gc();
        System.gc();
        
        start = System.currentTimeMillis();
        for(int each = 0; each < trials; each++) {
            mimes[each] = new MimeMediaType(strings[each]);
        }
        stop = System.currentTimeMillis();
        System.out.println( stop - start + "ms for " + trials + " unique types.");
        
        System.runFinalization();
        System.gc();
        System.gc();
        
        start = System.currentTimeMillis();
        for(int each = 0; each < trials; each++) {
            mimes[each] = new MimeMediaType(strings[each]).intern();
        }
        stop = System.currentTimeMillis();
        System.out.println( stop - start + "ms for " + trials + " unique interned types.");
        
        
        Arrays.fill(strings, null);
        Arrays.fill(mimes, null);
        
        for(int each = 0; each < trials; each++) {
            strings[each] = new String("text/xml; charset=\"UTF-XX" + (each % 100) + "\"");
        }
        
        System.runFinalization();
        System.gc();
        System.gc();
        
        start = System.currentTimeMillis();
        for(int each = 0; each < trials; each++) {
            mimes[each] = new MimeMediaType(strings[each]);
        }
        stop = System.currentTimeMillis();
        System.out.println( stop - start + "ms for " + trials + " non-unique types.");
        
        System.runFinalization();
        System.gc();
        System.gc();
        
        start = System.currentTimeMillis();
        for(int each = 0; each < trials; each++) {
            mimes[each] = new MimeMediaType(strings[each]).intern();
        }
        stop = System.currentTimeMillis();
        System.out.println( stop - start + "ms for " + trials + " non-unique interned types.");
    }
    
    public void testInternContention() {
        final int concurrency = 50;
        final int trials = 50000;
        long stop;
        
        ExecutorService executor = Executors.newCachedThreadPool(new ExceptionCatchingThreadFactory());
        
        // Pre-create threads.
        for(int spawn = 0; spawn < concurrency; spawn++) {
            executor.execute( new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch(InterruptedException woken) {
                        
                    }
                }
            });
        }
        
        try {
            Thread.sleep(500);
        } catch(InterruptedException woken) {
            
        }
        
        System.runFinalization();
        System.gc();
        System.gc();
        
        final long start = System.currentTimeMillis();
        for(int each = 0; each < concurrency; each++) {
            final String runcount = "Run #" + each;
            executor.execute( new Runnable() {
                public void run() {
                    System.err.println( runcount + " start " + (System.currentTimeMillis() - start));
                    
                    MimeMediaType [] mimes = new MimeMediaType[trials];
                    
                    for(int each = 0; each < trials; each++) {
                        mimes[each] = new MimeMediaType(new String("text/xml")).intern();
                    }
                    
                    System.err.println( runcount + " done " + (System.currentTimeMillis() - start));
                }
            });
        }
       
        executor.shutdown();
        try {
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch(InterruptedException woken) {
            
        }
        assertFalse(exceptionOnThread);
        stop = System.currentTimeMillis();
        System.err.flush();
        System.out.flush();
        
        System.out.println( stop - start + "ms for " + trials + " identical types on " + concurrency + " threads.");        
    }
    
    public void testInternUniquesContention() {
        final int concurrency = 50;
        final int trials = 1000;
        long stop;
        
        ExecutorService executor = Executors.newCachedThreadPool(new ExceptionCatchingThreadFactory());
        
        // Pre-create threads.
        for(int spawn = 0; spawn < concurrency; spawn++) {
            executor.execute( new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch(InterruptedException woken) {
                        
                    }
                }
            });
        }
        
        try {
            Thread.sleep(500);
        } catch(InterruptedException woken) {
            
        }
        
        System.runFinalization();
        System.gc();
        System.gc();
        
        final long start = System.currentTimeMillis();
        for(int each = 0; each < concurrency; each++) {
            final String runcount = "Run #" + each;
            executor.execute( new Runnable() {
                public void run() {
                    System.err.println( runcount + " start " + (System.currentTimeMillis() - start));
                    
                    MimeMediaType [] mimes = new MimeMediaType[trials];
                    
                    for(int each = 0; each < trials; each++) {
                        mimes[each] = new MimeMediaType(new String("text/xml; charset=\"" + runcount + each + "\"" )).intern();
                    }
                    
                    System.err.println( runcount + " done " + (System.currentTimeMillis() - start));
                }
            });
        }
       
        executor.shutdown();
        try {
            executor.awaitTermination(320, TimeUnit.SECONDS);
        } catch(InterruptedException woken) {
            
        }
        stop = System.currentTimeMillis();
        assertFalse("Exception occurred in one or more threads", exceptionOnThread);
        System.err.flush();
        System.out.flush();
        
        System.out.println( stop - start + "ms for " + trials + " unique types on " + concurrency + " threads.");        
    }
    
    public void testSerialization() {
        MimeMediaType one = new MimeMediaType("text/xml");
        
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            
            oos.writeObject(one);
            
            oos.close();
            bos.close();
            
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            
            MimeMediaType two = (MimeMediaType) ois.readObject();
            
            assertTrue("MimeMediaTypes should have been equal()", one.equals(two));
        } catch (Throwable failure) {
            fail("Exception during test! " + failure.getMessage());
        }
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(MimeMediaTypeTest.class);
        
        return suite;
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
        
        System.err.flush();
        System.out.flush();
    }
}
