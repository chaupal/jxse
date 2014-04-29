package net.jxse.systemtests.colocated.configs;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.jxta.exception.JxtaException;
import net.jxta.platform.JxtaApplication;

import org.junit.rules.TemporaryFolder;

import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.platform.NetworkManager.ConfigMode;

/**
 * Helper class to quickly set up peers for tests
 */
public class PeerConfigurator {

	/**
	 * Configures the most important properties of the three main transports (TCP, HTTP legacy, and HTTP 2).
	 * 
	 * @param man the network manager of the peer to configure
	 * @param tcpOn whether TCP should be enabled
	 * @param tcpPort what port the peer should listen on for TCP connections. Set to -1 to disable incoming connections.
	 * @param httpOn whether HTTP should be enabled
	 * @param httpPort what port the peer should listen on for HTTP legacy connections. Set to -1 to disable incoming connections.
	 * @param http2On whether HTTP2 should be enabled
	 * @param http2Port what port the peer should listen on for HTTP2 connections. Set to -1 to disable incoming connections.
	 */
	public static void configureTransports(NetworkManager man, boolean tcpOn, int tcpPort, boolean httpOn, int httpPort, boolean http2On, int http2Port) throws IOException {
		NetworkConfigurator configurator = man.getConfigurator();
		
		configurator.setTcpEnabled(tcpOn);
		configurator.setTcpIncoming(tcpPort != -1);
		configurator.setTcpInterfaceAddress("127.0.0.1");
		configurator.setTcpOutgoing(true);
		configurator.setTcpPort(tcpPort);

		configurator.setHttpEnabled(httpOn);
		configurator.setHttpIncoming(httpPort != -1);
		configurator.setHttpInterfaceAddress("127.0.0.1");
		configurator.setHttpOutgoing(true);
		configurator.setHttpPort(httpPort);
		
		configurator.setHttp2Enabled(http2On);
		configurator.setHttp2Incoming(http2Port != -1);
		configurator.setHttp2InterfaceAddress("127.0.0.1");
		configurator.setHttp2Outgoing(true);
		configurator.setHttp2Port(http2Port);
	}
	
	/**
	 * Configure a peer to exclusively use the TCP transport.
	 * @param man the network manager of the peer to configure
	 * @param tcpPort what port the peer should listen on for TCP connections. Set to -1 to disable incoming connections.
	 */
	public static void configureTcpOnly(NetworkManager man, int tcpPort) throws IOException {
		configureTransports(man, true, tcpPort, false, -1, false, -1);
	}
	
	/**
	 * Configure a peer to exclusively use the HTTP legacy transport.
	 * @param man the network manager of the peer to configure
	 * @param tcpPort what port the peer should listen on for HTTP connections. Set to -1 to disable incoming connections.
	 */
	public static void configureHttpOnly(NetworkManager man, int httpPort) throws IOException {
		configureTransports(man, false, -1, true, httpPort, false, -1);
	}
	
	/**
	 * Configure a peer to exclusively use the HTTP2 transport.
	 * @param man the network manager of the peer to configure
	 * @param tcpPort what port the peer should listen on for HTTP2 connections. Set to -1 to disable incoming connections.
	 */
	public static void configureHttp2Only(NetworkManager man, int http2Port) throws IOException {
		configureTransports(man, false, -1, false, -1, true, http2Port);
	}
	
	/**
	 * Configures whether or not the peer should attempt to discover other resources locally,
	 * or only use the configured rendezvous for this purpose.
	 * 
	 * @param isolated when set to true, only the configured rendezvous should be used.
	 */
	public static void configureIsolation(NetworkManager man, boolean isolated) throws IOException {
		NetworkConfigurator configurator = man.getConfigurator();
		configurator.setUseMulticast(!isolated);
		configurator.setUseOnlyRelaySeeds(isolated);
		configurator.setUseOnlyRendezvousSeeds(isolated);
	}

	/**
	 * Acquires the list of transport addresses that the relay will expose.
	 */
	public static List<URI> getRelayAddresses(NetworkManager relayManager) throws IOException {
		ArrayList<URI> addresses = new ArrayList<URI>(3);
		NetworkConfigurator configurator = relayManager.getConfigurator();
		
		if(configurator.isTcpEnabled() && configurator.getTcpIncomingStatus()) {
			String tcpInterfaceAddress = withDefault(configurator.getTcpInterfaceAddress(), "127.0.0.1");
			addresses.add(URI.create("tcp://" + tcpInterfaceAddress + ":" + configurator.getTcpPort()));
		}
		
		if(configurator.isHttpEnabled() && configurator.getHttpIncomingStatus()) {
			String httpInterfaceAddress = withDefault(configurator.getHttpInterfaceAddress(), "127.0.0.1");
			addresses.add(URI.create("http://" + httpInterfaceAddress + ":" + configurator.getHttpPort()));
		}
		
		if(configurator.isHttp2Enabled() && configurator.getHttp2IncomingStatus()) {
			String http2InterfaceAddress = withDefault(configurator.getHttp2InterfaceAddress(), "127.0.0.1");
			addresses.add(URI.create("http2://" + http2InterfaceAddress + ":" + configurator.getHttp2Port()));
		}
		
		return addresses;
	}
	
	private static String withDefault(String str, String defaultVal) {
		if(str == null) {
			return defaultVal;
		}
		
		return str;
	}

	/**
	 * Configures a peer with knowledge of a particular rendesvous/relay peer's transport addresses.
	 */
	public static void configureRelayClient(NetworkManager clientMan, NetworkManager rdvRelayMan) throws IOException {
		NetworkConfigurator clientConfig = clientMan.getConfigurator();
		
		for(URI rdvRelayAddr : getRelayAddresses(rdvRelayMan)) {
			clientConfig.addSeedRelay(rdvRelayAddr);
			clientConfig.addSeedRendezvous(rdvRelayAddr);
		}
	}
	
	public static NetworkManager createTcpRdvRelayPeer(String name, int port, TemporaryFolder tempFolder) throws IOException, JxtaException {
		//NetworkManager man = new NetworkManager(ConfigMode.RENDEZVOUS_RELAY, name, tempFolder.newFolder(name).toURI());
                NetworkManager man = JxtaApplication.getNetworkManager(ConfigMode.RENDEZVOUS_RELAY, name, tempFolder.newFolder(name).toURI());
		configureTcpOnly(man, port);
		configureIsolation(man, true);
		return man;
	}
	
	public static NetworkManager createHttpRdvRelayPeer(String name, int port, TemporaryFolder tempFolder) throws IOException, JxtaException {
//		/NetworkManager man = new NetworkManager(ConfigMode.RENDEZVOUS_RELAY, name, tempFolder.newFolder(name).toURI());
                NetworkManager man = JxtaApplication.getNetworkManager(ConfigMode.RENDEZVOUS_RELAY, name, tempFolder.newFolder(name).toURI());
		configureHttpOnly(man, port);
		configureIsolation(man, true);
		return man;
	}
	
	public static NetworkManager createHttp2RdvRelayPeer(String name, int port, TemporaryFolder tempFolder) throws IOException, JxtaException {
		//NetworkManager man = new NetworkManager(ConfigMode.RENDEZVOUS_RELAY, name, tempFolder.newFolder(name).toURI());
                NetworkManager man = JxtaApplication.getNetworkManager(ConfigMode.RENDEZVOUS_RELAY, name, tempFolder.newFolder(name).toURI());
		configureHttp2Only(man, port);
		configureIsolation(man, true);
		return man;
	}
	
	public static NetworkManager createTcpClientPeer(String name, NetworkManager relay, TemporaryFolder tempFolder) throws IOException, JxtaException {
		//NetworkManager man = new NetworkManager(ConfigMode.EDGE, name, tempFolder.newFolder(name).toURI());
                NetworkManager man = JxtaApplication.getNetworkManager(ConfigMode.EDGE, name, tempFolder.newFolder(name).toURI());
		configureTcpOnly(man, -1);
		configureIsolation(man, true);
		configureRelayClient(man, relay);
		
		return man;
	}
	
	public static NetworkManager createHttpClientPeer(String name, NetworkManager relay, TemporaryFolder tempFolder) throws IOException, JxtaException {
		//NetworkManager man = new NetworkManager(ConfigMode.EDGE, name, tempFolder.newFolder(name).toURI());
                NetworkManager man = JxtaApplication.getNetworkManager(ConfigMode.EDGE, name, tempFolder.newFolder(name).toURI());
		configureHttpOnly(man, -1);
		configureIsolation(man, true);
		configureRelayClient(man, relay);
		
		return man;
	}
	
	public static NetworkManager createHttp2ClientPeer(String name, NetworkManager relay, TemporaryFolder tempFolder) throws IOException, JxtaException {
//		/NetworkManager man = new NetworkManager(ConfigMode.EDGE, name, tempFolder.newFolder(name).toURI());
                NetworkManager man = JxtaApplication.getNetworkManager(ConfigMode.EDGE, name, tempFolder.newFolder(name).toURI());
		configureHttp2Only(man, -1);
		configureIsolation(man, true);
		configureRelayClient(man, relay);
		
		return man;
	}
	
	public static NetworkManager createTcpAdhocPeer(String name, int port, TemporaryFolder tempFolder) throws IOException, JxtaException {
		//NetworkManager man = new NetworkManager(ConfigMode.EDGE, name, tempFolder.newFolder(name).toURI());
                NetworkManager man = JxtaApplication.getNetworkManager(ConfigMode.EDGE, name, tempFolder.newFolder(name).toURI());
		configureTcpOnly(man, port);
		configureIsolation(man, false);
		
		return man;
	}
	
	public static NetworkManager createHttpAdhocPeer(String name, int port, TemporaryFolder tempFolder) throws IOException, JxtaException {
		//NetworkManager man = new NetworkManager(ConfigMode.EDGE, name, tempFolder.newFolder(name).toURI());
                NetworkManager man = JxtaApplication.getNetworkManager(ConfigMode.EDGE, name, tempFolder.newFolder(name).toURI());
		configureHttp2Only(man, port);
		configureIsolation(man, false);
		
		return man;
	}
	
	public static NetworkManager createHttp2AdhocPeer(String name, int port, TemporaryFolder tempFolder) throws IOException, JxtaException {
		//NetworkManager man = new NetworkManager(ConfigMode.EDGE, name, tempFolder.newFolder(name).toURI());
                NetworkManager man = JxtaApplication.getNetworkManager(ConfigMode.EDGE, name, tempFolder.newFolder(name).toURI());
		configureHttp2Only(man, port);
		configureIsolation(man, false);
		
		return man;
	}
}
