/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *       Sun Microsystems, Inc. for Project JXTA."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact Project JXTA at http://www.jxta.org.
 *
 * 5. Products derived from this software may not be called "JXTA",
 *    nor may "JXTA" appear in their name, without prior written
 *    permission of Sun.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 */
package net.jxta.impl.content;

import java.util.logging.Logger;
import junit.framework.TestCase;
import net.jxta.document.Advertisement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Module;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the workings of the ModuleLifecycleTracker.
 */
@RunWith(JMock.class)
public class ModuleLifecycleTrackerTest extends TestCase {
    private static final Logger LOG =
            Logger.getLogger(ModuleLifecycleTrackerTest.class.getName());
    private ModuleLifecycleTracker tracker;
    private ModuleLifecycleListener listener;
    private Module module;
    private PeerGroup peerGroup;
    private ID id;
    private Advertisement adv;
    private PeerGroupException pgx = new PeerGroupException(
            "Testing hardcoded exception");
    private Mockery context = new Mockery();

    /**
     * Default constructor.
     */
    public ModuleLifecycleTrackerTest() {
    }

    @Before
    @Override
    public void setUp() {
        LOG.info("===========================================================");
        module = context.mock(Module.class);
        listener = context.mock(ModuleLifecycleListener.class);
        peerGroup = context.mock(PeerGroup.class);

        tracker = new ModuleLifecycleTracker<Module>(
                module, peerGroup, id, null, new String[0]);
        tracker.addModuleLifecycleListener(listener);
    }

    @After
    @Override
    public void tearDown() {
        System.out.flush();
    }

    @Test
    public void testGetModule() throws Exception {
        assertSame(module, tracker.getModule());
    }

    @Test
    public void testListener() throws Exception {
        context.checking(new Expectations() {{
            exactly(2).of(module).init(peerGroup, id, null);
            will(throwException(pgx));

            one(listener).unhandledPeerGroupException(tracker, pgx);
        }});

        tracker.init();
        tracker.removeModuleLifecycleListener(listener);
        tracker.init();

        context.assertIsSatisfied();
    }

    @Test
    public void testInitStartStopStart() throws Exception {
        context.checking(new Expectations() {{
            one(module).init(peerGroup, id, null);
            one(module).stopApp();

            exactly(2).of(module).startApp(with(any(String[].class)));
            will(returnValue(Module.START_OK));

            one(listener).moduleLifecycleStateUpdated(
                    tracker, ModuleLifecycleState.INITIALIZED);
            one(listener).moduleLifecycleStateUpdated(
                    tracker, ModuleLifecycleState.STARTED);
            one(listener).moduleLifecycleStateUpdated(
                    tracker, ModuleLifecycleState.STOPPED);
            one(listener).moduleLifecycleStateUpdated(
                    tracker, ModuleLifecycleState.STARTED);
        }});

        assertEquals(ModuleLifecycleState.UNINITIALIZED, tracker.getState());
        tracker.init();
        assertEquals(ModuleLifecycleState.INITIALIZED, tracker.getState());
        tracker.startApp();
        assertEquals(ModuleLifecycleState.STARTED, tracker.getState());
        tracker.stopApp();
        assertEquals(ModuleLifecycleState.STOPPED, tracker.getState());
        tracker.startApp();
        assertEquals(ModuleLifecycleState.STARTED, tracker.getState());

        context.assertIsSatisfied();
    }

    @Test
    public void testJustStart() throws Exception {
        context.checking(new Expectations() {{
            one(module).init(peerGroup, id, null);

            one(module).startApp(with(any(String[].class)));
            will(returnValue(Module.START_OK));

            one(listener).moduleLifecycleStateUpdated(
                    tracker, ModuleLifecycleState.INITIALIZED);
            one(listener).moduleLifecycleStateUpdated(
                    tracker, ModuleLifecycleState.STARTED);
        }});

        assertEquals(ModuleLifecycleState.UNINITIALIZED, tracker.getState());
        tracker.startApp();
        assertEquals(ModuleLifecycleState.STARTED, tracker.getState());

        context.assertIsSatisfied();
    }

    @Test
    public void testInitException() throws Exception {
        context.checking(new Expectations() {{
            one(module).init(peerGroup, id, null);
            will(throwException(pgx));

            one(listener).unhandledPeerGroupException(tracker, pgx);
        }});

        assertEquals(ModuleLifecycleState.UNINITIALIZED, tracker.getState());
        tracker.init();
        assertEquals(ModuleLifecycleState.UNINITIALIZED, tracker.getState());

        context.assertIsSatisfied();
    }

    @Test
    public void testJustStartException() throws Exception {
        context.checking(new Expectations() {{
            one(module).init(peerGroup, id, null);
            will(throwException(pgx));

            one(listener).unhandledPeerGroupException(tracker, pgx);
        }});

        assertEquals(ModuleLifecycleState.UNINITIALIZED, tracker.getState());
        tracker.startApp();
        assertEquals(ModuleLifecycleState.UNINITIALIZED, tracker.getState());

        context.assertIsSatisfied();
    }

    @Test
    public void testFailedStart() throws Exception {
        context.checking(new Expectations() {{
            one(module).init(peerGroup, id, null);

            one(module).startApp(with(any(String[].class)));
            will(returnValue(Module.START_AGAIN_STALLED));

            one(module).startApp(with(any(String[].class)));
            will(returnValue(Module.START_AGAIN_PROGRESS));

            one(module).startApp(with(any(String[].class)));
            will(returnValue(Module.START_OK));

            one(listener).moduleLifecycleStateUpdated(
                    tracker, ModuleLifecycleState.INITIALIZED);
            one(listener).moduleLifecycleStateUpdated(
                    tracker, ModuleLifecycleState.STARTED);
        }});

        assertEquals(ModuleLifecycleState.UNINITIALIZED, tracker.getState());
        tracker.startApp();
        assertEquals(ModuleLifecycleState.INITIALIZED, tracker.getState());
        tracker.startApp();
        assertEquals(ModuleLifecycleState.INITIALIZED, tracker.getState());
        tracker.startApp();
        assertEquals(ModuleLifecycleState.STARTED, tracker.getState());
        
        context.assertIsSatisfied();
    }
    
    @Test
    public void testDisabledStart() throws Exception {
        context.checking(new Expectations() {{
            one(module).init(peerGroup, id, null);

            one(module).startApp(with(any(String[].class)));
            will(returnValue(Module.START_DISABLED));

            one(listener).moduleLifecycleStateUpdated(
                    tracker, ModuleLifecycleState.INITIALIZED);
            one(listener).moduleLifecycleStateUpdated(
                    tracker, ModuleLifecycleState.DISABLED);
        }});

        assertEquals(ModuleLifecycleState.UNINITIALIZED, tracker.getState());
        tracker.startApp();
        assertEquals(ModuleLifecycleState.DISABLED, tracker.getState());
        
        // For good measure, make sure it stays DISABLED on stop attempt
        tracker.stopApp();
        assertEquals(ModuleLifecycleState.DISABLED, tracker.getState());

        context.assertIsSatisfied();
    }
}
