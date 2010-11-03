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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.jxta.content.Content;
import net.jxta.content.ContentProvider;
import net.jxta.content.ContentShare;
import net.jxta.content.ContentShareListener;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Document;
import net.jxta.protocol.ContentAdvertisement;
import net.jxta.protocol.ContentShareAdvertisement;

/**
 * Partial implementation of the ContentShare interface for use in provider
 * implementations.  This class implements the bare minimum requirements
 * for a Content share implementation and will need to be
 * extended by the provider implementation to be useful.  It works by
 * (optionally) delegating the creation of the embedded ContentAdvertisement
 * to the implementor, and by requiring the implementor to create a
 * ContentShareAdvertisement instance to house the ContentAdvertisement.
 */
public abstract class AbstractContentShare
        <T extends ContentAdvertisement,
        U extends ContentShareAdvertisement>
        implements ContentShare {
    /**
     * List of share listeners.
     */
    private final CopyOnWriteArrayList<ContentShareListener> listeners =
            new CopyOnWriteArrayList<ContentShareListener>();

    /**
     * Prover which created this share.
     */
    private final ContentProvider provider;

    /**
     * The content being shared.
     */
    private Content content;

    /**
     * Construct a new ContentShare object.
     *
     * @param origin provider which created and manages this share
     * @param content content object to share
     */
    public AbstractContentShare(ContentProvider origin, Content content) {
        this.content = content;
        provider = origin;
    }

    /**
     * {@inheritDoc}
     */
    public void addContentShareListener(ContentShareListener listener) {
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeContentShareListener(ContentShareListener listener) {
        listeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    public ContentProvider getContentProvider() {
        return provider;
    }

    /**
     * {@inheritDoc}
     *
     * This method calls <code>createContentShareListener()</code> to
     * instantiate it's advertisement.  It then sets the ContentID, MetaID,
     * and the MimeType fields.
     */
    public U getContentShareAdvertisement() {
        T contentAdv = createContentAdvertisement();
        U shareAdv = createContentShareAdvertisement();
        shareAdv.setContentAdvertisement(contentAdv);
        return shareAdv;
    }

    /**
     * {@inheritDoc}
     */
    public Content getContent() {
        return content;
    }

    //////////////////////////////////////////////////////////////////////////
    // Package methods:

    /**
     * Returns a list of event listeners.  This list will be safe to iteraate
     * over.
     *
     * @return list of event listeners
     */
    protected List<ContentShareListener> getContentShareListeners() {
        return listeners;
    }

    /**
     * Called when a new content advertisement object is needed.
     * The default implementation of this method will construct
     * a vanilla ContentAdvertisement instance using the Content
     * provided in the constructor.  Extension of this method is
     * only required if a special subclass of ContentAdvertisement
     * needs to be used.
     *
     * @return ContentAdvertisement to be inserted into the
     *  ContentShareAdvertisement
     */
    protected T createContentAdvertisement() {
        T adv = (T) AdvertisementFactory.newAdvertisement(
                ContentAdvertisement.getAdvertisementType());
        Document doc = content.getDocument();
        adv.setContentID(content.getContentID());
        adv.setMetaID(content.getMetaID());
        adv.setMimeType(doc.getMimeType());
        return adv;
    }

    /**
     * Called when a new share advertisement object is needed.
     *
     * @return created content share advertisement
     */
    protected abstract U createContentShareAdvertisement();
}
