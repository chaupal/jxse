package net.jxta.impl.endpoint.netty;

import java.util.LinkedList;
import java.util.Queue;

import org.jboss.netty.channel.AbstractChannelSink;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelPipeline;

public class FakeChannelSink extends AbstractChannelSink {

    public Queue<ChannelEvent> events = new LinkedList<ChannelEvent>();

    public void eventSunk(ChannelPipeline pipeline, ChannelEvent e) throws Exception {
        events.add(e);
    }

}
