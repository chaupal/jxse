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


import java.net.URISyntaxException;
import net.jxta.peer.PeerID;
import net.jxta.id.IDFactory;
import net.jxta.impl.util.BASE64InputStream;
import net.jxta.impl.util.BASE64OutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Enumeration;


/**
 * The XMLSignatureInfo element for signed advertisements
 */
public class XMLSignatureInfo  {

    private PeerID peerID = null;
    private byte[] digest = null;
    private String keyalgorithm = null;
    private String signaturealgorithm = null;
    private byte[] encodedkey = null;

    private boolean includePublicKey = false;
    private boolean includePeerID = false;

    /**
     * The XMLSignatureInfo element for an outgoing adv
     * @param digest
     * @param peerID
     * @param keyalgorithm
     * @param encodedkey
     * @param includePublicKey
     * @param includePeerID
     */
    public XMLSignatureInfo(byte[] digest, PeerID peerID, String keyalgorithm, byte[] encodedkey, String signaturealgorithm, boolean includePublicKey, boolean includePeerID) {
        this.peerID = peerID;
        this.digest = digest;
        this.keyalgorithm = keyalgorithm;
        this.encodedkey = encodedkey;
        this.signaturealgorithm = signaturealgorithm;
        this.includePublicKey = includePublicKey;
        this.includePeerID = includePeerID;
    }

    /**
     * The XMLSignatureInfo element for an incoming adv
     * @param raw
     */
    public XMLSignatureInfo(Element raw) {

        XMLElement elem = (XMLElement) raw;
        
        if ("XMLSignatureInfo".equals(elem.getName())) {

            Enumeration eachChild = elem.getChildren();

            while (eachChild.hasMoreElements()) {
                XMLElement aChild = (XMLElement) eachChild.nextElement();

                if ("peerid".equals(aChild.getName())) {

                    try {

                        URI pID = new URI(aChild.getValue());

                        peerID = (PeerID)IDFactory.fromURI(pID);

                    } catch (URISyntaxException ex) {
                        IllegalArgumentException failure = new IllegalArgumentException("Could not process Key element");
                        throw failure;
                    }

                } else if ("digest".equals(aChild.getName()) || "encodedkey".equals(aChild.getName())) {

                    try {

                        Reader encodedB64 = new StringReader(aChild.getValue());
                        InputStream bis = new BASE64InputStream(encodedB64);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();

                        do {
                            int c = bis.read();

                            if (-1 == c) {
                                break;
                            }
                            bos.write(c);
                        } while (true);

                        if ("digest".equals(aChild.getName()))
                            digest = bos.toByteArray();
                        else
                            encodedkey = bos.toByteArray();
                        
                        bis.close();
                        bos.close();

                    } catch (IOException failed) {
                        IllegalArgumentException failure = new IllegalArgumentException("Could not process Key element");
                        throw failure;
                    }

                } else if ("keyalgorithm".equals(aChild.getName())) {

                    keyalgorithm = aChild.getValue();

                } else if ("signaturealgorithm".equals(aChild.getName())) {

                    signaturealgorithm = aChild.getValue();

                }
            }
        }
    }

    /**
     * Get the Key Element in order to attach it to an outgoing advertisement
     * @param doc
     * @return XMLElement containing peerID, digest, encodedkey, keyalgorithm
     */
    public XMLDocument getXMLSignatureInfoDocument() {

        StringBuilder docBuilder = new StringBuilder();
        docBuilder.append("<XMLSignatureInfoDocument><XMLSignatureInfo>");

        try {

            StringWriter digestB64 = new StringWriter();
            OutputStream digestOut = new BASE64OutputStream(digestB64);

            digestOut.write(digest);
            digestOut.close();

            docBuilder.append("<digest>");
            docBuilder.append(digestB64.toString());
            docBuilder.append("</digest>");

            if (encodedkey != null && includePublicKey) {

                StringWriter encodedkeyB64 = new StringWriter();
                OutputStream encodedkeyOut = new BASE64OutputStream(encodedkeyB64);

                encodedkeyOut.write(encodedkey);
                encodedkeyOut.close();

                docBuilder.append("<encodedkey>");
                docBuilder.append(encodedkeyB64.toString());
                docBuilder.append("</encodedkey>");
            }

            if (peerID != null && includePeerID) {
                docBuilder.append("<peerid>");
                docBuilder.append(peerID.toString());
                docBuilder.append("</peerid>");
            }

            if (keyalgorithm != null && includePublicKey) {
                docBuilder.append("<keyalgorithm>");
                docBuilder.append(keyalgorithm);
                docBuilder.append("</keyalgorithm>");
            }

            if (signaturealgorithm != null) {
                docBuilder.append("<signaturealgorithm>");
                docBuilder.append(signaturealgorithm);
                docBuilder.append("</signaturealgorithm>");
            }

        } catch (Exception failed) {
            if (failed instanceof RuntimeException) {
                throw (RuntimeException) failed;
            } else {
                throw new UndeclaredThrowableException(failed, "Failure building element");
            }
        }
        
        docBuilder.append("</XMLSignatureInfo></XMLSignatureInfoDocument>");

        try {
        
            XMLDocument xmlSignatureInfoDocument = (XMLDocument)StructuredDocumentFactory.newStructuredDocument(
                    MimeMediaType.XMLUTF8,
                    new StringReader(docBuilder.toString()));

            return xmlSignatureInfoDocument;

        } catch (Exception failed) {
            if (failed instanceof RuntimeException) {
                throw (RuntimeException) failed;
            } else {
                throw new UndeclaredThrowableException(failed, "Failure building element");
            }
        }
    }

    /**
     * Get the peerid
     * @return PeerID of originator
     */
    public PeerID getPeerID() {
        return peerID;
    }

    /**
     * Get the digest of the original adv
     * @return digest
     */
    public byte[] getDigest() {
        return digest;
    }

    /**
     * Get the encoded public key used for signing
     * @return encoded public key
     */
    public byte[] getEncodedKey() {
        return encodedkey;
    }

    /**
     * Get the public key type
     * @returnthe public key type
     */
    public String getKeyAlgorithm() {
        return keyalgorithm;
    }

    /**
     * Get the signature algorithm
     * @returnthe signature algorithm
     */
    public String getSignatureAlgorithm() {
        return signaturealgorithm;
    }
}
