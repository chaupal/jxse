package net.jxta.impl.cm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.jxta.document.Advertisement;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.protocol.SrdiMessage.Entry;

public class DeltaTracker {

	private Map<String,List<Entry>> deltas = new HashMap<String, List<Entry>>();
	private boolean trackingDeltas = false;
	
	public void setTrackingDeltas(boolean trackDeltas) {
		this.trackingDeltas = trackDeltas;
	}
	
	public boolean isTrackingDeltas() {
		return trackingDeltas;
	}
	
	public void clearDeltas(String dn) {
		deltas.remove(dn);
	}
	
	public List<Entry> getDeltas(String dn) {
        List<Entry> currentDeltas = deltas.get(dn);
        if(currentDeltas == null) {
            currentDeltas = new ArrayList<Entry>(0);
        }
        clearDeltas(dn);
        return currentDeltas;
    }
	
	public void generateDeltas(String dn, Advertisement adv, StructuredDocument<?> doc, long expiry) {
		if(!trackingDeltas || expiry <= 0) {
			return;
		}
		
		if(doc == null) {
			doc = (StructuredDocument<?>)adv.getDocument(MimeMediaType.XMLUTF8);
		}
		
		Map<String, String> indexFields = CacheUtils.getIndexfields(adv.getIndexFields(), doc);
		List<Entry> deltasForDn = deltas.get(dn);
		if(deltasForDn == null) {
		    deltasForDn = new LinkedList<Entry>();
		}
		
		for(String indexField : indexFields.keySet()) {
			deltasForDn.add(new Entry(indexField, indexFields.get(indexField), expiry));
		}
		
		deltas.put(dn, deltasForDn);
	}
	
}
