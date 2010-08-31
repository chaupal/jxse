package net.jxta.document;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.Hashtable;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Ignore("Investigate should this not be a trival case if still required?")
public class AdvertisementSerializableTest{
	private static final String TestDescription = "Testing Advertisement Serializable";
	
	private static final String testPipeID = "urn:jxta:uuid-EA8D0447E3EB4BB58D8A81AF06DFA88E297664E3EC6248CFAE50FA285ADA12AB04";
	private static final String testPeerID = "urn:jxta:uuid-59616261646162614A787461503250336ACC981CFAF047CFADA8A31FC6D0B88C03";
	private static final String testPeerGroupID = "urn:jxta:uuid-EA8D0447E3EB4BB58D8A81AF06DFA88E02";
	
	private static final String testMSID = "urn:jxta:uuid-4CD1574ABA614A5FA242B613D8BAA30FD0A45F5F0E1A450DA912BB01585AB0FC06";
	private static final String testMCID = "urn:jxta:uuid-4CD1574ABA614A5FA242B613D8BAA30F05";

    
    private void toSerialize(Advertisement adv,String filepath) throws Exception{
    	System.out.println(adv);
    	FileOutputStream out = new FileOutputStream(filepath);
		ObjectOutputStream oo = new ObjectOutputStream(out);
		oo.writeObject(adv);
		oo.close();
    }
    
    private Advertisement toDeserialize(String filepath) throws Exception{
    	FileInputStream in = new FileInputStream(filepath);
		ObjectInputStream oi = new ObjectInputStream(in);
		PipeAdvertisement reAdv = (PipeAdvertisement)oi.readObject();
		oi.close();
		return reAdv;
    }
    
    private PipeAdvertisement getPipeAdvertisement(PipeID pipeid){
    	PipeAdvertisement pipeAdv = (PipeAdvertisement)AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
    	pipeAdv.setPipeID(pipeid);
    	pipeAdv.setType(PipeService.UnicastType);
		pipeAdv.setName("test Pipe");
		pipeAdv.setDescription("test Pipe Advertisement Description");
		pipeAdv.setDesc(buildDesc());
    	return pipeAdv;
    }
    private PeerAdvertisement getPeerAdvertisement(PeerGroupID pgid,PeerID pid,ID serviceid){
    	PeerAdvertisement peerAdv = (PeerAdvertisement)AdvertisementFactory.newAdvertisement(PeerAdvertisement.getAdvertisementType());
    	peerAdv.setPeerID(pid);
    	peerAdv.setPeerGroupID(pgid);
    	peerAdv.setDescription("test Peer Advertisement Description");
    	peerAdv.setName("test peer");
    	peerAdv.setDesc(buildDesc());
    	peerAdv.setServiceParams(buildService(serviceid));
    	return peerAdv;
    }
    private PeerGroupAdvertisement getPeerGroupAdvertisement(PeerGroupID pgid,ModuleSpecID msid,ID serviceid){
    	PeerGroupAdvertisement peergroupAdv = (PeerGroupAdvertisement)AdvertisementFactory.newAdvertisement(PeerGroupAdvertisement.getAdvertisementType());
    	peergroupAdv.setPeerGroupID(pgid);
    	peergroupAdv.setName("test peergroup");
    	peergroupAdv.setDescription("test peergroup Advertisement Description");
    	peergroupAdv.setDesc(buildDesc());
    	peergroupAdv.setModuleSpecID(msid);
    	peergroupAdv.setServiceParams(buildService(serviceid));
    	return peergroupAdv;
    }
    
    private Element buildDesc() {
        StructuredTextDocument desc = (StructuredTextDocument) StructuredDocumentFactory.newStructuredDocument(
                MimeMediaType.XMLUTF8, "Desc");

        desc.appendChild(desc.createElement("Text1", TestDescription));
        desc.appendChild(desc.createElement("Text2", TestDescription));
        desc.appendChild(desc.createElement("Text3", TestDescription));
        return desc;
    }
    private Hashtable<ID,Element> buildService(ID serviceid){
    	Hashtable<ID,Element> service = new Hashtable<ID,Element>();
    	service.put(serviceid,  buildDesc());
    	return service;
    }
    
    @Test public void testPipeAdv() throws Exception{
    	String fileName = "pipdAdv.dat";
    	PipeID pipeid = (PipeID)IDFactory.fromURI(new URI(testPipeID));
    	assertNotNull("IDFactory cannot convert PipeID", pipeid);
    	PipeAdvertisement pipeAdvc = getPipeAdvertisement(pipeid);
    	assertNotNull("Cannot get PipeAdvertisement", pipeAdvc);
    	
    	toSerialize(pipeAdvc,fileName);
    	
    	PipeAdvertisement pipeAdvl = (PipeAdvertisement)toDeserialize(fileName);
    	assertEquals("PipeAdvertisement Serialize is corrupted", pipeAdvc, pipeAdvl);
    }
    
    @Test public void testPeerAdv() throws Exception{
    	String fileName = "peerAdv.dat";
    	PeerID peerid = (PeerID)IDFactory.fromURI(new URI(testPeerID));
    	assertNotNull("IDFactory cannot convert PeerID", peerid);
    	
    	PeerGroupID pgid = (PeerGroupID)IDFactory.fromURI(new URI(testPeerGroupID));
    	assertNotNull("IDFactory cannot convert PeerGroupID", pgid);
    	
    	ID mcid = (ID)IDFactory.fromURI(new URI(testMCID));
    	assertNotNull("IDFactory cannot convert ServiceID", mcid);
    	
    	PeerAdvertisement peerAdvc = getPeerAdvertisement(pgid,peerid,mcid);
    	assertNotNull("Cannot get PeerAdvertisement", peerAdvc);
    	
    	toSerialize(peerAdvc,fileName);
    	
    	PeerAdvertisement peerAdvl = (PeerAdvertisement)toDeserialize(fileName);
    	assertEquals("PeerAdvertisement Serialize is corrupted", peerAdvc, peerAdvl);
    }
   
    @Test public void testPeerGroupAdv() throws Exception{
    	String fileName = "peergroupAdv.dat";

    	PeerGroupID pgid = (PeerGroupID)IDFactory.fromURI(new URI(testPeerGroupID));
    	assertNotNull("IDFactory cannot convert PeerGroupID", pgid);
    	
    	ModuleSpecID msid = (ModuleSpecID)IDFactory.fromURI(new URI(testMSID));
    	assertNotNull("IDFactory cannot convert msID", msid);
    	
    	ID mcid = (ID)IDFactory.fromURI(new URI(testMCID));
    	assertNotNull("IDFactory cannot convert MCID", mcid);
    	
    	PeerGroupAdvertisement peergroupAdvc = getPeerGroupAdvertisement(pgid,msid,mcid);
    	assertNotNull("Cannot get PeerGroupAdvertisement", peergroupAdvc);
    	
    	toSerialize(peergroupAdvc,fileName);
    	
    	PeerGroupAdvertisement peergroupAdvl = (PeerGroupAdvertisement)toDeserialize(fileName);
    	assertEquals("PeerGroupAdvertisement Serialize is corrupted", peergroupAdvc, peergroupAdvl);
    }
}
