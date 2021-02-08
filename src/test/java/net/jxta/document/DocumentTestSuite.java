/**
 * This class can be used to combine various tests
 */
package net.jxta.document;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
	AdvertisementTest.class,
	DocumentTest.class,
	MimeMediaTypeTest.class

})
public class DocumentTestSuite {}