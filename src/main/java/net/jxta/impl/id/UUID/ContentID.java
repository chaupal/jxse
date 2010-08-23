/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
 *  
 *  The Sun Project JXTA(TM) Software License
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
 *  
 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *  
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */

package net.jxta.impl.id.UUID;

import java.io.InputStream;
import java.security.MessageDigest;

import java.io.IOException;
import java.security.ProviderException;
import java.security.NoSuchAlgorithmException;

/**
 * An implementation of the {@link net.jxta.content.ContentID} ID Type.
 * The format of the ID is broken down as follows:
 * <p/>
 * <pre>
 *  0 - 15  PeerGroupID
 * 16 - 31  Unique Content ID
 * 32       Mode Byte:
 *              Bit      7: Static content flag
 *              Bits 6 - 0: Integer representing number the number of bytes
 *                          used for the variant ID.
 * 33 - 62  Optional variant ID data.  Bytes not declared as used via the
 *          integer portion of the mode byte should be zero-filled.
 * 63       IDType
 * </pre>
 */
public class ContentID extends net.jxta.content.ContentID {
    
    /**
     * Location of the group id in the byte array.
     */
    protected final static int groupIdOffset = 0;
    
    /**
     * Location of the indexable/unique ID of the content.
     */
    protected final static int indexIdOffset =
            ContentID.groupIdOffset + IDFormat.uuidSize;
    
    /**
     * Length of the indexable/unique ID field.
     */
    protected final static int indexIdLength = IDFormat.uuidSize;
    
    /**
     * Location of the mode byte used to flag static content as well as to
     * indicate the length of the optional variant data.
     */
    protected final static int modeOffset =
            ContentID.indexIdOffset + indexIdLength;
    
    /**
     * Location of the variant hash value portion of the id within the
     * byte array.
     */
    protected final static int variantOffset = ContentID.modeOffset + 1;
    
    /**
     * Maximum number of bytes that can be used as the unique value.
     * This is capped at 127 such that the most significant bit of the mode
     * byte can be used to indicate static content.
     */
    protected final static int variantMaxLength =
            Math.min(127, IDFormat.flagsOffset - ContentID.variantOffset);
    
    /**
     * The id data.
     */
    protected IDBytes id;
    
    /**
     * Intializes contents from provided bytes.
     *
     * @param id    the ID data
     */
    protected ContentID( IDBytes id ) {
        super();
        this.id = id;
    }
    
    /**
     * Internal constructor.
     */
    protected ContentID() {
        super();
        id = new IDBytes(IDFormat.flagContentID);
    }
    
    /**
     * Internal constructor.
     */
    protected ContentID( PeerGroupID groupID ) {
        this();
        
        UUID groupUUID = groupID.getUUID();
        id.longIntoBytes( groupIdOffset,
                groupUUID.getMostSignificantBits() );
        id.longIntoBytes( groupIdOffset + 8,
                groupUUID.getLeastSignificantBits() );
    }
    
    /**
     * Internal constructor.
     */
    protected ContentID( PeerGroupID groupID, UUID indexUUID ) {
        this(groupID);
        
        id.longIntoBytes( ContentID.indexIdOffset,
                indexUUID.getMostSignificantBits() );
        id.longIntoBytes( ContentID.indexIdOffset + 8,
                indexUUID.getLeastSignificantBits() );
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Public constructors:
    
    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newContentID(net.jxta.peergroup.PeerGroupID,boolean)}.
     */
    public ContentID( PeerGroupID groupID, boolean contentIsStatic ) {
        this( groupID, UUIDFactory.newUUID() );
        applyStatic(contentIsStatic);
    }
    
    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newContentID(net.jxta.peergroup.PeerGroupID,boolean,byte[])}.
     */
    public ContentID( PeerGroupID groupID, boolean contentIsStatic,
            byte[] indexSeed) {
        this( groupID );
        applyIndexSeed(indexSeed);
        applyStatic(contentIsStatic);
    }
    
    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newContentID(net.jxta.peergroup.PeerGroupID,boolean,InputStream)}.
     */
    public ContentID( PeerGroupID groupID, boolean contentIsStatic,
            InputStream indexSeed)
            throws IOException {
        this( groupID );
        applyIndexSeed(indexSeed);
        applyStatic(contentIsStatic);
    }
    
    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newContentID(net.jxta.peergroup.PeerGroupID,boolean,byte[],byte[])}.
     */
    public ContentID( PeerGroupID groupID, boolean contentIsStatic,
            byte[] indexSeed, byte[] variant) {
        this( groupID );
        applyIndexSeed(indexSeed);
        applyVariant(variant);
        applyStatic(contentIsStatic);
    }
    
    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newContentID(net.jxta.peergroup.PeerGroupID,boolean,InputStream,byte[])}.
     */
    public ContentID( PeerGroupID groupID, boolean contentIsStatic,
            InputStream indexSeed, byte[] variant)
            throws IOException {
        this( groupID );
        applyIndexSeed(indexSeed);
        applyVariant(variant);
        applyStatic(contentIsStatic);
    }
    
    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newContentID(net.jxta.peergroup.PeerGroupID,boolean,byte[],InputStream)}.
     */
    public ContentID( PeerGroupID groupID, boolean contentIsStatic,
            byte[] indexSeed, InputStream variant)
            throws IOException {
        this( groupID );
        applyIndexSeed(indexSeed);
        applyVariant(variant);
        applyStatic(contentIsStatic);
    }
    
    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newContentID(net.jxta.peergroup.PeerGroupID,boolean,InputStream,InputStream)}.
     */
    public ContentID( PeerGroupID groupID, boolean contentIsStatic,
            InputStream indexSeed, InputStream variant)
            throws IOException {
        this( groupID );
        applyIndexSeed(indexSeed);
        applyVariant(variant);
        applyStatic(contentIsStatic);
    }
    
    //////////////////////////////////////////////////////////////////////////
    // Public methods:
    
    /**
     * {@inheritDoc}
     */
    public Object getVariantValue() {
        StringBuilder encoded = new StringBuilder();
        int lastIndex = variantOffset + (modeOffset & 0x7F);
        int index = variantOffset;
        
        while (index++ < lastIndex) {
            encoded.append( id.bytes[ index ] );
        }
        
        return encoded.toString();
    }
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean equals( Object target ) {
        if (this == target) {
            return true;
        }
        
        if (target instanceof ContentID ) {
            ContentID contentTarget = (ContentID) target;
            
            return id.equals( contentTarget.id );
        }  else {
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getIDFormat() {
        return IDFormat.INSTANTIATOR.getSupportedIDFormat();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object getUniqueValue() {
        return getIDFormat() + "-" + (String) id.getUniqueValue();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public net.jxta.id.ID getPeerGroupID() {
        UUID groupUUID = new UUID(
                id.bytesIntoLong( groupIdOffset ),
                id.bytesIntoLong( groupIdOffset + 8 ) );
        PeerGroupID groupID = new PeerGroupID( groupUUID );
        
        // Convert to the generic world PGID as necessary
        return IDFormat.translateToWellKnown( groupID );
    }
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean isStatic() {
        return (id.bytes[ modeOffset ] < 0);
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Private methods:
    
    /**
     * Applies the seed data provided into the portion of the ID which
     * is unique per Content instance, up to the maximum number of bytes
     * provided.  Also sets the mode byte equal to the number of bytes
     * applied.
     *
     * @param seed uniquely identifying seed data
     */
    private void applyIndexSeed(InputStream indexSeed) throws IOException {
        applyIndexSeed( getHash(indexSeed) );
    }
    
    /**
     * Applies the seed data provided into the portion of the ID which
     * is unique per Content instance, up to the maximum number of bytes
     * provided.  Also sets the mode byte equal to the number of bytes
     * applied.
     *
     * @param seed uniquely identifying seed data
     */
    private void applyIndexSeed(byte[] indexSeed) {
        System.arraycopy( indexSeed, 0, id.bytes, indexIdOffset,
                Math.min(indexSeed.length, indexIdLength));
        
        // Make it a valid UUID
        id.bytes[indexIdOffset + 6] &= 0x0f;
        id.bytes[indexIdOffset + 6] |= 0x40; /* version 4 */
        id.bytes[indexIdOffset + 8] &= 0x3f;
        id.bytes[indexIdOffset + 8] |= 0x80; /* IETF variant */
        id.bytes[indexIdOffset + 10] &= 0x3f;
        id.bytes[indexIdOffset + 10] |= 0x80; /* multicast bit */
    }
    
    /**
     * Applies the seed data provided into the portion of the ID which
     * is unique per Content instance, up to the maximum number of bytes
     * provided.  Also sets the mode byte equal to the number of bytes
     * applied.
     *
     * @param seed uniquely identifying seed data
     */
    private void applyVariant(InputStream variantSeed) throws IOException {
        applyVariant( getHash(variantSeed) );
    }
    
    /**
     * Applies the seed data provided into the portion of the ID which
     * is unique per Content instance, up to the maximum number of bytes
     * provided.  Also sets the mode byte equal to the number of bytes
     * applied.
     *
     * @param seed uniquely identifying seed data
     */
    private void applyVariant(byte[] variantSeed) {
        int len = Math.min(variantSeed.length, variantMaxLength);
        id.bytes[ modeOffset ] = (byte) len;
        System.arraycopy( variantSeed, 0, id.bytes, variantOffset, len);
    }
    
    
    /**
     * To be called after the all ID bytes are applied, this sets the
     * mode byte to indicate that the ID represents static data.
     */
    private void applyStatic(boolean contentIsStatic) {
        // Implicit: id.bytes[modeOffset] &= 0x80;
        if (contentIsStatic) {
            id.bytes[ modeOffset ] |= 0x80;
        }
    }
    
    /**
     *  Calculates the SHA-1 hash of a stream.
     *
     *  @param in The InputStream.
     */
    private byte[] getHash( InputStream in ) throws IOException {
        MessageDigest dig;
        try {
            dig = MessageDigest.getInstance( "SHA-1" );
        } catch( NoSuchAlgorithmException caught ) {
            throw new ProviderException(
                    "SHA-1 digest algorithm not found\n", caught);
        }
        
        dig.reset();
        byte [] chunk = new byte[1024];
        try {
            do {
                int read = in.read(chunk);
                if( read == -1 ) {
                    break;
                }
                
                dig.update( chunk, 0, read );
            } while( true );
        } finally {
            in.close();
        }
        
        return dig.digest();
    }
    
}
