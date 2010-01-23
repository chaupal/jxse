package net.jxta.impl.cm.sql;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;

import javax.sql.ConnectionPoolDataSource;

import org.h2.jdbcx.JdbcDataSource;

public class H2AdvertisementCache extends JdbcAdvertisementCache {
	public H2AdvertisementCache(URI storeRoot, String areaName) throws IOException {
		super(storeRoot, areaName);
	}
	
	public H2AdvertisementCache(URI storeRoot, String areaName, long gcinterval, boolean trackDeltas) throws IOException {
		super(storeRoot, areaName, gcinterval, trackDeltas);
	}
	
	@Override
	protected ConnectionPoolDataSource createDataSource() {
		if(!loadDbDriver("org.h2.Driver")) {
			throw new RuntimeException("Unable to loadDB driver: org.h2.Driver");
		}
		JdbcDataSource source = new JdbcDataSource();
		source.setURL("jdbc:h2:" + dbDir.getAbsolutePath());
		return source;
	}
	
	@Override
	protected void shutdownDb() throws SQLException {
		// no special shutdown is required for H2
	}

}
