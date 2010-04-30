package net.jxta.impl.endpoint.netty;

import net.jxta.impl.endpoint.msgframing.MessagePackageHeader;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Holds the serialized components of a message to be transmitted in the format dictated by
 * Section 7.1 "TCP/IP Message Transport" of the JXTA v2.0 protocols specification. Consists
 * of a header section containing a message property map, and the encoded contents of the
 * message itself.
 * 
 * @author Iain McGinniss (iain.mcginniss@onedrum.com)
 */
public class SerializedMessage {

    private ChannelBuffer messageContents;
    private MessagePackageHeader header;
    
    public SerializedMessage(MessagePackageHeader header, ChannelBuffer messageContents) {
        this.header = header;
        this.messageContents = messageContents;
    }
    
    public ChannelBuffer getMessageContents() {
        return messageContents;
    }
    
    public MessagePackageHeader getMessageHeader() {
        return header;
    }
}
