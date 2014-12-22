package net.jxta.impl.endpoint.netty;

import java.util.LinkedList;
import java.util.Queue;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelHandler.Sharable;//PipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;

@Sharable//ChannelPipelineCoverage("one")
public class UpstreamEventCatcher implements ChannelUpstreamHandler {

    public static final String NAME = "upstreamCatcher";
    public Queue<ChannelEvent> events = new LinkedList<ChannelEvent>();
    
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        events.add(e);
    }

}
