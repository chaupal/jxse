package net.jxse.systemtests.colocated;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.jxta.endpoint.Message;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.platform.NetworkManager;

public class LoadTester {

	public static void loadTestSinglePipe(NetworkManager alice, NetworkManager bob, int numMessages, int messageSize) throws Exception {
		
		final AtomicBoolean sendComplete = new AtomicBoolean(false);
		final AtomicInteger numReceived = new AtomicInteger(0);
		final CountDownLatch receiveComplete = new CountDownLatch(1);
		
		final ConcurrentSkipListSet<Integer> messagesToReceive = new ConcurrentSkipListSet<Integer>();
		
		PipeMsgListener aliceListener = new PipeMsgListener() {
			public void pipeMsgEvent(PipeMsgEvent event) {
				if(event.getMessage() != null) {
					int received = numReceived.incrementAndGet();
					if(received % 100 == 0) {
						System.out.println("Received " + received + " messages");
					}
					int msgNum = Integer.parseInt(SystemTestUtils.getMessageString(event.getMessage()));
					messagesToReceive.remove(msgNum);
					
					if(sendComplete.get() && messagesToReceive.isEmpty()) {
						receiveComplete.countDown();
					}
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
			messagesToReceive.add(i);
			while(ends.clientEnd.sendMessage(sentMessage) != true) {
				System.out.print(".");
				Thread.yield();
			}
		}
		
		sendComplete.set(true);
		
		receiveComplete.await(15L, TimeUnit.SECONDS);
	}
	
}
