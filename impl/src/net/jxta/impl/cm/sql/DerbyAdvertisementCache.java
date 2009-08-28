package net.jxta.impl.cm.sql;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;

import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;

public class DerbyAdvertisementCache extends JdbcAdvertisementCache {

	public DerbyAdvertisementCache(URI storeRoot, String areaName) throws IOException {
		super(storeRoot, areaName);
	}
	
	public DerbyAdvertisementCache(URI storeRoot, String areaName, long gcinterval, boolean trackDeltas) throws IOException {
		super(storeRoot, areaName, gcinterval, trackDeltas);
	}
	
	@Override
	protected EmbeddedConnectionPoolDataSource createDataSource() {
		EmbeddedConnectionPoolDataSource dataSource = new EmbeddedConnectionPoolDataSource();
		dataSource.setDatabaseName(dbDir.getAbsolutePath());
		dataSource.setCreateDatabase("create");
		return dataSource;
	}
	
	@Override
	protected void shutdownDb() throws SQLException {
		// annoyingly, shutting down a derby instance involves catching an exception
		// and checking error codes to make sure it shut down "normally"
		
		try {
			EmbeddedDataSource dataSource = new EmbeddedDataSource();
			dataSource.setDatabaseName(dbDir.getAbsolutePath());
			dataSource.setShutdownDatabase("shutdown");
			dataSource.getConnection();
		} catch(SQLException e) {
			// make sure we get the correct error codes 
			if(e.getErrorCode() != 45000 || !"08006".equals(e.getSQLState())) {
				throw e;
			}
		}
	}
	
}
