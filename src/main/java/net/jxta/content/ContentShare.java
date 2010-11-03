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

package net.jxta.content;

import net.jxta.protocol.ContentShareAdvertisement;

/**
 * ContentShare objects represent the server / data provider portion
 * of a Content transfer mechanism.  ContentShare instances are
 * obtained through the use of a ContentProvider's
 * <code>shareContent</code> method.  Although this interface only
 * defines the bare minimum functionality required to work with shared
 * Content it was designed to be extended by provider implementations
 * such that more detailed information and control over the share
 * can be achieved.  For example, some providers may wish to expose
 * controls allowing programmatic control over the amount of bandwidth
 * which is allowed to be consumed by the transmission of the Content to
 * other peers.  Other provider implementations may simply provide
 * more detailed telemetry for the end-user to obtain and use in their
 * applications.
 * <p/>
 * An explicit effort was made when this API was created to ensure that
 * ContentShare objects created by providers would always be directly
 * accessible to the end user of the shares.  This maintains the end
 * user's ability to use type comparison (e.g., instanceof) to identify
 * a specific provider's share programmatically, allowing a typecast or
 * bean introspection to provide access to more comprehensive tuning
 * controls and interfaces.
 * <p/>
 * In addition to providing direct access, this interface also allows
 * for the possibility of a single Content instance to be provisioned
 * by multiple ContentProvider implementations concurrently on a single peer.
 * Each ContentProvider will provide a ContentShareAdvertisement describing
 * it's contact information (understandable by other instances of the
 * same ContentProvider), allowing remote peers (potentially) multiple
 * means by which to obtain the Content.  ContentProvider implementations are
 * only responsible for providing this advertisement to the end-user and
 * should not publish the advertisement to remote peers.
 * 
 * @see net.jxta.content.ContentProvider
 * @see net.jxta.content.ContentTransfer
 */
public interface ContentShare {

    /**
     * Adds a listener to be notified of share events.
     *
     * @param listener listener instance to add
     */
    void addContentShareListener(ContentShareListener listener);

    /**
     * Removes previously registered listener.
     *
     * @param listener listener instance to remove
     */
    void removeContentShareListener(ContentShareListener listener);

    /**
     * Get the provider which created and manages this share.
     * 
     * @return origin ContentProvider
     */
    ContentProvider getContentProvider();

    /**
     * Obtain the ContentShareAdvertisement which can be used to inform
     * remote peers of how to contact the possessor of this Content.
     * Note that this does not necessitate using the discovery
     * mechanism as this info can be embedded in other documents
     * and/or sent out-of-band.  This method may return null, signalling
     * the fact that the provider does not provide a mechanism by which
     * remote peers can obtain the Content.  This allows for the ability to
     * define stores of Content while not providing methods of transport
     * (other providers may be used for transport in this case).
     *
     * @return advertisement containing information on how to contact
     *  the Content possessor, or null if the provider does not support
     *  remote retrieval of the Content.  If another peer is made aware of this
     *  advertisement it should be able to retrieve the Content it
     *  advertises.
     */
    ContentShareAdvertisement getContentShareAdvertisement();

    /**
     * Returns the Content being shared.
     *
     * @return Content being shared
     */
    Content getContent();

}
