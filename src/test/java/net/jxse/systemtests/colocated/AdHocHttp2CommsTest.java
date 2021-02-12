package net.jxse.systemtests.colocated;

import net.jxse.systemtests.colocated.configs.PeerConfigurator;
import net.jxta.exception.PeerGroupException;
import net.jxta.platform.NetworkManager;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests ad-hoc mode communication using the HTTP2 transport. This test exists within
 * {@link AdHocCommsTest} however due to peer isolation issues it must currently
 * be run separately - placing it in it's own class ensures it is run in a forked
 * VM.
 */
public class AdHocHttp2CommsTest {

	@Rule
	public TemporaryFolder tempStorage = new TemporaryFolder();
	
	private NetworkManager aliceManager;
	private NetworkManager bobManager;
	
    private ExecutorService service;


	@Before
	public void createPeers() throws Exception {
        service = Executors.newCachedThreadPool();
		aliceManager = PeerConfigurator.createHttp2AdhocPeer("alice", 58000, tempStorage);
		bobManager = PeerConfigurator.createHttp2AdhocPeer("bob", 58001, tempStorage);

		try {
			aliceManager.startNetwork();
			bobManager.startNetwork();
		} catch (PeerGroupException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		// XXX: give the network managers time to stabilise
		Thread.sleep(5000L);
	}
	
	@Test
	public void testComms() throws Exception {
		service.execute(()-> onTestpeerCommunication());
	}
	
	protected void onTestpeerCommunication() {
		try {
			SystemTestUtils.testPeerCommunication(aliceManager, bobManager);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@After
	public void tearDown() throws Exception {
		service.shutdown();
		try {
			service.awaitTermination(120, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			fail( e.getMessage());
		}
		finally {
			aliceManager.stopNetwork();
			bobManager.stopNetwork();
		}
	}	
}