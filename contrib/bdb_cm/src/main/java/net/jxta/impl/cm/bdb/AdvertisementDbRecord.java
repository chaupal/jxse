/**
 * 
 */
package net.jxta.impl.cm.bdb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import net.jxta.document.Advertisement;
import net.jxta.document.Document;
import net.jxta.document.MimeMediaType;
import net.jxta.impl.cm.CacheUtils;
import net.jxta.impl.util.TimeUtils;

import com.sleepycat.je.DatabaseEntry;

/**
 * Represents the data that is stored in the BDB database against
 * the areaName/dn/fn key. As the database can be used to store
 * both advertisements and raw binary data, the two are distinguished
 * in this object to ensure that secondary databases can be populated
 * correctly.
 */
class AdvertisementDbRecord {
	
	byte[] data;
	long expiration;
	long lifetime;
	
	/**
	 * Used to distinguish whether this record was used to store a real advertisement,
	 * or just binary data (in JXSE 2.5, encryption keys get stored in the advertisement cache).
	 */
	public boolean isAdvertisement;
	
	public AdvertisementDbRecord() {}
	
	public AdvertisementDbRecord(Advertisement adv, long lifetimeAsRelative, long expirationAsRelative) throws IOException {
		this.data = getBytesForAdvert(adv);
		this.lifetime = TimeUtils.toAbsoluteTimeMillis(lifetimeAsRelative);
		this.expiration = expirationAsRelative;
		this.isAdvertisement = true;
	}
	
	
	public AdvertisementDbRecord(byte[] data, long lifetime, long expiration, boolean isAdvertisement) {
		this.data = data;
		this.lifetime = lifetime;
		this.expiration = expiration;
		this.isAdvertisement = isAdvertisement;
	}
	
	public DatabaseEntry toDataEntry() {
		AdvertisementDbRecordTupleBinding binding = new AdvertisementDbRecordTupleBinding();
		DatabaseEntry dataEntry = new DatabaseEntry();
		binding.objectToEntry(this, dataEntry);
		return dataEntry;
	}
	
	public static AdvertisementDbRecord fromDataEntry(DatabaseEntry entry) {
		AdvertisementDbRecordTupleBinding binding = new AdvertisementDbRecordTupleBinding();
		return binding.entryToObject(entry);
	}
	
	public long getExpirationTime() {
		return CacheUtils.getRelativeExpiration(lifetime, expiration);
	}

	public byte[] getData() {
		return data;
	}
	
	private byte[] getBytesForAdvert(Advertisement adv) throws IOException {
		Document doc = adv.getDocument(MimeMediaType.XMLUTF8);
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream(2048);
		doc.sendToStream(byteStream);
		return byteStream.toByteArray();
	}
}