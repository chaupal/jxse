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

import net.jxta.content.Content;
import net.jxta.content.ContentProvider;
import net.jxta.protocol.ContentAdvertisement;
import net.jxta.protocol.PipeAdvertisement;

/**
 * Partial implementation of the ContentShare interface for use in provider
 * implementations that use a PipeAdvertisement to contact the server.
 * This class implements the bare minimum requirements for a Content share
 * implementation and will need to be extended by the provider
 * implementation to be useful.
 */
public abstract class AbstractPipeContentShare<
        T extends ContentAdvertisement,
        U extends AbstractPipeContentShareAdvertisement>
        extends AbstractContentShare<T, U> {

    /**
     * Pipe advertisement used to locate the server which is promoting this
     * advertisment.  The pipe identified must be used by a JxtaServerSocket.
     */
    private PipeAdvertisement pipeAdv;

    /**
     * Construct a new ContentShare object.
     *
     * @param origin provider which created this share
     * @param content content object to share
     */
    public AbstractPipeContentShare(ContentProvider origin, Content content) {
	super(origin, content);
    }

    /**
     * Construct a new ContentShare object and immediately associate it
     * with the specified pipe.
     *
     * @param content content object to share
     * @param pAdv the pipe advertisement used to contact the server
     */
    public AbstractPipeContentShare(
            ContentProvider origin, Content content, PipeAdvertisement pAdv) {
        super(origin, content);
        setPipeAdvertisement(pAdv);
    }

    /**
     * Sets the PipeAdvertisement used to contact the server.  This must
     * be called prior to the call to <code>getContentAdvertisement()</code>.
     *
     * @param pAdv the pipe advertisement used to contact the server
     */
    public final void setPipeAdvertisement(PipeAdvertisement pAdv) {
        pipeAdv = pAdv;
    }

    /**
     * {@inheritDoc}
     *
     * This method extends the functionality provided by the super-class
     * by intercepting the resulting AbstractPipeContentShareAdvertisement
     * and associating the Pipe with it.
     */
    @Override
    public U getContentShareAdvertisement() {
        U adv = super.getContentShareAdvertisement();
        if (pipeAdv == null) {
            throw new IllegalStateException(
                    "The PipeAdvertisement has not yet been set");
        }
        adv.setPipeAdvertisement(pipeAdv);
        return adv;
    }

    /**
     * {@inheritDoc}
     *
     * Restricts return type to AbstractPipeContentAdvertisement.
     */
    protected abstract U createContentShareAdvertisement();

}
