package net.jxta.impl.endpoint.netty;

import java.util.concurrent.atomic.AtomicBoolean;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.peer.PeerID;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.util.Timer;

public class NettyTransportChannelPipelineFactory implements ChannelPipelineFactory {

	private PeerID localPeerId;
	private Timer timeoutTimer;
	private NettyChannelRegistry registry;
	private AtomicBoolean acceptConnectionFlag;
    private AddressTranslator addrTranslator;
    private EndpointAddress returnAddress;
    private EndpointAddress remoteAddress;
	
	public NettyTransportChannelPipelineFactory(PeerID localPeerId, Timer timeoutTimer, NettyChannelRegistry registry, AddressTranslator addrTranslator, EndpointAddress remoteAddress, EndpointAddress returnAddress) {
		this(localPeerId, timeoutTimer, registry, addrTranslator, new AtomicBoolean(true), remoteAddress, returnAddress);
	}
	
	public NettyTransportChannelPipelineFactory(PeerID localPeerId, Timer timeoutTimer, NettyChannelRegistry registry, AddressTranslator addrTranslator, AtomicBoolean acceptConnectionFlag, EndpointAddress remoteAddress, EndpointAddress returnAddress) {
		this.localPeerId = localPeerId;
		this.timeoutTimer = timeoutTimer;
		this.registry = registry;
		this.acceptConnectionFlag = acceptConnectionFlag;
		this.addrTranslator = addrTranslator;
		this.remoteAddress = remoteAddress;
		this.returnAddress = returnAddress;
	}

	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addFirst(ConnectionRejector.NAME, new ConnectionRejector(acceptConnectionFlag));
		pipeline.addLast(JxtaProtocolHandler.NAME, new JxtaProtocolHandler(addrTranslator, localPeerId, timeoutTimer, remoteAddress, returnAddress));
		pipeline.addLast(JxtaMessageEncoder.NAME, new JxtaMessageEncoder());
		pipeline.addLast(JxtaMessageDecoder.NAME, new JxtaMessageDecoder());
		pipeline.addLast(MessageDispatchHandler.NAME, new MessageDispatchHandler(registry));
		
		return pipeline;
	}
}
