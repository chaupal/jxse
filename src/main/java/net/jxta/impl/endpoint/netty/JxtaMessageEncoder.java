package net.jxta.impl.endpoint.netty;

import net.jxta.endpoint.Message;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.impl.endpoint.msgframing.MessagePackageHeader;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * Encodes a Message instance into the corresponding wire header and body, using the default
 * encoding for WireFormatMessageFactory.
 * 
 * @author iain.mcginniss@onedrum.com
 */
@ChannelPipelineCoverage("all")
public class JxtaMessageEncoder extends OneToOneEncoder {

    public static final String NAME = "jxtaMessageEncoder";
    private PeerGroup peerGroup;

    JxtaMessageEncoder(PeerGroup peerGroup) {
        this.peerGroup = peerGroup;
    }

	@Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object toEncode) throws Exception {
        Message message = (Message)toEncode;
        WireFormatMessage wireMessage = WireFormatMessageFactory.toWireExternal(message, WireFormatMessageFactory.DEFAULT_WIRE_MIME, null, peerGroup);
        ChannelBuffer messageBytes = ChannelBuffers.wrappedBuffer(wireMessage.getByteBuffers());
        
        MessagePackageHeader header = new MessagePackageHeader();
        header.setContentLengthHeader(messageBytes.readableBytes());
        header.setContentTypeHeader(WireFormatMessageFactory.DEFAULT_WIRE_MIME);
        SerializedMessage serialized = new SerializedMessage(header, messageBytes);
        return serialized;
    }

}
