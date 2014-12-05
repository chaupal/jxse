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


import junit.framework.*;


/**
 *
 * @author mike
 */
public class TimeUtilsTest extends TestCase {
    
    public TimeUtilsTest(java.lang.String testName) {
        super(testName);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
        
        System.err.flush();
        System.out.flush();
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(TimeUtilsTest.class);

        return suite;
    }
    
    public void testConstants() {
        assertTrue("zero milliseconds not less than a millisecond", TimeUtils.ZEROMILLISECONDS < TimeUtils.AMILLISECOND);
        assertTrue("millisecond not less than a second", TimeUtils.AMILLISECOND < TimeUtils.ASECOND);
        assertTrue("second not less than a minute", TimeUtils.ASECOND < TimeUtils.AMINUTE);
        assertTrue("minute not less than an hour", TimeUtils.AMINUTE < TimeUtils.ANHOUR);
        assertTrue("hour not less than a day", TimeUtils.ANHOUR < TimeUtils.ADAY);
        assertTrue("day not less than a week", TimeUtils.ADAY < TimeUtils.AWEEK);
        assertTrue("week not less than a fornight", TimeUtils.AWEEK < TimeUtils.AFORTNIGHT);
        assertTrue("fornight not less than a year", TimeUtils.AFORTNIGHT < TimeUtils.AYEAR);
        assertTrue("year not less than a leap year", TimeUtils.AYEAR < TimeUtils.ALEAPYEAR);
    }
    
    public void testTimeNow() {
        long firstNow = TimeUtils.timeNow();
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException woken) {
            Thread.interrupted();
        }
        
        long secondNow = TimeUtils.timeNow();
        
        assertTrue("Time is not working", secondNow > firstNow);
    }
    
    public void testToAbsoluteTimeMillis1() {
        long relativeNow = TimeUtils.toAbsoluteTimeMillis(0);
        
        long now = TimeUtils.timeNow();
        
        assertTrue("relative now is later than now", now >= relativeNow);
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException woken) {
            Thread.interrupted();
        }
        
        long aboutepoch = TimeUtils.toAbsoluteTimeMillis(-now);
        
        assertTrue("epoch is not before now", aboutepoch >= 0);
        
        long inTheFuture = TimeUtils.toAbsoluteTimeMillis(10000);
        
        assertTrue("Time is running backwards", inTheFuture >= now);
        
        long forever = TimeUtils.toAbsoluteTimeMillis(Long.MAX_VALUE);
        
        assertTrue("forever is not the end of time", forever == Long.MAX_VALUE);
        
        long almostForever = TimeUtils.toAbsoluteTimeMillis(Long.MAX_VALUE - 1);
        
        assertTrue("Almost forever was not enough", almostForever == Long.MAX_VALUE);
        
        long bigbang = TimeUtils.toAbsoluteTimeMillis(Long.MIN_VALUE);
        
        assertTrue("all time is is not the start of time.", bigbang == Long.MIN_VALUE);
        
        long longlongago = TimeUtils.toAbsoluteTimeMillis(Long.MIN_VALUE + 1);
        
        assertTrue("long long ago is not when it should be", longlongago < now);
        
        long rightaway = TimeUtils.toAbsoluteTimeMillis(0);
        
        assertTrue("right away should not be less than now.", rightaway >= now);
        
    }

    public void testToRelativeTime() {
        long justpast = TimeUtils.toRelativeTimeMillis(-1);
        
        assertTrue("-1 is not a mapped value", (justpast > Long.MIN_VALUE) && (justpast < Long.MAX_VALUE));
        
        long now = TimeUtils.toRelativeTimeMillis(0);
        
        assertTrue("0 is not a mapped value", (now > Long.MIN_VALUE) && (now < Long.MAX_VALUE));
        
        long beginingoftime = TimeUtils.toRelativeTimeMillis(Long.MIN_VALUE);
        
        assertTrue("beginging of time not beginging of time", beginingoftime == Long.MIN_VALUE);
          
        long endoftime = TimeUtils.toRelativeTimeMillis(Long.MAX_VALUE);
        
        assertTrue("end of time not end of time", endoftime == Long.MAX_VALUE);
    }

    public void testTimeWarp() {
        
        assertTrue("Warper active", TimeUtils.WARPBEGAN == 0); // private API
        
        TimeUtils.TIMEWARP = 0; // private API
        
        long now = TimeUtils.timeNow();
        
        TimeUtils.timeWarp(10000000);
        
        long later = TimeUtils.timeNow();
        
        assertTrue("later wasn't after now", later > now);
    }
    
    public void testAutoWarper() {
        
        assertTrue("Warper already active", TimeUtils.WARPFACTOR == 1.0); // private API
        
        TimeUtils.TIMEWARP = 0; // private API
        
        long start = TimeUtils.timeNow();

        TimeUtils.autoWarp((double) (TimeUtils.AWEEK / TimeUtils.ASECOND));
        
        try {
            Thread.sleep(3 * TimeUtils.ASECOND);
        } catch (InterruptedException woken) {
            Thread.interrupted();
        }
        
        long muchLater = TimeUtils.timeNow();
        
        assertTrue("much later wasn't in the future.", TimeUtils.toRelativeTimeMillis(muchLater, start) >= (3 * TimeUtils.AWEEK));
    }
}
