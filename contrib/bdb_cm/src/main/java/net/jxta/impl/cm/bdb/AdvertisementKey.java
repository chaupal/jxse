package net.jxta.impl.cm.bdb;

import com.sleepycat.je.DatabaseEntry;

public class AdvertisementKey {

    private String areaName;
    private String directoryName;
    private String fileName;
    
    public AdvertisementKey(String areaName, String directoryName, String fileName) {
        this.areaName = areaName;
        this.directoryName = directoryName;
        this.fileName = fileName;
    }
    
    public AdvertisementKey(String areaName, String directoryName) {
        this(areaName, directoryName, null);
    }
    
    public String getAreaName() {
        return areaName;
    }
    
    public String getDirectoryName() {
        return directoryName;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public DatabaseEntry toDatabaseEntry(DatabaseEntry entry) {
        new AdvertisementKeyTupleBinding().objectToEntry(this, entry);
        return entry;
    }
    
    public DatabaseEntry toDatabaseEntry() {
        return toDatabaseEntry(new DatabaseEntry());
    }
    
    public static AdvertisementKey fromDatabaseEntry(DatabaseEntry entry) {
        return new AdvertisementKeyTupleBinding().entryToObject(entry);
    }
}
