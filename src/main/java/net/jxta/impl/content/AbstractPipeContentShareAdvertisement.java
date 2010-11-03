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

package net.jxta.impl.content;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.XMLElement;
import net.jxta.impl.protocol.ContentShareAdvertisementImpl;
import net.jxta.protocol.PipeAdvertisement;

/**
 * An AbstractPipeContentShareAdvertisement describes a Content object
 * which uses a Pipe advertisement to contact the possessor of the
 * Content.  The protocol used over the pipe is undefined at this
 * point, hence the abstract nature of this class.
 *
 * @see net.jxta.content.Content
 * @see net.jxta.protocol.ContentAdvertisement
 * @see net.jxta.protocol.PipeAdvertisement
 */
public abstract class AbstractPipeContentShareAdvertisement
        extends ContentShareAdvertisementImpl {
    /**
     * PipeAdvertisement used to contact possessor.
     */
    private PipeAdvertisement pipeAdv;

    /**
     *  Construct a new AbstractPipeContentShareAdvertisement.
     */
    public AbstractPipeContentShareAdvertisement() {
        // Empty.
    }

    /**
     *  Construct a new AbstractPipeContentShareAdvertisement.
     */
    public AbstractPipeContentShareAdvertisement(Element root) {
        super(root);
    }

    /**
     * Returns the PipeAdvertisement used to contact the Content possessor.
     *
     * @return pipe advertisement
     */
    public PipeAdvertisement getPipeAdvertisement() {
        return (pipeAdv == null ? null : pipeAdv.clone());
    }

    /**
     * Sets the PipeAdvertisement used to contact the Content possessor.
     *
     * @param pipeAdvertisement pipe advertisement
     */
    public void setPipeAdvertisement(PipeAdvertisement pipeAdvertisement) {
        pipeAdv = pipeAdvertisement;
    }

    /**
     * Clone this AbstractPipeContentShareAdvertisement.
     *
     *
     * @return a copy of this AbstractPipeContentShareAdvertisement
     */
    @Override
    public AbstractPipeContentShareAdvertisement clone() {
        // All members are either immutable or never modified nor allowed to
        // be modified: all accessors return clones.
        AbstractPipeContentShareAdvertisement clone =
                (AbstractPipeContentShareAdvertisement) super.clone();
        clone.setPipeAdvertisement(getPipeAdvertisement());
        return clone;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    protected boolean handleElement( Element raw ) {
        if ( super.handleElement( raw ) )
            return true;

        XMLElement elem = (XMLElement) raw;
        String nm = elem.getName();

        if (nm.equals(PipeAdvertisement.getAdvertisementType())) {
            try {
                PipeAdvertisement aPipeAdv = (PipeAdvertisement)
                AdvertisementFactory.newAdvertisement(elem);
                setPipeAdvertisement(aPipeAdv);
            } catch (ClassCastException wrongAdv) {
                throw new IllegalArgumentException(
                        "Bad pipe advertisement in advertisement");
            }
            return true;
        }

        return false;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Document getDocument( MimeMediaType encodeAs ) {
        StructuredDocument adv =
                (StructuredDocument) super.getDocument( encodeAs );

        PipeAdvertisement pAdv = getPipeAdvertisement();
        if (pAdv != null) {
            StructuredTextDocument advDoc = (StructuredTextDocument)
            pAdv.getDocument(encodeAs);
            StructuredDocumentUtils.copyElements(adv, adv, advDoc);
        }

        return adv;
    }

}
