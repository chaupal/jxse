package net.jxta.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class PropertiesUtilTest {

	private Properties testProps;
	
	@Before
	public void setUp() throws Exception {
		testProps = new Properties();
		testProps.setProperty("a", "b");
		testProps.setProperty("c", "d");
		testProps.setProperty("e", "f");
		testProps.setProperty("g", "h");
	}

	@Test
	public void testStringPropertyNames() {
		Set<String> props = PropertiesUtil.stringPropertyNames(testProps);
		assertEquals(4, props.size());
		assertTrue(props.contains("a"));
		assertTrue(props.contains("c"));
		assertTrue(props.contains("e"));
		assertTrue(props.contains("g"));
	}

}
