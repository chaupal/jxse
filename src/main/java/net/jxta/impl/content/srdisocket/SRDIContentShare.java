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

import java.net.SocketAddress;
import net.jxta.content.Content;
import net.jxta.content.ContentShareEvent;
import net.jxta.content.ContentShareEvent.Builder;
import net.jxta.content.ContentShareListener;
import net.jxta.id.ID;
import net.jxta.protocol.ContentAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaSocketAddress;
import net.jxta.impl.content.AbstractPipeContentShare;

/**
 * Implementation of the ContentShare interface for use in the
 * SRDI-socket implementation.  This class implements the bare minimum requirements
 * for a Content share implementation and will likely always need to be
 * extended by the provider implementation to be useful.
 */
public class SRDIContentShare extends AbstractPipeContentShare<
        ContentAdvertisement, SRDISocketContentShareAdvertisementImpl> {
    /**
     * Construct a new SRDIContentShare object, generating a new
     * PipeAdvertisement.
     *
     * @param origin content provider which created this share
     * @param content content object to share
     * @param pipeAdv pipe advertisement for requests to be sent to
     */
    public SRDIContentShare(
            SRDISocketContentProvider origin,
            Content content,
            PipeAdvertisement pipeAdv) {
	super(origin, content, pipeAdv);
    }

    /**
     * {@inheritDoc}
     */
    protected SRDISocketContentShareAdvertisementImpl
            createContentShareAdvertisement() {
        return new SRDISocketContentShareAdvertisementImpl();
    }

    /**
     * Notify all listeners of this object of a new session being
     * created.
     * 
     * @param session session being opened
     */
    protected void fireShareSessionOpened(SocketAddress session) {
        ContentShareEvent event = null;
        for (ContentShareListener listener : getContentShareListeners()) {
            if (event == null) {
                event = createEvent(session);
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
    protected void fireShareSessionClosed(SocketAddress session) {
        ContentShareEvent event = null;
        for (ContentShareListener listener : getContentShareListeners()) {
            if (event == null) {
                event = createEvent(session);
            }
            listener.shareSessionClosed(event);
        }
    }

    /**
     * Notify all listeners of this object that the share is being
     * accessed.
     * 
     * @param session session being accessed
     */
    protected void fireShareSessionAccessed(SocketAddress session) {
        ContentShareEvent event = null;
        for (ContentShareListener listener : getContentShareListeners()) {
            if (event == null) {
                event = createEvent(session);
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
    private ContentShareEvent createEvent(SocketAddress session) {
        Builder result =  new Builder(this, session);

        // Name the remote peer by it's pipe ID
        if (session instanceof JxtaSocketAddress) {
            JxtaSocketAddress jxtaAddr = (JxtaSocketAddress) session;
            PipeAdvertisement pipeAdv = jxtaAddr.getPipeAdv();
            ID pipeID = pipeAdv.getPipeID();
            result.remoteName(pipeID.toString());
        }

        return result.build();
    }

}
