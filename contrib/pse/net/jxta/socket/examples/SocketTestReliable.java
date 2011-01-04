/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.jxta.socket.examples;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.platform.NetworkManager.ConfigMode;
import net.jxta.socket.JxtaServerSocket;
import net.jxta.socket.JxtaSocket;

/**
 *
 * @author nick
 */
public class SocketTestReliable {

    private static File tempStorage;
    protected NetworkManager rdvManager;
    protected NetworkManager aliceManager;
    protected NetworkManager bobManager;

    private String referenceDataFileName = "referenceDataFileName";
    private String streamedDataFileName = "streamedDataFileName";

    public static void main(String[] args) {
        try {
            SocketTestReliable t = new SocketTestReliable();
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
            rmdir(tempStorage);
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
            SecureRandom secureRandom = new SecureRandom();

//    private String referenceDataFileName = "referenceDataFileName";
//    private String streamedDataFileName = "streamedDataFileName";
            byte[] buf = new byte[3000];
            File referenceDataFile = new File(referenceDataFileName);
            FileOutputStream fos = new FileOutputStream(referenceDataFile);
System.err.println("current time : " + new Date());
            for (int i=0;i<1000;i++) {
                secureRandom.nextBytes(buf);
                fos.write(buf);
                fos.flush();
            }
            fos.close();
System.err.println("referenceDataFileName generated at : " + new Date());

            JxtaServerSocketRunnable serverSocketRunnable = new JxtaServerSocketRunnable(true);
            Thread thread = new Thread(serverSocketRunnable);
            thread.start();


            //JxtaSocket clientSocket = new JxtaSocket(PeerGroup group, PeerID peerid, PipeAdvertisement pipeAdv, int timeout, boolean reliable);
            JxtaSocket clientSocket=null;
            try {
            clientSocket = new JxtaSocket(bobManager.getNetPeerGroup(), aliceManager.getPeerID(), serverSocketRunnable.getJxtaServerSocket().getPipeAdv(), 5000, true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
System.out.println("got first client socket at : " + new Date() + ", clientSocket = " + clientSocket);

            FileInputStream referenceDataFileInputStream = new FileInputStream(referenceDataFileName);
            OutputStream os = clientSocket.getOutputStream();
            int len;
            while((len=referenceDataFileInputStream.read(buf))>=0) {
                os.write(buf, 0, len);
                os.flush();
            }
            os.close();
            clientSocket.close();

            referenceDataFileInputStream.close();
//            if (!clientSocket.isClosed())
//                clientSocket.close();
System.err.println("client socket finished sending file at : " + new Date());

            //buf

            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    class JxtaServerSocketRunnable implements Runnable {

        private boolean encrypt;
        private JxtaServerSocket serverSocket;

        JxtaServerSocketRunnable(boolean encrypt) throws IOException {
            this.encrypt = encrypt;
            serverSocket = SystemTestUtils.createServerSocket(aliceManager, encrypt);
System.err.println("JxtaServerSocketRunnable run 1");
        }

        public void run() {
            while(!serverSocket.isClosed()) {
                try {
System.err.println("JxtaServerSocketRunnable run 1");
                    JxtaSocket jxtaSocket = (JxtaSocket) serverSocket.accept();
System.err.println("JxtaServerSocketRunnable run 2");
                    processSocket(jxtaSocket);
                } catch (IOException ex) {
                    Logger.getLogger(SocketTestReliable.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        JxtaServerSocket getJxtaServerSocket() {
            return serverSocket;
        }

        void close() throws IOException {
            serverSocket.close();
        }

        private void processSocket(JxtaSocket jxtaSocket) throws IOException {
System.err.println("got server socket at : " + new Date());

            byte[] streamedbuf = new byte[3000];
            File streamedDataFile = new File(streamedDataFileName);
            FileOutputStream fos = new FileOutputStream(streamedDataFile);
            InputStream is = jxtaSocket.getInputStream();
            int streamedlen;
            while((streamedlen=is.read(streamedbuf))>=0) {
                fos.write(streamedbuf, 0, streamedlen);
            }
            fos.close();
            is.close();
//            jxtaSocket.close();
System.err.println("server socket fisished reading data at : " + new Date());

            boolean dataCorrect = true;

            FileInputStream referenceDataFileInputStream = new FileInputStream(referenceDataFileName);
            FileInputStream streamedDataFileInputStream = new FileInputStream(streamedDataFileName);

            byte[] referencebuf = new byte[3000];
            
            while((streamedlen=streamedDataFileInputStream.read(streamedbuf))>=0) {
                int referencelen = referenceDataFileInputStream.read(referencebuf);
                if (referencelen != streamedlen) {
                    dataCorrect = false;
System.err.println("server socket referencelen != streamedlen, referencelen = " + referencelen + ", streamedlen = " + streamedlen);
                }
            }

            if (referenceDataFileInputStream.read() != -1) {
System.err.println("referenceDataFileInputStream.read() != -1");
                    dataCorrect = false;
            }

            if (streamedDataFileInputStream.read() != -1) {
System.err.println("streamedDataFileInputStream.read() != -1");
                    dataCorrect = false;
            }

            referenceDataFileInputStream.close();
            streamedDataFileInputStream.close();
            
System.err.println("server socket fisished comparing data at : " + new Date() + ", dataCorrect = " + dataCorrect);
            
        }

    }

    public static void rmdir(File dir) throws IOException {
        if (null == dir)
            return;
        if (!dir.exists())
            return;
        File[] children = dir.listFiles();
        if (children!=null)
            for (int i=0; i<children.length; i++) {
                if(children[i].isFile())
                    children[i].delete();
                else if (children[i].isDirectory())
                    rmdir(children[i]);
            }
        dir.delete();
    }

    public void initPeers() throws Exception {
        rdvManager = new NetworkManager(ConfigMode.RENDEZVOUS, "rdv", (new File(tempStorage, "rdv")).toURI());
        configureForTcp(rdvManager, 57080, true);
        aliceManager = new NetworkManager(ConfigMode.ADHOC, "alice", (new File(tempStorage, "alice")).toURI());
        configureForTcp(aliceManager, 59080, false);
        bobManager = new NetworkManager(ConfigMode.ADHOC, "bob", (new File(tempStorage, "bob")).toURI());
        configureForTcp(bobManager, 58081, false);

        Object object = new Object();
        synchronized(object) {
            rdvManager.startNetwork();
            object.wait(2000);
            aliceManager.startNetwork();
            object.wait(2000);
            bobManager.startNetwork();
            object.wait(2000);
        }

        System.out.println("peerid rdvManager " + rdvManager.getNetPeerGroup().getPeerID());
        System.out.println("peerid aliceManager " + aliceManager.getNetPeerGroup().getPeerID());
        System.out.println("peerid bobManager " + bobManager.getNetPeerGroup().getPeerID());
        
        System.out.println("rdv is reachable from alice " + ((net.jxta.impl.endpoint.EndpointServiceImpl)aliceManager.getNetPeerGroup().getEndpointService()).isReachable(rdvManager.getPeerID(), true));
        System.out.println("rdv is reachable from bob " + ((net.jxta.impl.endpoint.EndpointServiceImpl)bobManager.getNetPeerGroup().getEndpointService()).isReachable(rdvManager.getPeerID(), true));

        System.out.println("bob is reachable from alice " + ((net.jxta.impl.endpoint.EndpointServiceImpl)aliceManager.getNetPeerGroup().getEndpointService()).isReachable(bobManager.getPeerID(), true));
        System.out.println("alice is reachable from bob " + ((net.jxta.impl.endpoint.EndpointServiceImpl)bobManager.getNetPeerGroup().getEndpointService()).isReachable(aliceManager.getPeerID(), true));


    }

    private void configureForTcp(NetworkManager manager, int port, boolean isRdv) throws IOException {
		NetworkConfigurator configurator = manager.getConfigurator();
		configurator.setTcpEnabled(true);
		configurator.setHttpEnabled(false);
		configurator.setHttp2Enabled(false);

		configurator.setTcpIncoming(true);
		configurator.setTcpOutgoing(true);
		configurator.setTcpPort(port);
		configurator.setTcpStartPort(port);
		configurator.setTcpEndPort(port+100);

                configurator.setUseMulticast(true);

                if (!isRdv) {
                    java.util.HashSet rendezvoussHashSet = new java.util.HashSet();
                    String rendezvous = "tcp://10.0.0.12:"+57080;
                    rendezvoussHashSet.add(rendezvous);
                    configurator.setRendezvousSeeds(rendezvoussHashSet);
                }
	}

    public void killPeers() throws Exception {
        aliceManager.stopNetwork();
        bobManager.stopNetwork();
    }

}
