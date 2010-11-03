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

import net.jxta.content.Content;
import net.jxta.content.ContentShareEvent;
import net.jxta.content.ContentShareEvent.Builder;
import net.jxta.content.ContentShareListener;
import net.jxta.id.ID;
import net.jxta.pipe.OutputPipe;
import net.jxta.protocol.ContentAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.impl.content.AbstractPipeContentShare;

/**
 * Implementation of the ContentShare interface for use in the
 * default implementation.  This class implements the bare minimum requirements
 * for a Content share implementation and will likely always need to be
 * extended by the provider implementation to be useful.
 */
public class DefaultContentShare extends AbstractPipeContentShare<
    ContentAdvertisement, DefaultContentShareAdvertisementImpl> {

    /**
     * Construct a new DefaultContentShare object, generating a new
     * PipeAdvertisement.
     *
     * @param origin content provider sharing this content
     * @param content content object to share
     * @param pipeAdv pipe used to contact the server
     */
    public DefaultContentShare(
            DefaultContentProvider origin, 
            Content content, PipeAdvertisement pipeAdv) {
	super(origin, content, pipeAdv);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DefaultContentShareAdvertisementImpl createContentShareAdvertisement() {
        return new DefaultContentShareAdvertisementImpl();
    }

    /**
     * Notify all listeners of this object of a new session being
     * created.
     * 
     * @param session new session being created
     */
    protected void fireShareSessionOpened(ActiveTransfer session) {
        ContentShareEvent event = null;
        for (ContentShareListener listener : getContentShareListeners()) {
            if (event == null) {
                event = createEventBuilder(session).build();
            }
            listener.shareSessionOpened(event);
        }
    }

    /**
     * Notify all listeners of this object of an idle session being
     * garbage collected.
     * 
     * @param session session being closed
     */
    protected void fireShareSessionClosed(ActiveTransfer session) {
        ContentShareEvent event = null;
        for (ContentShareListener listener : getContentShareListeners()) {
            if (event == null) {
                event = createEventBuilder(session).build();
            }
            listener.shareSessionClosed(event);
        }
    }

    /**
     * Notify all listeners of this object that the share is being
     * accessed.
     * 
     * @param session share being accessed
     * @param resp response to the share access
     */
    protected void fireShareAccessed(
            ActiveTransfer session, DataResponse resp) {
        ContentShareEvent event = null;
        for (ContentShareListener listener : getContentShareListeners()) {
            if (event == null) {
                Builder builder = createEventBuilder(session);
                builder.dataStart(resp.getOffset());
                builder.dataSize(resp.getLength());
                event = builder.build();
            }
            listener.shareAccessed(event);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods:

    /**
     * Creates and initializes a ContentShareEvent for the session
     * given.
     * 
     * @param session session to create event for
     * @return event object
     */
    private Builder createEventBuilder(ActiveTransfer session) {
        Builder result = new Builder(this, session);

        // Name the remote peer by it's pipe ID
        OutputPipe pipe = session.getOutputPipe();
        ID pipeID = pipe.getPipeID();
        result.remoteName(pipeID.toString());

        return result;
    }

}
