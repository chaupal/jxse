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

import net.jxta.impl.util.BASE64InputStream;
import net.jxta.impl.util.BASE64OutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Enumeration;


/**
 * The XMLSignature element for signed advertisements
 */
public class XMLSignature  {

    private byte[] digest = null;
    private byte[] signature = null;

    /**
     * The XMLSignature element for an outgoing adv
     * @param digest
     * @param signature
     */
    public XMLSignature(byte[] digest, byte[] signature) {
        this.digest = digest;
        this.signature = signature;
    }

    /**
     * The XMLSignature element for an incoming adv
     * @param raw
     */
    public XMLSignature(Element raw) {

        XMLElement elem = (XMLElement) raw;
        
        if ("XMLSignature".equals(elem.getName())) {

            Enumeration eachChild = elem.getChildren();

            while (eachChild.hasMoreElements()) {
                XMLElement aChild = (XMLElement) eachChild.nextElement();

                if ("digest".equals(aChild.getName())) {

                    try {

                        Reader digestB64 = new StringReader(aChild.getValue());
                        InputStream bis = new BASE64InputStream(digestB64);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();

                        do {
                            int c = bis.read();

                            if (-1 == c) {
                                break;
                            }
                            bos.write(c);
                        } while (true);

                        digest = bos.toByteArray();

                        bis.close();
                        bos.close();

                    } catch (IOException failed) {
                        IllegalArgumentException failure = new IllegalArgumentException("Could not process Key element");
                        throw failure;
                    }

                } else if ("signature".equals(aChild.getName())) {

                    try {

                        Reader signatureB64 = new StringReader(aChild.getValue());
                        InputStream bis = new BASE64InputStream(signatureB64);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();

                        do {
                            int c = bis.read();

                            if (-1 == c) {
                                break;
                            }
                            bos.write(c);
                        } while (true);

                        signature = bos.toByteArray();
                        
                        bis.close();
                        bos.close();

                    } catch (IOException failed) {
                        IllegalArgumentException failure = new IllegalArgumentException("Could not process Key element");
                        throw failure;
                    }

                }
            }
        }
    }

    /**
     * Get the Key Element in order to attach it to an outgoing advertisement
     */
    public XMLDocument getXMLSignatureDocument() {

        StringBuilder docBuilder = new StringBuilder();
        docBuilder.append("<XMLSignatureDocument><XMLSignature>");

        try {

            StringWriter digestB64 = new StringWriter();
            StringWriter signatureB64 = new StringWriter();

            OutputStream digestOut = new BASE64OutputStream(digestB64);
            OutputStream signatureOut = new BASE64OutputStream(signatureB64);

            digestOut.write(digest);
            digestOut.close();

            signatureOut.write(signature);
            signatureOut.close();

            docBuilder.append("<digest>");
            docBuilder.append(digestB64.toString());
            docBuilder.append("</digest>");

            docBuilder.append("<signature>");
            docBuilder.append(signatureB64.toString());
            docBuilder.append("</signature>");

        } catch (Exception failed) {
            if (failed instanceof RuntimeException) {
                throw (RuntimeException) failed;
            } else {
                throw new UndeclaredThrowableException(failed, "Failure building element");
            }
        }

        docBuilder.append("</XMLSignature></XMLSignatureDocument>");

        try {

            XMLDocument xmlSignatureDocument = (XMLDocument)StructuredDocumentFactory.newStructuredDocument(
                    MimeMediaType.XMLUTF8,
                    new StringReader(docBuilder.toString()));

            return xmlSignatureDocument;

        } catch (Exception failed) {
            if (failed instanceof RuntimeException) {
                throw (RuntimeException) failed;
            } else {
                throw new UndeclaredThrowableException(failed, "Failure building element");
            }
        }
    }

    /**
     * Get the digest
     * @return digest
     */
    public byte[] getDigest() {
        return digest;
    }

    /**
     * Get the signature
     * @return signature
     */
    public byte[] getSignature() {
        return signature;
    }
}
