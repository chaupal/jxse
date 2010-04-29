package net.jxta.configuration;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class PropertiesUtil {

	/**
	 * Replacement for the stringPropertyNames method that is provided on
	 * Properties in Java 6, but not in Java 5. Once we move to Java 6 officially,
	 * this method can be replaced with the commented out version below.
	 */
	public static Set<String> stringPropertyNames(Properties p) {
		final Set<String> propertyNames = new TreeSet<String>();
		final Enumeration<?> propNameEnum = p.propertyNames();
		while(propNameEnum.hasMoreElements()) {
			propertyNames.add((String)propNameEnum.nextElement());
		}
		
		return propertyNames;
	}

	/* JAVA 6 VERSION
	public static Set<String> stringPropertyNaes(Properties p) {
		return p.stringPropertyNames();
	}
	*/
}
