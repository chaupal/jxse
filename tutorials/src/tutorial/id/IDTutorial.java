/*
 * Copyright (c) 2006-2007 Sun Microsystems, Inc.  All rights reserved.
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
package tutorial.id;

import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;

/**
 * A simple and re-usable example of creating various JXTA IDs
 * <p/>
 * This is a two part tutorial :
 * <ol>
 * <li>Illustrates the creation of predictable ID's based upon the hash of a
 * provided string. This method provides an independent and deterministic
 * method for generating IDs. However using this method does require care
 * in the choice of the seed expression in order to prevent ID collisions
 * (duplicate IDs).</li>
 * <p/>
 * <li>New random ID's encoded with a specific GroupID.</li>
 * </ol>
 */
public class IDTutorial {
    private static final String SEED = "IDTuorial";

    /**
     * Returns a SHA1 hash of string.
     *
     * @param expression to hash
     * @return a SHA1 hash of string or {@code null} if the expression could
     *         not be hashed.
     */
    private static byte[] hash(String expression) {
        byte[] result;
        MessageDigest digest;

        if (expression == null) {
            throw new IllegalArgumentException("Invalid null expression");
        }

        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException failed) {
            failed.printStackTrace(System.err);
            RuntimeException failure = new IllegalStateException("Could not get SHA-1 Message");
            failure.initCause(failed);
            throw failure;
        }

        try {
            byte[] expressionBytes = expression.getBytes("UTF-8");
            result = digest.digest(expressionBytes);
        } catch (UnsupportedEncodingException impossible) {
            RuntimeException failure = new IllegalStateException("Could not encode expression as UTF8");

            failure.initCause(impossible);
            throw failure;
        }
        return result;
    }

    /**
     * Given a pipe name, it returns a PipeID who's value is chosen based upon that name.
     *
     * @param pipeName instance name
     * @param pgID     the group ID encoding
     * @return The pipeID value
     */
    public static PipeID createPipeID(PeerGroupID pgID, String pipeName) {
        String seed = pipeName + SEED;
        return IDFactory.newPipeID(pgID, hash(seed.toLowerCase()));
    }

    /**
     * Creates group encoded random PipeID.
     *
     * @param pgID the group ID encoding
     * @return The pipeID value
     */
    public static PipeID createNewPipeID(PeerGroupID pgID) {
        return IDFactory.newPipeID(pgID);
    }

    /**
     * Creates group encoded random PeerID.
     *
     * @param pgID the group ID encoding
     * @return The PeerID value
     */
    public static PeerID createNewPeerID(PeerGroupID pgID) {
        return IDFactory.newPeerID(pgID);
    }

    /**
     * Given a peer name generates a Peer ID who's value is chosen based upon that name.
     *
     * @param peerName instance name
     * @param pgID     the group ID encoding
     * @return The PeerID value
     */
    public static PeerID createPeerID(PeerGroupID pgID, String peerName) {
        String seed = peerName + SEED;
        return IDFactory.newPeerID(pgID, hash(seed.toLowerCase()));
    }

    /**
     * Creates group encoded random PeerGroupID.
     *
     * @param pgID the group ID encoding
     * @return The PeerGroupID value
     */
    public static PeerGroupID createNewPeerGroupID(PeerGroupID pgID) {
        return IDFactory.newPeerGroupID(pgID);
    }

    /**
     * Constructs a Peer Group ID based upon a hash of the provided group name.
     *
     * @param groupName group name encoding value
     * @return The PeerGroupID value
     */
    public static PeerGroupID createPeerGroupID(String groupName) {
        // Use lower case to avoid any locale conversion inconsistencies.
        return IDFactory.newPeerGroupID(PeerGroupID.defaultNetPeerGroupID, hash(SEED + groupName.toLowerCase()));
    }

    /**
     * Constructs a peer group id suitable for use as an Infrastructure Peer
     * Group ID based upon a hash of the provided group name.
     *
     * @param groupName the string encoding
     * @return The infraPeerGroupID PeerGroupID
     */
    public static PeerGroupID createInfraPeerGroupID(String groupName) {
        return createPeerGroupID(groupName);
    }

    /**
     * Main method
     *
     * @param args command line arguments.  None defined
     */
    public static void main(String args[]) {
        PeerGroupID infra = createInfraPeerGroupID("infra");
        PeerID peerID = createPeerID(infra, "peer");
        PipeID pipeID = createPipeID(PeerGroupID.defaultNetPeerGroupID, "pipe");

        System.out.println(MessageFormat.format("\n\nAn infrastucture PeerGroupID: {0}", infra.toString()));
        System.out.println(MessageFormat.format("PeerID with the above infra ID encoding: {0}", peerID.toString()));
        System.out.println(MessageFormat.format("PipeID with the default defaultNetPeerGroupID encoding: {0}", pipeID.toString()));

        peerID = createNewPeerID(PeerGroupID.defaultNetPeerGroupID);
        pipeID = createNewPipeID(PeerGroupID.defaultNetPeerGroupID);
        PeerGroupID pgid = createNewPeerGroupID(PeerGroupID.defaultNetPeerGroupID);

        System.out.println(MessageFormat.format("\n\nNew PeerID created : {0}", peerID.toString()));
        System.out.println(MessageFormat.format("New PipeID created : {0}", pipeID.toString()));
        System.out.println(MessageFormat.format("New PeerGroupID created : {0}", pgid.toString()));
    }
}

