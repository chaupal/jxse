package net.jxta.impl.cm.sql;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.ConnectionPoolDataSource;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Document;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.impl.cm.AbstractAdvertisementCache;
import net.jxta.impl.cm.CacheUtils;
import net.jxta.impl.cm.DeltaTracker;
import net.jxta.impl.util.TimeUtils;
import net.jxta.logging.Logging;
import net.jxta.protocol.SrdiMessage.Entry;

/**
 * Apache Derby (though potentially any SQL99 DB) based advertisement cache.
 * 
 * <p>Implementation notes:</p>
 * <ul>
 * <li>A connection pool is used to deal with concurrent access, rather than attempting
 * to synchronize access to a single connection.</li>
 * <li>We assume JDBC 3.0 prepared statement pooling is available in the database, rather
 * than trying to hold on to PreparedStatement objects for as long as possible (impossible
 * with the connection pool strategy in use).</li>
 * </ul>
 */
public abstract class JdbcAdvertisementCache extends AbstractAdvertisementCache {

	private static final Logger LOG = Logger.getLogger(JdbcAdvertisementCache.class.getName());
	public static final String DATABASE_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	private static final int MAX_CONNECTIONS = 16;
	
	private static final String CREATE_RECORD_TABLE_SQL 
		= "CREATE TABLE Record \n" + 
			"(\n" + 
			"	dn VARCHAR(255) NOT NULL, \n" + 
			"	fn VARCHAR(255) NOT NULL, \n" +
			"   isAdvertisement TINYINT NOT NULL, \n" +
			"	lifetime BIGINT NOT NULL, \n" + 
			"	expiry BIGINT NOT NULL, \n" + 
			"	data BLOB NOT NULL,\n" + 
			"	PRIMARY KEY (dn, fn)\n" + 
			")";
	
	private static final String CREATE_INDEXFIELD_TABLE_SQL
		= "CREATE TABLE IndexField\n" + 
			"(\n" + 
			"	dn VARCHAR(255) NOT NULL,\n" + 
			"	fn VARCHAR(255) NOT NULL,\n" + 
			"	name VARCHAR(255) NOT NULL,\n" + 
			"	value VARCHAR(255) NOT NULL,\n" + 
			"	FOREIGN KEY(dn, fn) REFERENCES Record(dn, fn) ON DELETE CASCADE,\n" +
			"   PRIMARY KEY(dn, fn, name)\n" +
			")";
	
	private static final String CREATE_DELTA_EXPIRY_INDEX_SQL
		= "CREATE INDEX RecordExpiryIndex ON Record ( lifetime )";
	
	private static final String PUT_INDEXFIELD_SQL
		= "INSERT INTO IndexField VALUES (?,?,?,?)";
	
	static {
		if(!loadDbDriver(DATABASE_DRIVER)) {
			throw new RuntimeException("Unable to load Apache Derby DB driver");
		}
	}
	
	private static boolean loadDbDriver(String dbDriver) {
		try {
			Class.forName(dbDriver).newInstance();
			return true;
		} catch(ClassNotFoundException e) {
			if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
				LOG.log(Level.WARNING, "Unable to find JDBC driver [" + dbDriver + "]", e);
			}
			return false;
		} catch(InstantiationException e) {
			if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
				LOG.log(Level.WARNING, "Unable to instantiate JDBC driver [" + dbDriver + "]", e);
			}
			return false;
		} catch(IllegalAccessException e) {
			if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
				LOG.log(Level.WARNING, "Cannot access JDBC driver [" + dbDriver + "]", e);
			}
			return false;
		}
	}
	
	private MiniConnectionPoolManager connPool;
	protected File dbDir;
	private DeltaTracker deltaTracker;
	
	public JdbcAdvertisementCache(URI storeRoot, String areaName) throws IOException {
		this(storeRoot, areaName, 1 * TimeUtils.ANHOUR, false);
	}

	public JdbcAdvertisementCache(URI storeRoot, String areaName, long gcinterval, boolean trackDeltas) throws IOException {
		File dbParentDir = new File(storeRoot);
		dbDir = new File(dbParentDir, areaName);
		ConnectionPoolDataSource dataSource = createDataSource();
		connPool = new MiniConnectionPoolManager(dataSource,MAX_CONNECTIONS);
		deltaTracker = new DeltaTracker();
		deltaTracker.setTrackingDeltas(trackDeltas);
		
		try {
			configureDatabase();
		} catch (SQLException e) {
			try {
				connPool.dispose();
			} catch (SQLException e1) {
				if(Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
					LOG.log(Level.SEVERE, "Failed to dispose database pool when recovering from configuring database", e1);
				}
			}
			IOException wrapper = new IOException("Failed to configure database properly");
			wrapper.initCause(e);
			throw wrapper;
		}
	}

	protected abstract ConnectionPoolDataSource createDataSource();

	private void configureDatabase() throws SQLException {
		Connection conn = null;
		boolean successful = false;
		try {
			conn = getConnection();
			
			if(testDatabaseSetUp(conn)) {
				successful = true;
			}
			
			executeCreate(conn, CREATE_RECORD_TABLE_SQL);
			executeCreate(conn, CREATE_INDEXFIELD_TABLE_SQL);
			executeCreate(conn, CREATE_DELTA_EXPIRY_INDEX_SQL);
			
			conn.commit();
			successful = true;
		} finally {
			closeResources(conn, !successful);
		}
	}
	
	/**
	 * Attempts to close the common resources used in most DB accesses. If any
	 * exceptions occur, they are logged and ignored. 
	 * @param st if true, an attempt will be made to roll back the connection.
	 */
	private void closeResources(Connection conn, boolean rollBack, Statement... statements) {
		if(statements != null) {
			for(Statement st : statements) {
				closeStatement(st);
			}
		}
		
		if(conn != null) {
			if(rollBack) {
				try {
					conn.rollback();
				} catch(SQLException e) {
					if(Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
						LOG.log(Level.SEVERE, "Failed to roll back connection", e);
					}
				}
			}
			
			try {
				conn.close();
			} catch(SQLException e) {
				if(Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
					LOG.log(Level.SEVERE, "Failed to close connection", e);
				}
			}
		}
	}
	
	private void closeStatement(Statement st) {
		if(st == null) {
			return;
		}
		try {
			st.close();
		} catch(SQLException e) {
			if(Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
				LOG.log(Level.SEVERE, "Failed to close statement", e);
			}
		}
	}
	
	private void closeResultSet(ResultSet rs) {
		if(rs == null) {
			return;
		}
		
		try {
			rs.close();
		} catch(SQLException e) {
			if(Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
				LOG.log(Level.SEVERE, "Failed to close result set", e);
			}
		}
	}
	
	private boolean testDatabaseSetUp(Connection conn) throws SQLException {
		// check to see if the Record table exists
		return conn.getMetaData().getTables(null, null, "Record", null).next();
	}
	
	private Connection getConnection() throws SQLException {
		Connection connection = connPool.getConnection();
		connection.setAutoCommit(false);
		
		return connection;
	}
	
	private void executeCreate(Connection conn, String sql) throws SQLException {
		Statement st = null;
		try {
			st = conn.createStatement();
			st.executeUpdate(sql);
		} finally {
			closeStatement(st);
		}
	}
	
	private static final String REMOVE_EXPIRED_RECORDS = "DELETE FROM Record WHERE lifetime < ?";
	public void garbageCollect() throws IOException {
		Connection conn = null;
		PreparedStatement st = null;
		boolean rollback = true;
		try {
			conn = getConnection();
			
			st = conn.prepareStatement(REMOVE_EXPIRED_RECORDS);
			st.setLong(1, TimeUtils.timeNow());
			st.executeUpdate();
			
			conn.commit();
			rollback = false;
		} catch(SQLException e) {
			throw createWrapper("Error occurred while garbage collecting", e);
		} finally {
			closeResources(conn, rollback, st);
		}
	}

	public List<Entry> getDeltas(String dn) {
		return deltaTracker.getDeltas(dn);
	}

	private static final String GET_ENTRIES_SQL 
		= "SELECT IndexField.name, IndexField.value, Record.lifetime " +
		  "FROM Record, IndexFIELD " +
		  "WHERE Record.dn = ?" +
		  "  AND Record.dn = IndexField.dn" +
		  "  AND Record.fn = IndexField.fn";
	public List<Entry> getEntries(String dn, boolean clearDeltas) throws IOException {
		LinkedList<Entry> entries = new LinkedList<Entry>();
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		boolean rollback = true;
		try {
			conn = getConnection();
			st = conn.prepareStatement(GET_ENTRIES_SQL);
			st.setString(1, dn);
			st.execute();
			
			rs = st.getResultSet();
			while(rs.next()) {
				String fieldName = rs.getString(1);
				String fieldValue = rs.getString(2);
				long lifetime = rs.getLong(3);
				
				entries.add(new Entry(fieldName, fieldValue, TimeUtils.toRelativeTimeMillis(lifetime)));
			}
			
			if(clearDeltas) {
				deltaTracker.clearDeltas(dn);
			}
			
			conn.commit();
			rollback = false;
			return entries;
		} catch(SQLException e) {
			throw createWrapper("Unable to get entries for dn=[" + dn + "]", e);
		} finally {
			closeResultSet(rs);
			closeResources(conn, rollback, st);
		}
	}

	private static final String GET_EXPIRATION_SQL = "SELECT lifetime, expiry FROM Record WHERE dn = ? AND fn = ?";
	
	public long getExpirationtime(String dn, String fn) throws IOException {
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		boolean rollback = true;
		try {
			conn = getConnection();
			st = conn.prepareStatement(GET_EXPIRATION_SQL);
			st.setString(1, dn);
			st.setString(2, fn);
			st.execute();
			
			rs = st.getResultSet();
			if(!rs.next()) {
				return -1;
			}
			
			long absoluteLifetime = rs.getLong(1);
			long relativeExpiry = rs.getLong(2);
			
			conn.commit();
			rollback = false;
			return CacheUtils.getRelativeExpiration(absoluteLifetime, relativeExpiry);
		} catch(SQLException e) {
			throw createWrapper("Unable to get expiration for dn=[" + dn + "], fn=[" + fn + "]", e);
		} finally {
			closeResultSet(rs);
			closeResources(conn, rollback, st);
		}
	}

	private static final String GET_INPUT_STREAM_SQL = "SELECT data FROM Record WHERE dn = ? AND fn = ?";
	
	public InputStream getInputStream(String dn, String fn) throws IOException {
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		boolean rollback = true;
		try {
			conn = getConnection();
			st = conn.prepareStatement(GET_INPUT_STREAM_SQL);
			st.setString(1, dn);
			st.setString(2, fn);
			st.execute();
			
			rs = st.getResultSet();
			if(!rs.next()) {
				return null;
			}
			
			ByteArrayInputStream result = new ByteArrayInputStream(rs.getBytes(1)); 
			conn.commit();
			rollback = false;
			return result;
		} catch(SQLException e) {
			throw createWrapper("Unable to get input stream for dn=[" + dn + "], fn=[" + fn + "]", e);
		} finally {
			closeResultSet(rs);
			closeResources(conn, rollback, st);
		}
	}

	private static final String GET_LIFETIME_SQL = "SELECT lifetime FROM Record WHERE dn = ? AND fn = ?";
	
	public long getLifetime(String dn, String fn) throws IOException {
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		boolean rollback = true;
		try {
			conn = getConnection();
			st = conn.prepareStatement(GET_LIFETIME_SQL);
			st.setString(1, dn);
			st.setString(2, fn);
			
			st.execute();
			rs = st.getResultSet();
			if(!rs.next()) {
				return -1;
			}
			
			long result = TimeUtils.toRelativeTimeMillis(rs.getLong(1));
			conn.commit();
			rollback = false;
			return result;
		} catch(SQLException e) {
			throw createWrapper("Unable to get lifetime for dn=[" + dn + "], fn=[" + fn + "]", e);
		} finally {
			closeResultSet(rs);
			closeResources(conn, rollback, st);
		}
	}
	
	private static final String GET_RECORDS_SQL = "SELECT data,lifetime,expiry FROM Record WHERE dn = ? AND lifetime > ?";
	
	public List<InputStream> getRecords(String dn, int threshold, List<Long> expirations, boolean purge) throws IOException {
		LinkedList<InputStream> results = new LinkedList<InputStream>();
		if(dn == null) {
			return results;
		}
		
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		boolean rollback = true;
		try {
			conn = getConnection();
			String queryStr = GET_RECORDS_SQL;
			st = conn.prepareStatement(queryStr);
			st.setString(1, dn);
			st.setLong(2, TimeUtils.timeNow());
			st.setMaxRows(threshold);
			st.execute();
			
			rs = st.getResultSet();
			while(rs.next()) {
				long lifetime = rs.getLong(2);
				long expiry = rs.getLong(3);
				long relativeExpiry = CacheUtils.getRelativeExpiration(lifetime, expiry);
				
				if(relativeExpiry > 0) {
					results.add(new ByteArrayInputStream(rs.getBytes(1)));
					
					if(expirations != null) {
						expirations.add(relativeExpiry);
					}
				} else if(purge) {
					rs.deleteRow();
				}
			}
			
			conn.commit();
			rollback = false;
		} catch(SQLException e) {
			throw createWrapper("Error occurred while fetching records for dn=[" + dn + "]", e);
		} finally {
			closeResultSet(rs);
			closeResources(conn, rollback, st);
		}
		
		return results;
	}

	private static final String GET_DATA_AND_EXPIRY_SQL 
		= "SELECT data, lifetime " +
		  "FROM Record " +
		  "WHERE dn = ? " +
		  "  AND fn = ? " +
		  "  AND isAdvertisement = 1";
	private static final String REMOVE_RECORD_SQL = "DELETE FROM Record WHERE dn = ? AND fn = ?";
	public void remove(String dn, String fn) throws IOException {
		Connection conn = null;
		PreparedStatement fetchSt = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		boolean rollback = true;
		try {
			conn = getConnection();
			
			if(deltaTracker.isTrackingDeltas()) {
				fetchSt = conn.prepareStatement(GET_DATA_AND_EXPIRY_SQL);
				fetchSt.setString(1, dn);
				fetchSt.setString(2, fn);
				fetchSt.execute();
				rs = fetchSt.getResultSet();
				if(rs.next()) {
					byte[] data = rs.getBytes(1);
					long lifetime = rs.getLong(2);
					XMLDocument<?> doc = (XMLDocument<?>) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, new ByteArrayInputStream(data));
	                Advertisement adv = AdvertisementFactory.newAdvertisement(doc);
	                deltaTracker.generateDeltas(dn, adv, doc, lifetime);
				}
			}
			
			st = conn.prepareStatement(REMOVE_RECORD_SQL);
			st.setString(1, dn);
			st.setString(2, fn);
			
			st.executeUpdate();
			
			rollback = (st.getUpdateCount() != 1);
			if(!rollback) {
				conn.commit();
			}
		} catch(SQLException e) {
			throw createWrapper("Unable to remove record for dn=[" + dn + "], fn=[" + fn + "]", e);
		} finally {
			closeResultSet(rs);
			closeResources(conn, rollback, fetchSt, st);
		}
	}

	public void save(String dn, String fn, Advertisement adv, long lifetime, long expiration) throws IOException {
		
		if(lifetime < 0 || expiration < 0) {
			throw new IllegalArgumentException("Bad expiration or lifetime.");
		}
		
		Connection conn = null;
		Statement st = null;
		boolean rollback = true;
		try {
			conn = getConnection();
			boolean wasNew = putRecord(conn, dn, fn, true, getBytesForAdvert(adv), lifetime, expiration);
			
			if(!wasNew) {
				deleteIndexables(conn, dn, fn);
			}
			
			StructuredDocument<?> doc = (StructuredDocument<?>)adv.getDocument(MimeMediaType.XMLUTF8);
			Map<String, String> indexFields = CacheUtils.getIndexfields(adv.getIndexFields(), doc);
			for(String indexField : indexFields.keySet()) {
				if(!putIndexable(conn, dn, fn, indexField, indexFields.get(indexField))) {
					return;
				}
			}
			
			deltaTracker.generateDeltas(dn, adv, null, expiration);
			
			conn.commit();
			rollback = false;
		} catch(SQLException e) {
			throw createWrapper("Failed to write advertisement to cache", e);
		} finally {
			closeResources(conn, rollback, st);
		}
	}
	
	private static final String DELETE_INDEXABLES_SQL = "DELETE FROM IndexField WHERE dn = ? AND fn = ?";
	private void deleteIndexables(Connection conn, String dn, String fn) throws SQLException {
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(DELETE_INDEXABLES_SQL);
			st.setString(1, dn);
			st.setString(2, fn);
			
			st.execute();
		} finally {
			closeStatement(st);
		}
	}

	private static final String PUT_INDEXABLE_SQL = "INSERT INTO IndexField VALUES (?,?,?,?)";
	private boolean putIndexable(Connection conn, String dn, String fn, String field, String value) throws SQLException {
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(PUT_INDEXABLE_SQL);
			st.setString(1, dn);
			st.setString(2, fn);
			st.setString(3, field);
			st.setString(4, value);
			
			st.executeUpdate();
			
			return st.getUpdateCount() == 1;
		} finally {
			closeStatement(st);
		}
	}

	private IOException createWrapper(String message, SQLException e) {
		IOException wrapper = new IOException(message);
		wrapper.initCause(e);
		return wrapper;
	}

	private byte[] getBytesForAdvert(Advertisement adv) throws IOException {
		Document doc = adv.getDocument(MimeMediaType.XMLUTF8);
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream(2048);
		doc.sendToStream(byteStream);
		return byteStream.toByteArray();
	}

	public void save(String dn, String fn, byte[] data, long lifetime,
			long expiration) throws IOException {
		
		if(lifetime < 0 || expiration < 0) {
			throw new IllegalArgumentException("Bad expiration or lifetime.");
		}
		Connection conn = null;
		boolean rollback = true;
		try {
			conn = getConnection();
			putRecord(conn, dn, fn, false, data, lifetime, expiration);
			
			conn.commit();
			rollback = false;
		} catch(SQLException e) {
			IOException wrapper = new IOException("Failed to write advert to database");
			wrapper.initCause(e);
			throw wrapper;
		} finally {
			closeResources(conn, rollback);
		}
	}
	
	/**
	 * @return true if the record was new, false otherwise.
	 * @throws SQLException if an error occurred, or writing the record failed
	 */
	private boolean putRecord(Connection conn, String dn, String fn, boolean isAdvertisement, byte[] data, long lifetime, long expiration) throws SQLException {
		
		long newLifetime = TimeUtils.toAbsoluteTimeMillis(lifetime);
		Long oldLifetime = getOldLifetime(conn, dn, fn);
		if(oldLifetime != null) {
			newLifetime = Math.max(newLifetime, oldLifetime.longValue());
		}
		
		long newLifetimeAsRelative = TimeUtils.toRelativeTimeMillis(newLifetime);
		long boundedExpiration = Math.min(newLifetimeAsRelative, expiration); 
		
		if(oldLifetime != null) {
			// there is an existing record, update rather than insert
			updateRecord(conn, dn, fn, isAdvertisement, data, newLifetime, boundedExpiration);
			return false;
		} else {
			insertRecord(conn, dn, fn, isAdvertisement, data, newLifetime, boundedExpiration);
			return true;
		}
	}

	private static final String GET_OLD_LIFETIME_SQL = "SELECT lifetime FROM Record WHERE dn = ? AND fn = ?";
	private Long getOldLifetime(Connection conn, String dn, String fn) throws SQLException {
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			st = conn.prepareStatement(GET_OLD_LIFETIME_SQL);
			st.setString(1, dn);
			st.setString(2, fn);
			
			st.execute();
			
			rs = st.getResultSet();
			if(rs.next()) {
				return rs.getLong(1);
			} else {
				return null;
			}
		} finally {
			closeResultSet(rs);
			closeStatement(st);
		}
	}
	
	private static final String INSERT_RECORD_SQL = "INSERT INTO Record VALUES (?,?,?,?,?,?)";
	private void insertRecord(Connection conn, String dn, String fn, boolean isAdvertisment, byte[] data, long newLifetime, long boundedExpiration) throws SQLException {
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(INSERT_RECORD_SQL);
			st.setString(1, dn);
			st.setString(2, fn);
			st.setInt(3, isAdvertisment ? 1 : 0);
			st.setLong(4, newLifetime);
			st.setLong(5, boundedExpiration);
			st.setBytes(6, data);
			st.execute();
			
			if(st.getUpdateCount() != 1) {
				throw new SQLException("Incorrect number of rows updated");
			}
		} finally {
			closeStatement(st);
		}
	}

	private static final String UPDATE_RECORD_SQL = "UPDATE Record SET lifetime = ?, expiry = ?, data = ?, isAdvertisement = ? WHERE dn = ? AND fn = ?";
	private void updateRecord(Connection conn, String dn, String fn, boolean isAdvertisement, byte[] data, long newLifetime, long expiration) throws SQLException {
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(UPDATE_RECORD_SQL);
			st.setLong(1, newLifetime);
			st.setLong(2, expiration);
			st.setBytes(3, data);
			st.setInt(4, isAdvertisement ? 1 : 0);
			st.setString(5, dn);
			st.setString(6, fn);
			
			st.execute();
			
			if(st.getUpdateCount() != 1) {
				throw new SQLException("Incorrect number of rows updated");
			}
		} finally {
			closeStatement(st);
		}
	}

	static String SEARCH_RECORDS_SQL(boolean withValueMatch) {
		return "SELECT Record.data, Record.lifetime, Record.expiry\n" +
		  "FROM Record, IndexField\n" +
		  "WHERE Record.isAdvertisement = 1" +
		  "  AND IndexField.name = ?\n" +
		  ((withValueMatch) ? "  AND IndexField.value LIKE ?\n" : "") +
		  "  AND IndexField.dn = ?\n" +
		  "  AND IndexField.dn = Record.dn\n" +
		  "  AND IndexField.fn = Record.fn";
	}

	public List<InputStream> search(String dn, String attribute, String value,
			int threshold, List<Long> expirations) throws IOException {
		
		LinkedList<InputStream> results = new LinkedList<InputStream>();
		
		boolean withValueMatch = !("*".equals(value));
		boolean returnExpiry = (expirations != null);
		
		Connection conn = null;
		PreparedStatement st = null;
		boolean rollback = true;
		try {
			conn = getConnection();
			st = conn.prepareStatement(SEARCH_RECORDS_SQL(withValueMatch));
			
			int attrIndex = 1;
			st.setString(attrIndex++, attribute);
			if(withValueMatch) {
				st.setString(attrIndex++, value.replace('*', '%'));
			}
			
			st.setString(attrIndex++, dn);
			st.setMaxRows(threshold);
			
			st.execute();
			ResultSet resultSet = st.getResultSet();
			
			while(resultSet.next()) {
				byte[] bytes = resultSet.getBytes(1);
				long lifetime = resultSet.getLong(2);
				long expiry = resultSet.getLong(3);
				long relativeExp = CacheUtils.getRelativeExpiration(lifetime, expiry); 
				if(relativeExp > 0) {
					results.add(new ByteArrayInputStream(bytes));
					if(returnExpiry) {
						expirations.add(relativeExp);
					}
				}
			}
			
			conn.commit();
			rollback = false;
			return results;
		} catch(SQLException e) {
			throw createWrapper("SQLException occurred while searching. dn=[" + dn + "], attribute=[" + attribute + "], value=[" + value + "]", e);
		} finally {
			closeResources(conn, rollback, st);
		}
	}

	public void setTrackDeltas(boolean trackDeltas) {
		deltaTracker.setTrackingDeltas(trackDeltas);
	}

	public void stop() throws IOException {
		try {
			connPool.dispose();
			shutdownDb();
		} catch(SQLException e) {
			if(Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
				LOG.log(Level.SEVERE, "Failed to shut down database", e);
			}
			IOException wrapper = new IOException("Failed to shut down database");
			wrapper.initCause(e);
			throw wrapper;
		}
	}

	protected abstract void shutdownDb() throws SQLException;
}
