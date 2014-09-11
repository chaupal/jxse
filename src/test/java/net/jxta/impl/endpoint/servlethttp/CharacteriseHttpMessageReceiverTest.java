package net.jxta.impl.endpoint.servlethttp;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.test.util.JUnitRuleMockery;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.jmock.Expectations;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Characterise the current behaviour of the Jetty servlet-based
 * HttpMessageReceiver, to ensure nothing breaks when replacing Jetty 4.2.25
 * with the latest version.
 * 
 */
public class CharacteriseHttpMessageReceiverTest {

	private static Logger LOG;
	private static final int port = 58000;
	
    @Rule
    public JUnitRuleMockery mockContext = new JUnitRuleMockery();
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

	private ServletHttpTransport mockServletHttpContext;
	private EndpointService mockEndpointService;
	private PeerGroup mockPeerGroup;
	
	private PeerGroupID assignedPeerGroupId;
	private PeerID assignedPeerId;
	private HttpMessageReceiver httpMessageReceiver;

	@BeforeClass
	public static void setupLogging() {
		System.setProperty("net.jxta.logging.Logging", java.util.logging.Level.ALL.getName());
		BasicConfigurator.configure();
		LOG = Logging.getLogger(CharacteriseHttpMessageReceiverTest.class);
		org.apache.log4j.Logger.getLogger(HttpMessageReceiver.class).setLevel(Level.ALL);
	}
	
	@Before
	public void setUp() throws PeerGroupException, UnknownHostException {
		LOG.info("temp folder is " + tempFolder.getRoot().getAbsolutePath());

		mockServletHttpContext = mockContext.mock(ServletHttpTransport.class);
		mockEndpointService = mockContext.mock(EndpointService.class);
		mockPeerGroup = mockContext.mock(PeerGroup.class);

		assignedPeerGroupId = IDFactory.newPeerGroupID();
		assignedPeerId = IDFactory.newPeerID(assignedPeerGroupId);
		// expectations of HttpMessageReceiver's constructor
		mockContext.checking(new Expectations() {{
			oneOf(mockServletHttpContext).getEndpointService(); will(returnValue(mockEndpointService));
			oneOf(mockServletHttpContext).getAssignedID(); will(returnValue(assignedPeerGroupId));
			oneOf(mockEndpointService).getGroup(); will(returnValue(mockPeerGroup));
			oneOf(mockPeerGroup).getStoreHome(); will(returnValue(tempFolder.getRoot().toURI()));
		}});

		LOG.info("Creating HttpMessageReceiver");
		assertThat(portOpen(), is(false));
		httpMessageReceiver = new HttpMessageReceiver(
				mockServletHttpContext, 
				Arrays.asList(new EndpointAddress("urn:jxta:test#service/param")), 
				InetAddress.getLocalHost(), port, 
				Thread.currentThread().getContextClassLoader());
		assertThat(portOpen(), is(false));

		// expectations of HttpMessageReceiver's start
		mockContext.checking(new Expectations() {{
			oneOf(mockServletHttpContext).getEndpointService(); will(returnValue(mockEndpointService));
			oneOf(mockEndpointService).addMessageTransport(httpMessageReceiver); // ?
		}});

		
		LOG.info("Starting HttpMessageReceiver");
		httpMessageReceiver.start();
		assertThat(portOpen(), is(true));
	}

	@After
	public void tearDown() {
		assertThat(portOpen(), is(true));		
		LOG.info("Stopping HttpMessageReceiver");

		// expectations of HttpMessageReceiver's stop
		mockContext.checking(new Expectations() {{
			oneOf(mockServletHttpContext).getEndpointService(); will(returnValue(mockEndpointService));
			oneOf(mockEndpointService).removeMessageTransport(httpMessageReceiver); // ?
		}});

		httpMessageReceiver.stop();
		assertThat(portOpen(), is(false));		
	}

	private interface HttpConnectionTestBody {
		void apply(HttpURLConnection httpConnection) throws IOException, URISyntaxException;
	}
	
	@Test(timeout = 3000)
	public void httpMessageServletCanBePinged() throws PeerGroupException, IOException, URISyntaxException {
		mockContext.checking(new Expectations() {{
			oneOf(mockServletHttpContext).getEndpointService(); will(returnValue(mockEndpointService));
			oneOf(mockEndpointService).getGroup(); will(returnValue(mockPeerGroup));
			oneOf(mockPeerGroup).getPeerID(); will(returnValue(assignedPeerId));
		}});

		connect("", new HttpConnectionTestBody() {
			public void apply(HttpURLConnection httpConnection) throws IOException, URISyntaxException {
				// not sure if this is the recommended way of going from the
				// string received in the input stream, which is of the form
				// cbid-2FD774D5B372433D84F1BAF6098F73C05BBC13E206EB725BAD1AD059E49FC57F03
				// to a PeerID
				final String readFromStream = readFromStream(httpConnection.getInputStream(), httpConnection.getContentLength());
				final URI uri = new URI("urn:jxta:" + readFromStream);
				final PeerID receivedPeerId = (PeerID) IDFactory.fromURI(uri);
				
				LOG.info("Received peer Id '" + receivedPeerId + "'");
				assertThat(receivedPeerId, equalTo(assignedPeerId));
			}
		});
	}

	@Test(timeout = 3000)
	public void httpMessageServletCanBePolled() throws PeerGroupException, IOException, URISyntaxException {
		final PeerID requestorPeerId = IDFactory.newPeerID(assignedPeerGroupId);
		final EndpointAddress destinationAddress = null; // TODO define this
		// Requestor defined, positive response timeout and destination address.
		mockContext.checking(new Expectations() {{
			//oneOf(mockServletHttpContext).getEndpointService(); will(returnValue(mockEndpointService));
			//oneOf(mockEndpointService).getGroup(); will(returnValue(mockPeerGroup));
			//oneOf(mockPeerGroup).getPeerID(); will(returnValue(assignedPeerId));
		}});

		connect(requestorPeerId.toString() + "?500,600," + destinationAddress,
				new HttpConnectionTestBody() {			
			public void apply(final HttpURLConnection httpConnection) throws IOException, URISyntaxException {
				//TODO fail("test more stuff here");
			}
		}); 
	}

	private void connect(final String restOfURL, final HttpConnectionTestBody body) throws IOException,
			ProtocolException, URISyntaxException {
		// TODO if connected to a real network, getLocalHost gives real IP address. If
		// trying to use localhost here, connection fails... how can I force JXTA to
		// only use localhost (that should be sufficient for these tests?)
		final URL url = new URL("http://" + InetAddress.getLocalHost().getHostAddress() + ":" + port + "/" + restOfURL);
		final HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
		try {
			httpConnection.setRequestMethod("GET");
			httpConnection.setConnectTimeout(500);
			httpConnection.setReadTimeout(1000);
	
			LOG.info("Connecting to " + url);
			httpConnection.connect();
			LOG.info("Connected");
			
			body.apply(httpConnection);
		} finally {
			LOG.info("Disconnecting");
			httpConnection.disconnect();
		} 	
	}

	
	private String readFromStream(final InputStream inputStream, final int len)
			throws IOException {
		BufferedReader reader = null;
		try {
			char buf[] = new char[len];
			reader = new BufferedReader(new InputStreamReader(inputStream));
			return new String(buf, 0, reader.read(buf, 0, len));
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException ioe) {
				LOG.warn("Could not close HttpURLConnection", ioe);
			}
		}
	}

	private Boolean portOpen() {
		Socket socket = null;
		try {
			socket = new Socket(InetAddress.getLocalHost(), port);
			final boolean connected = socket.isConnected();
			LOG.info(connected ? "Connected to port " + port : "Could not connect to port " + port);
			return connected;
		} catch (ConnectException ce) {
			LOG.info("Connection refused");
			return false;
		} catch (IOException ioe) {
			LOG.warn("IOException while setting up socket", ioe);
		} finally {
			if (socket != null) {
				try {
					LOG.info("Closing connection");
					socket.close();
				} catch (IOException ioe) {
					LOG.warn("IOException on close", ioe);
				}
			}
		}
		return false;
	}

}
