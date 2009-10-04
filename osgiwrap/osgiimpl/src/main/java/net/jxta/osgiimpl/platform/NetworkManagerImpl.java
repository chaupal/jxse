package net.jxta.osgiimpl.platform;

import net.jxta.osgi.platform.NetworkManager;
import net.jxta.osgiimpl.peergroup.PeerGroupImpl;

public class NetworkManagerImpl implements NetworkManager
{
	private final net.jxta.platform.NetworkManager jxtaNetManager;

	public NetworkManagerImpl(net.jxta.platform.NetworkManager jxtaNetManager)
	{
		this.jxtaNetManager = jxtaNetManager;
	}

	public net.jxta.osgi.peergroup.PeerGroup getNetPeerGroup()
	{
		return new PeerGroupImpl(jxtaNetManager.getNetPeerGroup());
	}
}
