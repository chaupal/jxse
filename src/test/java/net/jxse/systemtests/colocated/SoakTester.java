package net.jxse.systemtests.colocated;

import static junit.framework.Assert.*;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import net.jxta.endpoint.Message;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.platform.NetworkManager;

public class SoakTester {
	
	public static void soakTestSinglePipe(NetworkManager alice, NetworkManager bob, int numMessages, int messageSize) throws Exception {
		final LinkedBlockingQueue<Message> aliceReceived = new LinkedBlockingQueue<Message>();
		PipeMsgListener aliceListener = new PipeMsgListener() {
			public void pipeMsgEvent(PipeMsgEvent event) {
				if(event.getMessage() != null) {
					aliceReceived.offer(event.getMessage());
				}
			}
		};
		
		PipeMsgListener bobListener = new PipeMsgListener() {
			public void pipeMsgEvent(PipeMsgEvent event) {
				// we are not sending any messages to bob, so simply discard
			}
		};
		
		PipeEnds ends = SystemTestUtils.createBiDiPipe(alice, bob, aliceListener, bobListener, 15L, TimeUnit.SECONDS);
		
		
		for(int i=0; i < numMessages; i++) {
			if(i % 100 == 0) {
				System.out.println("Sending message " + (i+1) + " of " + numMessages);
			}
			
			Message sentMessage = SystemTestUtils.createMessage("" + i, messageSize);
			ends.clientEnd.sendMessage(sentMessage);
			Message receivedMessage = aliceReceived.poll(2, TimeUnit.SECONDS);
			if(receivedMessage == null) {
				fail("failed to receive message number " + i + " in a timely manner");
			}
			
			SystemTestUtils.checkMessagesEqual(sentMessage, receivedMessage);
		}
	}
	
}