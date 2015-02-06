package net.jxta.impl.loader;

import java.util.logging.Logger;

import junit.framework.JUnit4TestAdapter;
import net.jxta.peergroup.core.Module;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JxtaLoaderModulemanagerTest {

    static final Logger LOG =
            Logger.getLogger(RefJxtaLoaderTest.class.getName());

    public static junit.framework.Test suite() { 
        return new JUnit4TestAdapter(JxtaLoaderModulemanagerTest.class); 
    }

    private JxtaLoaderModuleManager<Module> manager;
    
    @BeforeClass
    public static void setupClass() throws Exception {
        LOG.info("============ Begin setupClass");
    }

    @AfterClass
    public static void tearDownClass() throws InterruptedException {
        LOG.info("============ Begin tearDownClass");
    }
    
    @Before
    public void setup() throws Exception {
        LOG.info("============ Begin setup");
        manager = JxtaLoaderModuleManager.getRoot( this.getClass(), true );
    }
    
    @After
    public void tearDown() throws Exception {
        LOG.info("============ Begin tearDown");
        manager = null;
        Thread.sleep(300);
        LOG.info("============ End tearDown");
    }
    
    @Test
    public void testBuild(){
    	
    }
}
