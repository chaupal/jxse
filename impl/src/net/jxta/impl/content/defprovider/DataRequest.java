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

package net.jxta.impl.content.defprovider;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.content.ContentID;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.id.IDFactory;
import net.jxta.protocol.PipeAdvertisement;

/**
 * Implements a Content Data Request Message according to the schema:
 *
 * <p/><pre>
 * &lt;xs:element name="DataRequest" type="DataRequestType"/>
 *
 * &lt;xs:complexType name="DataRequestType">
 *   &lt;xs:sequence>
 *     &lt;xs:element name="ContentID" type="xs:string"
 *       minOccurs="1" maxOccurs="1" />
 *     &lt;xs:element name="Offs" type="xs:int"
 *       minOccurs="1" maxOccurs="1" />
 *     &lt;xs:element name="Len"  type="xs:int"
 *       minOccurs="1" maxOccurs="1" />
 *     &lt;xs:element name="PipeAdv"  type="jxta:PipeAdvertisement"
 *       minOccurs="1" maxOccurs="1" />
 *     &lt;xs:element name="QID"  type="xs:int"
 *       minOccurs="0" maxOccurs="1" />
 *   &lt;/xs:sequence>
 * &lt;/xs:complexType>
 * </pre>
 */
public class DataRequest {
    private final static Logger LOG =
            Logger.getLogger(DataRequest.class.getName());
    private static final String tagRoot = "DataRequest";
    private static final String tagID = "ContentID";
    private static final String tagOffs = "Offs";
    private static final String tagLen = "Len";
    private static final String tagQueryID = "QID";
    private static final String tagAdv = "PipeAdv";

    private PipeAdvertisement adv;
    private ContentID id;
    private long offs;
    private int len;
    private int qid;

    /**
     * Default constructor.
     */
    public DataRequest() {
    }

    /**
     * Build request object from existing XML document.
     */
    public DataRequest(Element root) {
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
        PipeAdvertisement pAdv;
        XMLElement elem = (XMLElement) raw;
        ContentID contentId;
        URI uri;
        int i;
        long l;

        if (elem.getName().equals(tagID)) {
            try {
                uri =  new URI( elem.getTextValue() );
                contentId = (ContentID) IDFactory.fromURI( uri );
                setContentID(contentId);
            } catch ( URISyntaxException badID ) {
                throw new IllegalArgumentException("Bad content ID in request", badID);
            }
            return true;
        } else if (elem.getName().equals(tagOffs)) {
            try {
                l = Long.parseLong(elem.getTextValue());
                setOffset(l);
                if (l < 0) {
                    throw new IllegalArgumentException("Unusable offset in request");
                }
            } catch (NumberFormatException nfx) {
                throw new IllegalArgumentException("Unusable offset in request", nfx);
            }
            return true;
        } else if (elem.getName().equals(tagLen)) {
            try {
                i = Integer.parseInt(elem.getTextValue());
                setLength(i);
                if (i <= 0) {
                    throw new IllegalArgumentException("Unusable length in request");
                }
            } catch (NumberFormatException nfx) {
                throw new IllegalArgumentException("Unusable length in request", nfx);
            }
            return true;
        } else if (elem.getName().equals(tagQueryID)) {
            try {
                i = Integer.parseInt(elem.getTextValue());
                setQueryID(i);
            } catch (NumberFormatException nfx) {
                throw new IllegalArgumentException( "Unusable query ID in request" );
            }
            return true;
        } else if (elem.getName().equals(tagAdv)) {
            try {
                XMLDocument xml = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(
                        MimeMediaType.XMLUTF8, new StringReader(elem.getTextValue()) );
                pAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(xml);
                setResponsePipe(pAdv);
            } catch ( IOException iox ) {
                throw new IllegalArgumentException(
                        "Could not extract PipeAdvertisement from request\n", iox);
            }
            return true;
        }

        // element was not handled
        return false;
    }

    /**
     *  Intialize a Discovery Query from a portion of a structured document.
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
        StructuredTextDocument doc = (StructuredTextDocument)
            StructuredDocumentFactory.newStructuredDocument(asMimeType, tagRoot);
        Element e;

        if (doc instanceof XMLDocument) {
            XMLDocument xmlDoc = (XMLDocument) doc;
            xmlDoc.addAttribute("xmlns:jxta", "http://jxta.org");
        }

        e = doc.createElement(tagID, getContentID().toString());
        doc.appendChild(e);

        e = doc.createElement(tagOffs, Long.toString(getOffset()));
        doc.appendChild(e);

        e = doc.createElement(tagLen, Integer.toString(getLength()));
        doc.appendChild(e);

        e = doc.createElement(tagQueryID, Integer.toString(getQueryID()));
        doc.appendChild(e);

        e = doc.createElement(tagAdv, getResponsePipe().toString());
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
     * Sets the starting offset of this request.
     */
    public void setOffset(long offs) {
        this.offs = offs;
    }

    /**
     * Returns starting offset of this request.
     */
    public long getOffset() {
        return offs;
    }

    /**
     * Sets the length of this request.
     */
    public void setLength(int len) {
        this.len = len;
    }

    /**
     * Returns length of this request.
     */
    public int getLength() {
        return len;
    }

    /**
     * Sets the query ID of this request.
     */
    public void setQueryID(int qid) {
        this.qid = qid;
    }

    /**
     * Returns query ID of this request.
     */
    public int getQueryID() {
        return qid;
    }

    /**
     * Sets the response pipe of this request.
     */
    public void setResponsePipe(PipeAdvertisement adv) {
        this.adv = adv;
    }

    /**
     * Returns the response pipe for this request.
     */
    public PipeAdvertisement getResponsePipe() {
        return adv;
    }
}
