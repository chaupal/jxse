package net.jxta.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

import net.jxta.impl.membership.pse.PSEUtils;
import net.jxta.logging.Logging;

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
public class Base64Utils {

	/**
	 * Logger
	 */
	private static final transient Logger LOG = Logger.getLogger(PSEUtils.class.getName());

	/**
	 * Convert a byte array into a BASE64 encoded String.
	 *
	 * @param in the bytes to be converted
	 * @return the BASE64 encoded String.
	 */
	public static String base64EncodeToString(byte[] in, boolean wrap) throws IOException {
		String encoded = new String( base64Encode( in, wrap ), StandardCharsets.UTF_8 );
		Logging.logCheckedFiner(LOG, "Encoded ", in.length, " bytes -> ", encoded.length(), " characters.");
		return encoded.trim();
	}

	/**
	 * Convert a byte array into a BASE64 encoded String.
	 *
	 * @param in The bytes to be converted
	 * @return the BASE64 encoded String.
	 */
	public static String base64EncodeToString(byte[] in) throws IOException {
		return base64EncodeToString(in, false).trim();
	}

	/**
	 * Convert a byte array into a BASE64 encoded String.
	 *
	 * @param in the bytes to be converted
	 * @return the BASE64 encoded String.
	 */
	public static byte[] base64Encode(byte[] in, boolean wrap) throws IOException {
		Base64OutputStream b64os;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		if (wrap) {
			b64os = new Base64OutputStream(bout, true, 76, "\n".getBytes());
		} else {
			b64os = new Base64OutputStream(bout);
		}
		b64os.write(in);
		b64os.close();
		return bout.toByteArray();
	}

	/**
	 * Convert a byte array into a BASE64 encoded String.
	 *
	 * @param in the bytes to be converted
	 * @return the BASE64 encoded String.
	 */
	public static byte[] base64Encode(byte[] in ) throws IOException {
		return base64Encode( in, false );
	}

	/**
	 * Convert a byte array into a BASE64 encoded String.
	 *
	 * @param in the bytes to be converted
	 * @return the BASE64 encoded String.
	 */
	public static byte[] base64Encode( String str ) throws IOException {
		return base64Encode( str.getBytes(), false );
	}

	/**
	 * Convert a BASE64 Encoded String into byte array.
	 *
	 * @param in BASE64 encoded String
	 * @return the decoded bytes.
	 */
	public static byte[] base64Decode(InputStream in) throws IOException {
		Base64InputStream b64is = new Base64InputStream(in);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		do {
			int c = b64is.read();

			if (c < 0) {
				break;
			}

			bos.write(c);
		} while (true);

		byte[] result = bos.toByteArray();

		Logging.logCheckedFiner(LOG, "Decoded ", result.length, " bytes.");

		return result;
	}

	/**
	 * Convert a BASE64 Encoded String into byte array.
	 *
	 * @param in BASE64 encoded String
	 * @return the decoded bytes.
	 */
	public static byte[] base64Decode( String str) throws IOException {
		return base64Decode( new ByteArrayInputStream( str.getBytes()));
	}

	/**
	 * Convert a BASE64 Encoded String into byte array.
	 *
	 * @param in BASE64 encoded String
	 * @return the decoded bytes.
	 */
	public static byte[] base64Decode( byte[] bt) throws IOException {
		return base64Decode( new ByteArrayInputStream( bt ));
	}

	/**
	 * Convert a BASE64 Encoded String into byte array.
	 *
	 * @param in BASE64 encoded String
	 * @return the decoded bytes.
	 */
	public static byte[] base64Decode(Reader in) throws IOException {
		return base64Decode( convert( in ));
	}

	public static Base64OutputStream convertBase64( Writer writer ) 
			throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(baos);
		return new Base64OutputStream(baos);
	}

	public static InputStream convert( Reader reader ) 
			throws IOException {
		char[] charBuffer = new char[8 * 1024];
		StringBuilder builder = new StringBuilder();
		int numCharsRead;
		while ((numCharsRead = reader.read(charBuffer, 0, charBuffer.length)) != -1) {
			builder.append(charBuffer, 0, numCharsRead);
		}
		InputStream targetStream = new ByteArrayInputStream(
				builder.toString().getBytes(StandardCharsets.UTF_8));
		return targetStream;
	}

	public static OutputStream convert( Writer writer ) 
			throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(baos);
		return baos;
	}

}
