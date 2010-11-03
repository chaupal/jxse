package net.jxta.impl.cm.bdb;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class AttributeSearchKeyTupleBinding extends TupleBinding<AttributeSearchKey> {

    @Override
    public AttributeSearchKey entryToObject(TupleInput input) {
        String areaName = input.readString();
        String directoryName = input.readString();
        String attributeName = input.readString();
        String value = input.readString();

        return new AttributeSearchKey(areaName, directoryName, attributeName, value);
    }

    @Override
    public void objectToEntry(AttributeSearchKey object, TupleOutput output) {
        output.writeString(object.getAreaName());
        output.writeString(object.getDirectoryName());
        if(object.getAttributeName() != null) {
            output.writeString(object.getAttributeName());
            if(object.getValue() != null) {
                output.writeString(object.getValue());
            }
        }
    }

}
