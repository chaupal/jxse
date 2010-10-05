/**
 * 
 */
package net.jxta.impl.cm.bdb;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

/**
 * Responsible for converting {@link AdvertisementDbRecord} to/from
 * {@link com.sleepycat.je.DatabaseEntry}.
 */
class AdvertisementDbRecordTupleBinding extends TupleBinding<AdvertisementDbRecord> {

	@Override
	public AdvertisementDbRecord entryToObject(TupleInput input) {
		AdvertisementDbRecord r = new AdvertisementDbRecord();
		int dataSize = input.readInt();
		byte[] bytes = new byte[dataSize];
		input.readFast(bytes, 0, bytes.length);
		r.data = bytes;
		r.lifetime = input.readLong();
		r.expiration = input.readLong();
		r.isAdvertisement = input.readBoolean();
		
		return r;
	}

	@Override
	public void objectToEntry(AdvertisementDbRecord object, TupleOutput output) {
		output.writeInt(object.data.length);
		output.writeFast(object.data);
		output.writeLong(object.lifetime);
		output.writeLong(object.expiration);
		output.writeBoolean(object.isAdvertisement);
	}
	
}