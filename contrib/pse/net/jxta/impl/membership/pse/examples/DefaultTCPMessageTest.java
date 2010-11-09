/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.jxta.impl.membership.pse.examples;

import java.io.File;
import java.io.IOException;

import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.platform.NetworkManager.ConfigMode;

/**
 * 
 */
public class DefaultTCPMessageTest {

    private static File tempStorage;
    protected NetworkManager aliceManager;
    protected NetworkManager bobManager;

    DefaultTCPMessageTest() {
        
    }

    public static void main(String[] args) {
        try {
            DefaultTCPMessageTest t = new DefaultTCPMessageTest();
            t.init();
            t.run();
            t.end();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void init() {

System.setProperty("net.jxta.logging.Logging", "OFF");
System.setProperty("net.jxta.level", "OFF");

        try {
            SystemTestUtils.rmdir(tempStorage);
            tempStorage = new File("tempStorage");
            tempStorage.mkdir();
            initPeers();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }

    protected void end() {
        try {
            killPeers();
            SystemTestUtils.rmdir(tempStorage);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void run() {
        try {
            testColocatedPeerBidiPipeComms(false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void testColocatedPeerBidiPipeComms(boolean secure) throws Exception {
        SystemTestUtils.testPeerCommunication(aliceManager, bobManager, secure);
    }
    
    public void initPeers() throws Exception {
        aliceManager = new NetworkManager(ConfigMode.ADHOC, "alice", (new File(tempStorage, "alice")).toURI());
        configureForTcp(aliceManager, 59080);
        bobManager = new NetworkManager(ConfigMode.ADHOC, "bob", (new File(tempStorage, "bob")).toURI());
        configureForTcp(bobManager, 58081);

        aliceManager.startNetwork();
        bobManager.startNetwork();
    }

    private void configureForTcp(NetworkManager manager, int port) throws IOException {
		NetworkConfigurator configurator = manager.getConfigurator();
		configurator.setTcpEnabled(true);
		configurator.setHttpEnabled(false);
		configurator.setHttp2Enabled(false);

		configurator.setTcpIncoming(true);
		configurator.setTcpOutgoing(true);
		configurator.setTcpPort(port);
		configurator.setTcpStartPort(port);
		configurator.setTcpEndPort(port+100);
	}
    
    public void killPeers() throws Exception {
        aliceManager.stopNetwork();
        bobManager.stopNetwork();
    }

}
