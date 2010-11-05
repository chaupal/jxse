package net.jxse.systemtests.colocated;

import net.jxta.util.JxtaBiDiPipe;

/**
 * Simple class to store the two ends of an established pipe.
 */
public class PipeEnds {

	public JxtaBiDiPipe acceptedEnd;
	public JxtaBiDiPipe clientEnd;
	
	public PipeEnds(JxtaBiDiPipe acceptedEnd, JxtaBiDiPipe clientEnd) {
		this.acceptedEnd = acceptedEnd;
		this.clientEnd = clientEnd;
	}
	
}
