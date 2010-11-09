/*
 * Copyright (c) 2002-2007 Sun Microsystems, Inc.  All rights reserved.
 *
 *  The Sun Project JXTA(TM) Software License
 *
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice, 
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *
 *  3. The end-user documentation included with the redistribution, if any, must 
 *     include the following acknowledgment: "This product includes software 
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must 
 *     not be used to endorse or promote products derived from this software 
 *     without prior written permission. For written permission, please contact 
 *     Project JXTA at http://www.jxta.org.
 *
 *  5. Products derived from this software may not be called "JXTA", nor may 
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN 
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United 
 *  States and other countries.
 *
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of 
 *  the license in source files.
 *
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */
package net.jxta.rendezvous;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Hashtable;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.impl.endpoint.IPUtils;
import net.jxta.impl.membership.none.NoneMembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.test.util.AdvUtil;
import net.jxta.test.util.JxtaSink;
import net.jxta.test.util.MessageUtil;
import net.jxta.test.util.TcpConnection;

/**
 * Rendezvous load test is intended to load a rendezvous peer with connection
 * requests, and test it's limits.
 *
 * Running Requirements:
 * Run a local rdv peer on the same host running the test
 * The peer must be configured with a tcp transport on port 9701
 * In addition $JXTA_HOME/jxta.properties must define RdvManager.MaxClients=#clients simulated
 */
public class RendezvousLoad extends TestCase {

    String incarnationTagName = "RdvIncarnjxta-NetGroup" + System.currentTimeMillis();
    private String myAddress = null;
    private int PORT = 8000;
    // 200 is the default max number of edge peers, modify this
    // after modifying $JXTA_HOME/jxta.properties RdvManager.MaxClients with the corresponding value
    private int ITERATIONS = 1000;
    private long NAP = 100;
    private String serviceParm = PeerGroupID.defaultNetPeerGroupID.getUniqueValue().toString();
    private String service = PeerGroup.rendezvousClassID.toString();

    /**
     *  Constructor for the RendezvousLoad
     *
     *@param  testName
     *@exception  Exception
     */
    public RendezvousLoad(String testName) throws Exception {
        super(testName);
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception
     */
    @Override
    protected void finalize() throws Exception {}

    /**
     *  The main program for the RendezvousLoad class
     *
     *@param  args  The command line arguments
     */
    public static void main(java.lang.String[] args) {
        TestRunner.run(suite());
        System.err.flush();
        System.out.flush();
    }

    /**
     *  A unit test suite for JUnit
     *
     *@return    The test suite
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(RendezvousLoad.class);

        return suite;
    }

    /**
     *  Load test rdv peer using a tcp configuration
     */
    public void testLoad() {
        TcpConnection connection = null;
        PeerID destPeerID = null;
        Hashtable sinkTBL = new Hashtable(ITERATIONS);
        Hashtable connectionTBL = new Hashtable(ITERATIONS);

        for (int i = 0; i < ITERATIONS; i++) {
            PeerAdvertisement padv = AdvUtil.newPeerAdv("Fakey" + i, getMyAddress(), PORT + i, false);
            EndpointAddress epa = new EndpointAddress("tcp", getMyAddress() + ":9701", null, null);

            try {
                connection = new TcpConnection(epa, getMyInetAddress(), padv.getPeerID(), null);
                destPeerID = (PeerID) connection.getDestinationPeerID();
                JxtaSink sink = new JxtaSink(getMyInetAddress(), PORT + i, connection.getWM());
                // sinkTBL.put(padv.getPeerID(), sink);
                // connectionTBL.put(padv.getPeerID(), connection);
            } catch (Exception e) {
                e.printStackTrace();
                fail("Could not create a connection to " + getMyInetAddress() + " rdv on port 9701");
            }
            String incNum = newIncarnation();
            Message connectMsg = MessageUtil.rdvConnectMessage(padv, incNum);

            MessageUtil.addServiceParam(connectMsg, padv, "tcp://" + getMyAddress() + ":" + (PORT + i), destPeerID, service, serviceParm, new NoneMembershipService());
            // MessageUtil.printMessageStats(connectMsg, true);
            try {
                connection.sendMessage(connectMsg);
                System.out.println("Connection request : " + i + " sent");
                Thread.sleep(NAP);
            } catch (Exception e) {
                e.printStackTrace();
                fail("Could not send Message to rdv");
            }
        }

    }

    private String getMyAddress() {
        if (myAddress == null) {
            myAddress = getMyInetAddress().getHostAddress();
        }
        return myAddress;
    }

    private InetAddress getMyInetAddress() {
        Iterator<InetAddress> en = IPUtils.getAllLocalAddresses().iterator();

        while (en.hasNext()) {
            return (InetAddress) en.next();
        }
        return null;
    }

    public String newIncarnation() {

        return Long.toString(System.currentTimeMillis());
    }

    public static void fail(String message) {
        junit.framework.TestCase.fail(message);
    }
}
