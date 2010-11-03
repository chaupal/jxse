package net.jxta.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import net.jxta.test.util.JUnitRuleMockery;

import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class QueuingServerPipeAcceptorTest {

    @Rule
    public JUnitRuleMockery mockContext = new JUnitRuleMockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};

    private static final int BACKLOG = 10;
    QueuingServerPipeAcceptor acceptor;

    @Before
    public void setUp() throws Exception {
        acceptor = new QueuingServerPipeAcceptor(BACKLOG, 100);
    }

    @Test
    public void testDiscardsConnectionsBeyondBacklog() throws Exception {
        for(int i=0; i < BACKLOG; i++) {
            acceptor.pipeAccepted(createPipe("pipe" + i));
        }

        // add one last one, which should be ignored
        acceptor.pipeAccepted(createPipe("pipeToIgnore"));

        for(int i=0; i < BACKLOG; i++) {
            assertNotNull(acceptor.accept(0L, TimeUnit.MILLISECONDS));
        }

        assertNull(acceptor.accept(0L, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSetTimeoutBackwardsCompatible() {
    	acceptor.setTimeoutBackwardsCompatible(5000);
    	assertEquals(5000, acceptor.getTimeout());
    }

    @Test
    public void testSetTimeoutBackwardsCompatible_toZero() {
    	// zero is equivalent to "no timeout"
    	acceptor.setTimeoutBackwardsCompatible(0);
    	assertEquals(Long.MAX_VALUE, acceptor.getTimeout());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetTimeoutBackwardsCompatible_toNegative() {
    	acceptor.setTimeoutBackwardsCompatible(-1);
    }

    private JxtaBiDiPipe createPipe(String name) throws IOException {
        final JxtaBiDiPipe mockedPipe = mockContext.mock(JxtaBiDiPipe.class, name);
        mockContext.checking(new Expectations() {{
            ignoring(mockedPipe);
        }});

        return mockedPipe;
    }

}
