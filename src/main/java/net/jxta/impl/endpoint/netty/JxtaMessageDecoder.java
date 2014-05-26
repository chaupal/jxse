package net.jxta.impl.endpoint.netty;

import java.nio.ByteBuffer;

import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.peergroup.PeerGroup;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

/**
 * Decodes JXTA message frames from their network form into logical Message objects,
 * and passes them upstream.
 * 
 * @author iain.mcginniss@onedrum.com
 */
@ChannelPipelineCoverage("all")
public class JxtaMessageDecoder extends OneToOneDecoder {

    public static final String NAME = "jxtaMessageDecoder";
    private PeerGroup peerGroup;

    JxtaMessageDecoder(PeerGroup peerGroup) {
        this.peerGroup = peerGroup;
    }

	@Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if(!(msg instanceof SerializedMessage)) {
            return msg;
        }
        
        SerializedMessage message = (SerializedMessage) msg;
        MimeMediaType contentType = message.getMessageHeader().getContentTypeHeader();
        ByteBuffer messageContents = message.getMessageContents().toByteBuffer();
        
        // TODO: we should pull the "content-coding" header out and do something with it, presumably
        // pass it to the factory. However, this is not done by TCPMessenger or MCastTransport either
        return WireFormatMessageFactory.fromBufferExternal(messageContents, contentType, null, peerGroup);
    }

}
