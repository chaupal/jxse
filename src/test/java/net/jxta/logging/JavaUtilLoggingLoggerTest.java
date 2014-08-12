package net.jxta.logging;

import static org.junit.Assert.*;

import org.junit.Test;

public class JavaUtilLoggingLoggerTest {

	@Test
	public void slf4jFormatConversion() {
		assertNull(JavaUtilLoggingLogger.slf4jFormatToJULFormat(null));
		assertEquals("", JavaUtilLoggingLogger.slf4jFormatToJULFormat(""));
		assertEquals("xx {0} yy {1} zz", JavaUtilLoggingLogger.slf4jFormatToJULFormat("xx {} yy {} zz"));
		assertEquals("{0}", JavaUtilLoggingLogger.slf4jFormatToJULFormat("{}"));
		assertEquals("bar{0}", JavaUtilLoggingLogger.slf4jFormatToJULFormat("bar{}"));
		assertEquals("{0}bar", JavaUtilLoggingLogger.slf4jFormatToJULFormat("{}bar"));
		assertEquals("foo", JavaUtilLoggingLogger.slf4jFormatToJULFormat("foo"));
		assertEquals("{nondigit", JavaUtilLoggingLogger.slf4jFormatToJULFormat("{nondigit")); // bizzare, but cope with it
		assertEquals("{nondigit}", JavaUtilLoggingLogger.slf4jFormatToJULFormat("{nondigit}")); // bizzare, but cope with it
		assertEquals("{", JavaUtilLoggingLogger.slf4jFormatToJULFormat("{")); // bizzare, but cope with it
	}
}
