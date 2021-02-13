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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.junit.Test;

/**
 *
 * @author mike
 */
public class BASE64Test {
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	

	@Test
	public void testRoundTrip0() {
		byte testVector1[] = {};

		try {
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector1));
		} catch (IOException failed) {
			fail("Exception " + failed);
		}
	}

	@Test
	public void testRoundTrip1() {
		byte testVector1[] = { 0 };
		byte testVector3[] = { 127 };
		byte testVector4[] = { -128 };

		try {
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector1));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector3));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector4));
		} catch (IOException failed) {
			fail("Exception " + failed);
		}
	}

	@Test
	public void testRoundTrip2() {
		byte testVector1[] = { 0, 0 };
		byte testVector2[] = { 0, 1 };
		byte testVector3[] = { 127, 127 };
		byte testVector4[] = { -128, -128 };

		try {
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector1));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector2));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector3));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector4));
		} catch (IOException failed) {
			fail("Exception " + failed);
		}
	}

	@Test
	public void testRoundTrip3() {
		byte testVector1[] = { 0, 0, 0 };
		byte testVector2[] = { 0, 1, 2 };
		byte testVector3[] = { 127, 127, 127 };
		byte testVector4[] = { -128, -128, -128 };

		try {
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector1));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector2));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector3));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector4));
		} catch (IOException failed) {
			fail("Exception " + failed);
		}
	}

	@Test
	public void testRoundTrip4() {
		byte testVector1[] = { 0, 0, 0, 0 };
		byte testVector2[] = { 0, 1, 2, 3 };
		byte testVector3[] = { 127, 127, 127, 127 };
		byte testVector4[] = { -128, -128, -128, -128 };

		try {
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector1));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector2));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector3));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector4));
		} catch (IOException failed) {
			fail("Exception " + failed);
		}
	}

	@Test
	public void testRoundTrip80() {
		byte testVector1[] = new byte[80];

		Arrays.fill(testVector1, (byte) 0);
		byte testVector2[] = new byte[80];

		Arrays.fill(testVector1, (byte) 1);
		byte testVector3[] = new byte[80];

		Arrays.fill(testVector1, (byte) 127);
		byte testVector4[] = new byte[80];

		Arrays.fill(testVector1, (byte) -128);

		try {
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector1));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector2));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector3));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector4));
		} catch (IOException failed) {
			fail("Exception " + failed);
		}
	}

	@Test
	public void testRoundTrip1024() {
		byte testVector1[] = new byte[1024];

		Arrays.fill(testVector1, (byte) 0);
		byte testVector2[] = new byte[1024];

		Arrays.fill(testVector2, (byte) 1);
		byte testVector3[] = new byte[1024];

		Arrays.fill(testVector3, (byte) 127);
		byte[] testVector4 = createTestArray((byte) -128);

		try {
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector1));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector2));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector3));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector4));
		} catch (IOException failed) {
			fail("Exception " + failed);
		}
	}

	@Test
	public void testRoundTrip131072() {
		byte testVector1[] = new byte[131072];

		Arrays.fill(testVector1, (byte) 0);
		byte testVector2[] = new byte[131072];

		Arrays.fill(testVector1, (byte) 1);
		byte testVector3[] = new byte[131072];

		Arrays.fill(testVector1, (byte) 127);
		byte testVector4[] = new byte[131072];

		Arrays.fill(testVector1, (byte) -128);

		try {
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector1));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector2));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector3));
			assertTrue("vector didn't round trip successfully", roundTripTest(testVector4));
		} catch (IOException failed) {
			fail("Exception " + failed);
		}
	}

	@Test
	public void testRoundTripCR1024() {
		byte testVector1[] = createTestArray((byte)0);
		String expectedB64ForVector1 
		=     "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==";

		byte testVector2[] = createTestArray((byte)1);
		String expectedB64ForVector2 
		=     "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB\n"
				+ "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQ==";

		byte testVector3[] = createTestArray((byte)127);
		String expectedB64ForVector3 
		=   "f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/\n" + 
				"f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/f39/fw==";

		byte[] testVector4 = createTestArray((byte) -128);
		String expectedB64ForVector4 
		=   "gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA\n" + 
				"gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgA==";

		try {
			assertTrue("vector didn't round trip successfully", roundTripTestCR(testVector1, expectedB64ForVector1));
			assertTrue("vector didn't round trip successfully", roundTripTestCR(testVector2, expectedB64ForVector2));
			assertTrue("vector didn't round trip successfully", roundTripTestCR(testVector3, expectedB64ForVector3));
			assertTrue("vector didn't round trip successfully", roundTripTestCR(testVector4, expectedB64ForVector4));
		} catch (IOException failed) {
			fail("Exception " + failed);
		}
	}

	private byte[] createTestArray(byte fillByte) {
		byte testArray[] = new byte[1024];
		Arrays.fill(testArray, fillByte);
		return testArray;
	}

	@Test
	public void testRoundTripCR131072Random() {
		Random random = new Random();
		byte testVector1[] = new byte[131072];

		random.nextBytes(testVector1);
		byte testVector2[] = new byte[131072];

		random.nextBytes(testVector2);
		byte testVector3[] = new byte[131072];

		random.nextBytes(testVector3);
		byte testVector4[] = new byte[131072];

		random.nextBytes(testVector4);

		try {
			assertTrue("vector didn't round trip successfully", roundTripTestCR(testVector1, null));
			assertTrue("vector didn't round trip successfully", roundTripTestCR(testVector2, null));
			assertTrue("vector didn't round trip successfully", roundTripTestCR(testVector3, null));
			assertTrue("vector didn't round trip successfully", roundTripTestCR(testVector4, null));
		} catch (IOException failed) {
			fail("Exception " + failed);
		}
	}

	public boolean roundTripTest(byte[] source) throws IOException {

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		OutputStream out = new Base64OutputStream( bout, true, 76, "\n".getBytes() );
		out.write(source, 0, source.length);
		out.flush();
		out.close();

		InputStream inp = new ByteArrayInputStream(bout.toByteArray());
		InputStream input = new Base64InputStream(inp);
		return testResult(source, input);
	}

	/**
	 *  A round trip test that also compares whether our encoder works against
	 *  an alternate sun private implementation.
	 **/
	public boolean roundTripTestCR(byte[] source, String expectedB64) throws IOException {

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		OutputStream out = new Base64OutputStream( bout, true, 76, "\n".getBytes() );
		out.write(source, 0, source.length);
		out.flush();
		out.close();

		if(expectedB64 != null) {
			String result = new String( bout.toByteArray(), "UTF-8" ).trim();
			assertEquals(expectedB64, result);
		}

		InputStream inp = new ByteArrayInputStream(bout.toByteArray());
		InputStream input = new Base64InputStream(inp);
		return testResult(source, input);
	}
		
	protected boolean testResult( byte[] source, InputStream in ) throws IOException {
		DataInput di = new DataInputStream(in);

		byte result[] = new byte[source.length];

		di.readFully(result);

		if (-1 != in.read()) {
			throw new IOException("Not at EOF");
		}

		return Arrays.equals(source, result);		
	}

}
