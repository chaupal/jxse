package net.jxta.test.util;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * Test helper which creates a temporary directory on setUp and recursively deletes it
 * on tearDown.
 */
public abstract class FileSystemTest extends TestCase {

	/**
	 * The test directory that is created by setUp, for use in subclasses.
	 */
    protected File testRootDir;
    
    /**
     * Prefix which is used for the temporary directory creation. Override the value of this
     * if you want to check that your test is correctly cleaning up after itself on a test run. 
     */
    protected String testDirPrefix = "fstest";
    
    @Override
    protected void setUp() throws Exception {
    	super.setUp();
    	
    	testRootDir = File.createTempFile(testDirPrefix, "");
        testRootDir.delete();
        testRootDir.mkdir();
    }
    
    @Override
    protected void tearDown() throws Exception {
    	super.tearDown();
    	
    	deleteDir(testRootDir);
    }
    
    protected void deleteDir(File dir) throws IOException {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                File child = new File(dir, children[i]);
                deleteDir(child);
            }
        }

        if (!dir.delete()) {
            throw new IOException("Unable to delete file " + dir.getAbsolutePath());
        }
    }
}
