package net.jxta.impl.cm;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import net.jxta.document.Element;
import net.jxta.document.StructuredDocument;
import net.jxta.impl.util.TimeUtils;
import net.jxta.logging.Logging;

/**
 * Collection of utility methods useful to {@link AdvertisementCache} implementations,
 * extracted from the original {@link Cm} implementation.
 */
public class CacheUtils {

	/**
	 * Extracts the values for all indexable fields in an advertisement.
	 * @param fields the names of the indexable fields, typically provided by {@link net.jxta.document.Advertisement#getIndexFields()}.
	 * @param doc the document of the advertisement.
	 * @return a map of all indexable fields to their values in the provided advertisement document.
	 */
	public static Map<String, String> getIndexfields(String[] fields, StructuredDocument<?> doc) {

            Map<String, String> map = new HashMap<String, String>();
	
	    if (doc == null) {

	        if (Logging.SHOW_WARNING && XIndiceAdvertisementCache.LOG.isLoggable(Level.WARNING)) {
	            XIndiceAdvertisementCache.LOG.warning("Null document");
	        }

	        return map;
                
	    }

	    if (fields == null) return map;
	    
	    for (String field : fields) {

	        Enumeration<?> en = doc.getChildren(field);

	        while (en.hasMoreElements()) {

	            String val = (String) ((Element<?>) en.nextElement()).getValue();

                    if (val != null) map.put(field, val);
	            
	        }
	    }

	    return map;
	}
	
	public static long getRelativeExpiration(long absoluteLifetime, long relativeExpiry) {

	    return Math.min(Math.max(-1, TimeUtils.toRelativeTimeMillis(absoluteLifetime)), relativeExpiry);

	}
	
	public static String convertValueQueryToRegex(String value) {

	    return value.replaceAll("\\*", ".*?");
            
	}
}
