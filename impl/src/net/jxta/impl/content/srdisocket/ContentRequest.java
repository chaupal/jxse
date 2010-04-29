/*
 *  The Sun Project JXTA(TM) Software License
 *  
 *  Copyright (c) 2001-2007 Sun Microsystems, Inc. All rights reserved.
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

 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *  
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */

package net.jxta.impl.content.srdisocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.content.ContentID;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.id.IDFactory;
import net.jxta.util.LimitInputStream;

/**
 * Implements a Content Request Message according to the schema:
 *
 * <p/><pre>
 * &lt;xs:element name="ContentRequest" type="ContentRequestType"/>
 *
 * &lt;xs:complexType name="ContentRequestType">
 *   &lt;xs:sequence>
 *     &lt;xs:element name="ContentID" type="xs:string"
 *       minOccurs="1" maxOccurs="1" />
 *   &lt;/xs:sequence>
 * &lt;/xs:complexType>
 * </pre>
 */
public class ContentRequest {
    private static final Logger LOG =
            Logger.getLogger(ContentRequest.class.getName());
    private static final String tagRoot = "ContentRequest";
    private static final String tagID = "ContentID";

    private ContentID id;

    /**
     * Default constructor.
     */
    public ContentRequest() {
    }

    /**
     * Build request object from existing XML document.
     */
    public ContentRequest(Element root) {
        initialize(root);
    }

    /**
     *  Process an individual element from the document during parse. Normally,
     *  implementations will allow the base advertisments a chance to handle the
     *  element before attempting ot handle the element themselves. ie.
     *
     *  <p/><pre><code>
     *  protected boolean handleElement(Element elem) {
     *
     *      if (super.handleElement()) {
     *           // it's been handled.
     *           return true;
     *           }
     *
     *      <i>... handle elements here ...</i>
     *
     *      // we don't know how to handle the element
     *      return false;
     *      }
     *  </code></pre>
     *
     *  @param raw the element to be processed.
     *  @return true if the element was recognized, otherwise false.
     **/
    protected boolean handleElement(Element raw) {
        XMLElement elem = (XMLElement) raw;
        ContentID contentId;
        URI uri;

        if (elem.getName().equals(tagID)) {
            try {
                uri =  new URI( elem.getTextValue() );
                contentId = (ContentID) IDFactory.fromURI( uri );
                setContentID(contentId);
            } catch (URISyntaxException badID) {
                throw new IllegalArgumentException(
                        "Bad Content ID in request", badID);
            }
            return true;
        }

        // element was not handled
        return false;
    }

    /**
     *  Intialize a Content Request from a portion of a structured document.
     *  @param root document to intialize from
     */
    protected void initialize(Element root) {
        XMLElement doc = (XMLElement) root;

        if(!XMLElement.class.isInstance(root)) {
            throw new IllegalArgumentException(getClass().getName() +
                    " only supports XMLElement");
        }

        if (!doc.getName().equals(tagRoot)) {
            throw new IllegalArgumentException(
                    "Could not construct : " + getClass().getName() +
                    "from doc containing a " + doc.getName());
        }

        Enumeration elements = doc.getChildren();
        while (elements.hasMoreElements()) {
            Element elem = (Element) elements.nextElement();

            if (!handleElement(elem)) {
                Logging.logCheckedFine(LOG, "Unhandled Element : ", elem);
            }
        }
    }

    /**
     * Read in an XML document.
     */
    public Document getDocument(MimeMediaType asMimeType) {
        StructuredDocument doc = (StructuredTextDocument)
        StructuredDocumentFactory.newStructuredDocument(asMimeType, tagRoot);
        Element e;

        if (doc instanceof XMLDocument) {
            XMLDocument xmlDoc = (XMLDocument) doc;
            xmlDoc.addAttribute("xmlns:jxta", "http://jxta.org");
        }

        e = doc.createElement(tagID, getContentID().toString());
        doc.appendChild(e);

        return doc;
    }

    /**
     * Sets the Content ID of this request.
     */
    public void setContentID(ContentID id) {
        this.id = id;
    }

    /**
     * Returns Content ID of this request.
     */
    public ContentID getContentID() {
        return id;
    }

    /**
     * Utility method to write the document to a stream with a document
     * byte length prefixed to the data.
     *
     * @param out stream to write document to
     */
    public void writeToStream(OutputStream out)
    throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        Document doc = getDocument(MimeMediaType.XMLUTF8);
        doc.sendToStream(byteOut);
        int size = byteOut.size();
        out.write(size >>> 24 & 0xFF);
        out.write(size >>> 16 & 0xFF);
        out.write(size >>> 8 & 0xFF);
        out.write(size & 0xFF);
        out.write(byteOut.toByteArray());
        out.flush();
    }

    /**
     * Utility method to read a document transmitted with the corresponding
     * <code>writeToStream</code> method.
     *
     * @param in stream to read document from
     * @return response object
     */
    public static ContentRequest readFromStream(InputStream in)
    throws IOException {
        int size = 0;
        size = in.read() << 24;
        size |= in.read() << 16;
        size |= in.read() << 8;
        size |= in.read();
        LimitInputStream limitIn = new LimitInputStream(in, size);
        StructuredDocument requestDoc =
                StructuredDocumentFactory.newStructuredDocument(
                MimeMediaType.XMLUTF8, limitIn);
        return new ContentRequest(requestDoc);
    }

}