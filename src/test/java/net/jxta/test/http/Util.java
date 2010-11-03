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

package net.jxta.test.http;

import java.io.StringReader;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

/**
 *
 * @version $Id: Util.java,v 1.2 2007/01/26 02:14:00 bondolo Exp $
 *
 * @author james todd [gonzo at jxta dot org]
 */

public class Util {

    public static final int MINIMUM_UNICODE = 0x7f + 1;

    private static final boolean VERBOSE = false;

    private static Map getHeaders = null;
    private static Map postHeaders = null;
    private static byte[] code = new byte[64];

    static {
        getHeaders = new HashMap();

        getHeaders.put(Constants.MIME.Key.USER_AGENT, Constants.MIME.Value.TROLL);
        getHeaders.put(Constants.MIME.Key.ACCEPT, Constants.MIME.Value.ACCEPT_ALL);
        getHeaders.put(Constants.MIME.Key.CONNECTION, Constants.MIME.Value.KEEP_ALIVE);

        postHeaders = new HashMap();

        postHeaders.putAll(getHeaders);

        postHeaders.put(Constants.MIME.Key.ACCEPT_CHARSET, Constants.MIME.Value.CHARSET);
        postHeaders.put(Constants.MIME.Key.CONTENT_TYPE, Constants.MIME.Value.URL_FORM_ENCODED);

        for (int i = 0; i < 26; i++) {
            code[i] = (byte) ('A' + i);
        }

        for (int i = 0; i < 26; i++) {
            code[26 + i] = (byte) ('a' + i);
        }

        for (int i = 0; i < 10; i++) {
            code[52 + i] = (byte) ('0' + i);
        }

        code[62] = (byte) '+';
        code[63] = (byte) '/';
    }
    ;

    public static Map getDefaultGetHeaders() {
        return getHeaders;
    }

    public static Map getDefaultPostHeaders() {
        return postHeaders;
    }

    public static String getCharSet(String contentType) {
        final String defaultCharSet = "UTF8";
        String charSet = null;

        if (contentType != null) {
            String charSetPrefix = "charset=";
            int index = contentType.toLowerCase().indexOf(charSetPrefix);

            if (index > -1) {
                charSet = contentType.substring(index + charSetPrefix.length());
            }

            if (charSet != null) {
                if (charSet.equalsIgnoreCase("cp1252")) {
                    charSet = "Cp1252";
                } else if (charSet.equalsIgnoreCase("big5")) {
                    charSet = "Big5";
                } else if (charSet.equalsIgnoreCase("gb2312")) {
                    charSet = "GB2312";
                } else if (charSet.equalsIgnoreCase("shift_jis")) {
                    charSet = "SJIS";
                } else if (charSet.equalsIgnoreCase("utf-8")) {
                    charSet = defaultCharSet;
                } else {
                    charSet = null;
                }
            }
        }

        return ((charSet != null) ? charSet : defaultCharSet);
    }

    public static String toUnicodeEncoded(String data) {
        final int unicodeSize = 4;

        StringBuilder sb = new StringBuilder();

        if (data != null) {
            int l = data.length();

            for (int i = 0; i < l; i++) {
                if (data.charAt(i) >= MINIMUM_UNICODE) {
                    sb.append("\\u");

                    String s = Integer.toHexString((int) data.charAt(i));

                    for (int j = 0; j < unicodeSize - s.length(); j++) {
                        sb.append("0");
                    }

                    sb.append(s);
                } else {
                    sb.append(data.charAt(i));
                }
            }
        }

        return sb.toString();
    }

    public static String base64Encode(String s) {
        StringBuilder sb = new StringBuilder();
        StringReader r = new StringReader(s);
        int c = 0;
        int d = 0;
        int e = 0;
        int k = 0;
        int end = 0;
        byte u;
        byte v;
        byte w;
        byte x;

        while (end == 0) {
            try {
                if ((c = r.read()) == -1) {
                    c = 0;
                    end = 1;
                }
                if ((d = r.read()) == -1) {
                    d = 0;
                    end += 1;
                }
                if ((e = r.read()) == -1) {
                    e = 0;
                    end += 1;
                }
            } catch (IOException ioe) {
                if (VERBOSE) {
                    ioe.printStackTrace();
                }
                sb = null;
                break;
            }
            u = code[c >> 2];
            v = code[(3 & c) << 4 | d >> 4];
            w = code[(15 & d) << 2 | e >> 6];
            x = code[e & 63];
            if (k == 76) {
                k = 0;
                System.out.println("");
            }
            if (end >= 1) {
                x = (byte) '=';
            }
            if (end == 2) {
                w = (byte) '=';
            }
            if (end < 3) {
                sb.append("" + (char) u + (char) v + (char) w + (char) x);
            }
            k += 4;
        }
        return sb != null ? sb.toString() : null;
    }
}
