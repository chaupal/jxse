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

import java.util.EventObject;

/**
 * ContentShare event object, containing information related to the event
 * being published.  Accessor methods are provided for fields which are
 * optionally supplied by the ContentProvider implementation.
 * 
 * @see net.jxta.content.ContentShare
 * @see net.jxta.content.ContentShareListener
 */
public class ContentShareEvent extends EventObject {
    
    /**
     * Identity object which uniquely identifies the remote peer.
     */
    private final Object ident;
    
    /**
     * Human readable form of the remote peer's identity.
     */
    private String remoteName;
    
    /**
     * The index of the first byte that this event's information
     * pertains to.
     */
    private Long dataStart;
    
    /**
     * The number of bytes that this event's information pertains
     * to.
     */
    private Integer dataSize;
    
    /**
     * Creates a new instance of ContentShareEvent.
     *
     * @param source ContentShare issueing this event
     * @param remoteIdentity an object which is consistently unique in
     *  referencing the remote party of this transaction.  The object
     *  used here may be anything, but should be consistently used across
     *  multiple events to identify the remote peer.
     */
    public ContentShareEvent(ContentShare source, Object remoteIdentity) {
        super(source);
        if (remoteIdentity == null) {
            throw(new IllegalArgumentException(
                    "remoteIdentity argument cannot be null"));
        }
        ident = remoteIdentity;
    }

    /**
     * Get the ContentShare which produced this event.
     *
     * @return the originator of this event
     */
    public ContentShare getContentShare() {
        return (ContentShare) getSource();
    }
    
    /**
     * Returns an object which is consistently unique in
     * referencing the remote party of this transaction.  The object
     * returned here may be anything but should be consistently used across
     * multiple events to identify the remote peer.
     * 
     * @return remote peer identity object
     */
    public Object getRemoteIdentity() {
        return ident;
    }
    
    /**
     * Returns a human-readable string which can be used to represent
     * the remote peer's identity to the end user.  The format is
     * unspecified, but should be as unique per remote peer s possible
     * and as concise as can be.  If not explicitly provided by the
     * ContentProvided creating this event, this defaults to the
     * remote identity's toString() value.
     * 
     * @return remote peer name in human readable form
     */
    public String getRemoteName() {
        if (remoteName == null) {
            remoteName = ident.toString();
        }
        return remoteName;
    }
    
    /**
     * Sets the human-readable name that can be used to represent
     * the remote peer's identity to the end user.
     * 
     * @param name remote peer name in human readable form
     */
    public void setRemoteName(String name) {
        remoteName = name;
    }
    
    /**
     * Gets the index of the first byte to which this event information
     * pertains, if provided.
     * 
     * @return byte index, or null if not provided
     */
    public Long getDataStart() {
        return dataStart;
    }
    
    /**
     * Sets the index of the first byte to which this event information
     * pertains.  This method is intended to be called by ContentProvider
     * implementations during event creation and should not be called by
     * listeners of these events.
     * 
     * @param start byte index of the first byte
     */
    public void setDataStart(long start) {
        dataStart = start;
    }

    /**
     * Gets the number of bytes to which this event information
     * pertains, if provided.
     * 
     * @return number of bytes, or null if not provided
     */
    public Integer getDataSize() {
        return dataSize;
    }
    
    /**
     * Sets the number of bytes to which this event information
     * pertains.  This method is intended to be called by ContentProvider
     * implementations during event creation and should not be called by
     * listeners of these events.
     * 
     * @param size total number of bytes of data
     */
    public void setDataSize(int size) {
        dataSize = size;
    }

}
