package net.jxta.impl.cm.bdb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.impl.cm.CacheUtils;
import net.jxta.logging.Logging;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryMultiKeyCreator;

/**
 * Generates search keys against {@link AdvertisementDbRecord} if the record
 * is an advertisement, making it possible to search based on the indexable
 * fields of that advertisement.
 */
public class AdvertisementIndexableKeyCreator implements SecondaryMultiKeyCreator {

	private static final Logger LOG = Logger.getLogger(AdvertisementIndexableKeyCreator.class.getName());
	
	private AdvertisementKeyTupleBinding keyBinding = new AdvertisementKeyTupleBinding();
	private AdvertisementDbRecordTupleBinding binding = new AdvertisementDbRecordTupleBinding();
	
	public void createSecondaryKeys(SecondaryDatabase secondary,
			DatabaseEntry key, DatabaseEntry data, Set<DatabaseEntry> results)
			throws DatabaseException {
		
	    AdvertisementKey advKey = keyBinding.entryToObject(key);
	    String areaName = advKey.getAreaName();
	    String directoryName = advKey.getDirectoryName();

	    AdvertisementDbRecord record = binding.entryToObject(data);
		if(!record.isAdvertisement) {
			// we do not create any secondary keys for raw binary data that is cached
			return;
		}
		
		try {
			XMLDocument<?> asDoc = (XMLDocument<?>) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, new ByteArrayInputStream(record.getData()));
	        Advertisement adv = AdvertisementFactory.newAdvertisement(asDoc);
	        Map<String, String> indexfields = CacheUtils.getIndexfields(adv.getIndexFields(), asDoc);
	        
	        for(String indexField : indexfields.keySet()) {
                AttributeSearchKey newSearchKey = new AttributeSearchKey(areaName, directoryName, indexField, indexfields.get(indexField));
                results.add(newSearchKey.toDatabaseEntry());
	        }
		} catch(IOException e) {
			if(Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
				LOG.log(Level.SEVERE, "Unable to parse XML for advertisement that is being added to the DB!", e);
			}
		}
	}
}
