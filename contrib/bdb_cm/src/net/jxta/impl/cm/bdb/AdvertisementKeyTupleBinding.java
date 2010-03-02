package net.jxta.impl.cm.bdb;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class AdvertisementKeyTupleBinding extends TupleBinding<AdvertisementKey> {

    @Override
    public AdvertisementKey entryToObject(TupleInput input) {
        String areaName = input.readString();
        String directoryName = input.readString();
        String fileName = input.readString();
        return new AdvertisementKey(areaName, directoryName, fileName);
    }

    @Override
    public void objectToEntry(AdvertisementKey object, TupleOutput output) {
        output.writeString(object.getAreaName());
        output.writeString(object.getDirectoryName());
        
        if(object.getFileName() != null) {
            output.writeString(object.getFileName());
        }
    }

}
