/*
 * Copyright (c) 2006-2007 Sun Microsystems, Inc.  All rights reserved.
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
package tutorial.multicast;

import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkManager;
import net.jxta.socket.JxtaMulticastSocket;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Date;

/**
 * Simple example to illustrate the use of JxtaMulticastSocket
 */
public class JxtaMulticastSocketClient {

    /**
     * main
     *
     * @param args command line args
     */
    public static void main(String args[]) {
        NetworkManager manager = null;

        try {
            manager = new net.jxta.platform.NetworkManager(NetworkManager.ConfigMode.ADHOC, "JxtaMulticastSocketClient",
                    new File(new File(".cache"), "JxtaMulticastSocketClient").toURI());
            manager.startNetwork();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        PeerGroup netPeerGroup = manager.getNetPeerGroup();

        JxtaMulticastSocket mcastSocket = null;

        try {
            mcastSocket = new JxtaMulticastSocket(netPeerGroup, JxtaMulticastSocketServer.getSocketAdvertisement());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        Date date = new Date(System.currentTimeMillis());
        String hello = "Hello on : " + date.toString();
        try {
            DatagramPacket packet = new DatagramPacket(hello.getBytes(), hello.length());

            mcastSocket.send(packet);
            byte[] res = new byte[16384];
            DatagramPacket rpacket = new DatagramPacket(res, res.length);

            // It's likely we'll receive 2 packets a loopback and a response
            // loopback
            mcastSocket.receive(rpacket);
            // server response
            mcastSocket.receive(rpacket);
            String sw = new String(rpacket.getData(), 0, rpacket.getLength());

            System.out.println("Received data from :" + rpacket.getAddress());
            System.out.println(sw);
            // stop the platform
            manager.stopNetwork();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

