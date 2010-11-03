package net.jxta.impl.cm.bdb;

import com.sleepycat.je.DatabaseEntry;

public class AttributeSearchKey {

    private String areaName;
    private String directoryName;
    private String attributeName;
    private String value;

    public AttributeSearchKey(String areaName, String directoryName, String attributeName, String value) {
        this.areaName = areaName;
        this.directoryName = directoryName;
        this.attributeName = attributeName;
        this.value = value;
    }

    public AttributeSearchKey(String areaName, String directoryName, String attributeName) {
        this(areaName, directoryName, attributeName, null);
    }

    public AttributeSearchKey(String areaName, String directoryName) {
        this(areaName, directoryName, null, null);
    }

    public String getAreaName() {
        return areaName;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getValue() {
        return value;
    }

    public DatabaseEntry toDatabaseEntry(DatabaseEntry entry) {
        new AttributeSearchKeyTupleBinding().objectToEntry(this, entry);
        return entry;
    }

    public DatabaseEntry toDatabaseEntry() {
        return toDatabaseEntry(new DatabaseEntry());
    }

    public static AttributeSearchKey fromDatabaseEntry(DatabaseEntry entry) {
        return new AttributeSearchKeyTupleBinding().entryToObject(entry);
    }

}
