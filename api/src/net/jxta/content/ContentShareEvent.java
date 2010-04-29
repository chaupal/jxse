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
     * Serialized version.
     */
    private static final long serialVersionUID = 2009110500L;

    /**
     * Identity object which uniquely identifies the remote peer.
     */
    private final Object ident;
    
    /**
     * Human readable form of the remote peer's identity.
     */
    private final String remoteName;
    
    /**
     * The index of the first byte that this event's information
     * pertains to.
     */
    private final Long dataStart;
    
    /**
     * The number of bytes that this event's information pertains
     * to.
     */
    private final Integer dataSize;
    
    /**
     * Builder pattern.
     */
    public static class Builder {
        // Required parameters:
        private final ContentShare bSource;
        private final Object bIdent;
        
        // Optional parameters:
        private String bRemoteName;
        private Long bDataStart;
        private Integer bDataSize;
        
        /**
         * Constructs a new builder, used to create and initialize the
         * event instance.
         * 
         * @param sourceShare ContentShare issueing this event
         * @param sourceIdent an object which is consistently unique in
         *  referencing the remote party of this transaction.  The object
         *  used here may be anything, but should be consistently used across
         *  multiple events to identify the remote peer.
         */
        public Builder(
                final ContentShare sourceShare,
                final Object sourceIdent) {
            if (sourceIdent == null) {
                throw new IllegalArgumentException("sourceIdent argument cannot be null");
            }
            bSource = sourceShare;
            bIdent = sourceIdent;
        }
        
        /**
         * Sets the human-readable name that can be used to represent
         * the remote peer's identity to the end user.
         * 
         * @param value remote peer name in human readable form
         * @return builder instance
         */
        public Builder remoteName(String value) {
            bRemoteName = value;
            return this;
        }
        
        /**
         * Sets the index of the first byte to which this event information
         * pertains.  This method is intended to be called by ContentProvider
         * implementations during event creation and should not be called by
         * listeners of these events.
         * 
         * @param value byte index of the first byte
         * @return builder instance
         */
        public Builder dataStart(long value) {
            bDataStart = Long.valueOf(value);
            return this;
        }
        
        /**
         * Sets the number of bytes to which this event information
         * pertains.  This method is intended to be called by ContentProvider
         * implementations during event creation and should not be called by
         * listeners of these events.
         * 
         * @param value total number of bytes of data
         * @return builder instance
         */
        public Builder dataSize(int value) {
            bDataSize = Integer.valueOf(value);
            return this;
        }
        
        /**
         * Build the event object.
         * 
         * @return event instance
         */
        public ContentShareEvent build() {
            return new ContentShareEvent(this);
        }
    }
    
    /**
     * Creates a new instance of ContentShareEvent.
     *
     * @param builder builder instance to use to construct our event instance
     */
    private ContentShareEvent(Builder builder) {
        super(builder.bSource);
        ident = builder.bIdent;
        if (builder.bRemoteName == null) {
            remoteName = ident.toString();
        } else {
            remoteName = builder.bRemoteName;
        }
        dataStart = builder.bDataStart;
        dataSize = builder.bDataSize;
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
        return remoteName;
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
     * Gets the number of bytes to which this event information
     * pertains, if provided.
     * 
     * @return number of bytes, or null if not provided
     */
    public Integer getDataSize() {
        return dataSize;
    }
    
}
