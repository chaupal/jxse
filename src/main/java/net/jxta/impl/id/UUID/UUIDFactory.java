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
 *
 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */

package net.jxta.impl.id.UUID;

import java.security.SecureRandom;
import java.util.Random;
import net.jxta.util.UUIDUtilities;

/**
 * A Factory for generating random UUIDs.  Much of this class' functionality
 * has migrated into net.jxta.util.UUIDUtilities which deals with
 * java.util.UUIDs instead of the JXTA-specific implementation of UUID.
 *
 * @see         net.jxta.impl.id.UUID.UUID
 */
public final class UUIDFactory {

    /**
     *  Random number generator for UUID generation.
     */
    private Random randNum = null;

    /**
     *  We have to catch exceptions from construct of JRandom so we
     *  have to init it inline.
     */
    private static UUIDFactory factory = new UUIDFactory();

    /**
     *  Generate a new random UUID value. The UUID returned is a version 4 IETF
     *  variant random UUID.
     *
     *  <p/>This member must be synchronized because it makes use of shared
     *  internal state.
     *
     *  @return UUID returns a version 4 IETF variant random UUID.
     */
    public synchronized static UUID newUUID() {

        return newUUID(factory.randNum.nextLong(), factory.randNum.nextLong());
    }

    /**
     *  Generate a new UUID value. The UUID returned is a version 1 IETF
     *  variant UUID.
     *
     *  <p/>The node value used is currently a random value rather than the
     *  normal ethernet MAC address because the MAC address is not directly
     *  accessible in to java.
     *
     *  @return UUID returns a version 1 IETF variant UUID.
     */
    public static UUID newSeqUUID() {
        java.util.UUID utilID = UUIDUtilities.newSeqUUID();
        return new UUID(utilID.getMostSignificantBits(),
                utilID.getLeastSignificantBits());
    }

    /**
     *  Generate a new UUID value. The values provided are masked to produce a
     *  version 4 IETF variant random UUID.
     *
     *  @param bytes the 128 bits of the UUID
     *  @return UUID returns a version 4 IETF variant random UUID.
     */
    public static UUID newUUID(byte[] bytes) {
        if (bytes.length != 16) {
            throw new IllegalArgumentException("bytes must be 16 bytes in length");
        }

        long mostSig = 0;

        for (int i = 0; i < 8; i++) {
            mostSig = (mostSig << 8) | (bytes[i] & 0xff);
        }

        long leastSig = 0;

        for (int i = 8; i < 16; i++) {
            leastSig = (leastSig << 8) | (bytes[i] & 0xff);
        }

        return newUUID(mostSig, leastSig);
    }

    /**
     *  Generate a new UUID value. The values provided are masked to produce a
     *  version 3 IETF variant UUID.
     *
     *  @param mostSig High-long of UUID value.
     *  @param leastSig Low-long of UUID value.
     *  @return UUID returns a version 3 IETF variant random UUID.
     */
    public static UUID newHashUUID(long mostSig, long leastSig) {

        mostSig &= 0xFFFFFFFFFFFF0FFFL;
        mostSig |= 0x0000000000003000L; // version 3
        leastSig &= 0x3FFFFFFFFFFFFFFFL;
        leastSig |= 0x8000000000000000L; // IETF variant

        return new UUID(mostSig, leastSig);
    }

    /**
     *  Generate a new UUID value. The values provided are masked to produce a
     *  version 4 IETF variant random UUID.
     *
     *  @param mostSig High-long of UUID value.
     *  @param leastSig Low-long of UUID value.
     *  @return UUID returns a version 4 IETF variant random UUID.
     */
    public static UUID newUUID(long mostSig, long leastSig) {

        mostSig &= 0xFFFFFFFFFFFF0FFFL;
        mostSig |= 0x0000000000004000L; // version 4
        leastSig &= 0x3FFFFFFFFFFFFFFFL;
        leastSig |= 0x8000000000000000L; // IETF variant

        leastSig &= 0xFFFF7FFFFFFFFFFFL;
        leastSig |= 0x0000800000000000L; // multicast bit

        return new UUID(mostSig, leastSig);
    }

    /**
     *  Singleton class
     */
    private UUIDFactory() {
        randNum = new SecureRandom();
    }
}
