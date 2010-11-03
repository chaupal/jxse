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
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Module;
import net.jxta.test.util.JUnitRuleMockery;

import org.jmock.Expectations;
import org.jmock.Sequence;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test the workings of the ModuleLifecycleManager.
 */
public class ModuleLifecycleManagerTest {
    private static final Logger LOG =
            Logger.getLogger(ModuleLifecycleManagerTest.class.getName());
    private ModuleLifecycleManager<Module> manager;
    private ModuleLifecycleManagerListener managerListener;
    private ModuleLifecycleListener listener;
    private Module module1;
    private Module module2;
    private PeerGroup peerGroup;
    private ID id;
    private PeerGroupException pgx;

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    /**
     * Default constructor.
     */
    public ModuleLifecycleManagerTest() {
    }

    /**
     * Set up the test for execution.
     */
    @Before
    public void setUp() {
        LOG.info("===========================================================");
        module1 = context.mock(Module.class, "module1");
        module2 = context.mock(Module.class, "module2");
        managerListener = context.mock(ModuleLifecycleManagerListener.class);
        listener = context.mock(ModuleLifecycleListener.class);
        peerGroup = context.mock(PeerGroup.class);
        id = IDFactory.newModuleClassID();
        pgx = new PeerGroupException("Hardcoded test exception");

        manager = new ModuleLifecycleManager<Module>();
        manager.addModuleLifecycleManagerListener(managerListener);
        manager.addModuleLifecycleListener(listener);
    }

    /**
     * Tear down the test after execution.
     */
    @After
    public void tearDown() {
        System.out.flush();
    }

    @Test
    public void testModuleCounts() throws Exception {
        context.checking(new Expectations() {{
            one(module1).init(peerGroup, id, null);
            one(listener).moduleLifecycleStateUpdated(
                    with(any(ModuleLifecycleTracker.class)),
                    with(equal(ModuleLifecycleState.INITIALIZED)));
        }});

        assertEquals(0, manager.getModuleCount());
        assertEquals(0, manager.getModuleCountInGoalState());
        manager.addModule(module1, peerGroup, id, null, new String[0]);
        assertEquals(1, manager.getModuleCount());
        assertEquals(1, manager.getModuleCountInGoalState());

        manager.init();
        assertEquals(1, manager.getModuleCountInGoalState());

        context.assertIsSatisfied();
    }

    @Test
    public void testInitStartStopStart() throws Exception {
        final Sequence seq = context.sequence("event");

        context.checking(new Expectations() {{
            one(module1).init(peerGroup, id, null);
            one(module2).init(peerGroup, id, null);

            exactly(2).of(listener).moduleLifecycleStateUpdated(
                    with(any(ModuleLifecycleTracker.class)),
                    with(equal(ModuleLifecycleState.INITIALIZED)));
            inSequence(seq);

            one(module1).startApp(with(any(String[].class)));
            will(returnValue(Module.START_OK));

            one(module2).startApp(with(any(String[].class)));
            will(returnValue(Module.START_OK));

            exactly(2).of(listener).moduleLifecycleStateUpdated(
                    with(any(ModuleLifecycleTracker.class)),
                    with(equal(ModuleLifecycleState.STARTED)));
            inSequence(seq);

            one(module1).stopApp();
            one(module2).stopApp();

            exactly(2).of(listener).moduleLifecycleStateUpdated(
                    with(any(ModuleLifecycleTracker.class)),
                    with(equal(ModuleLifecycleState.STOPPED)));
            inSequence(seq);

            one(module1).startApp(with(any(String[].class)));
            will(returnValue(Module.START_OK));

            one(module2).startApp(with(any(String[].class)));
            will(returnValue(Module.START_OK));

            exactly(2).of(listener).moduleLifecycleStateUpdated(
                    with(any(ModuleLifecycleTracker.class)),
                    with(equal(ModuleLifecycleState.STARTED)));
            inSequence(seq);
        }});

        manager.addModule(module1, peerGroup, id, null, new String[0]);
        manager.addModule(module2, peerGroup, id, null, new String[0]);
        manager.init();

        assertEquals("Not all modules in init goal state",
                2, manager.getModuleCountInGoalState());

        manager.start();
        assertEquals("Not all modules in start goal state",
                2, manager.getModuleCountInGoalState());

        manager.stop();
        assertEquals("Not all modules in stop goal state",
                2, manager.getModuleCountInGoalState());

        manager.start();
        assertEquals("Not all modules in start 2 goal state",
                2, manager.getModuleCountInGoalState());

        context.assertIsSatisfied();
    }

    @Test
    public void testInitFailure() throws Exception {
        context.checking(new Expectations() {{
            one(module1).init(peerGroup, id, null);
            will(throwException(pgx));

            one(listener).unhandledPeerGroupException(
                    with(any(ModuleLifecycleTracker.class)),
                    with(any(PeerGroupException.class)));
        }});

        manager.addModule(module1, peerGroup, id, null, new String[0]);
        manager.init();

        context.assertIsSatisfied();
    }

    @Test
    public void testMaxIterations() throws Exception {
        context.checking(new Expectations() {{
            one(module1).init(peerGroup, id, null);

            one(module1).startApp(with(any(String[].class)));
            will(returnValue(Module.START_AGAIN_STALLED));

            one(managerListener).moduleStalled(
                    with(any(ModuleLifecycleManager.class)),
                    with(any(ModuleLifecycleTracker.class)));

            allowing(listener).moduleLifecycleStateUpdated(
                    with(any(ModuleLifecycleTracker.class)),
                    with(any(ModuleLifecycleState.class)));
        }});

        manager.addModule(module1, peerGroup, id, null, new String[0]);
        manager.start();

        context.assertIsSatisfied();
    }

    @Test
    public void testMaxStall() throws Exception {
        final Module[] bulkModules = new Module[9];

        // Give us a larger number of modules that simply work.
        for (int i=0; i<bulkModules.length; i++) {
            bulkModules[i] =
                    context.mock(Module.class, "bulkModule[" + i + "]");
        }
        context.checking(new Expectations() {{
            for (Module module : bulkModules) {
                one(module).init(peerGroup, id, null);
                allowing(module).startApp(with(any(String[].class)));
                will(returnValue(Module.START_OK));
            }
            one(module1).init(peerGroup, id, null);
            exactly(11).of(module1).startApp(with(any(String[].class)));
            will(returnValue(Module.START_AGAIN_STALLED));

            allowing(managerListener).moduleStalled(
                    with(any(ModuleLifecycleManager.class)),
                    with(any(ModuleLifecycleTracker.class)));

            allowing(listener).moduleLifecycleStateUpdated(
                    with(any(ModuleLifecycleTracker.class)),
                    with(any(ModuleLifecycleState.class)));
        }});

        for (Module module : bulkModules) {
            manager.addModule(module, peerGroup, id, null, new String[0]);
        }
        manager.addModule(module1, peerGroup, id, null, new String[0]);
        manager.start();

        context.assertIsSatisfied();
    }

    @Test
    public void testMaxStallWithProgressReset() throws Exception {
        final Module[] bulkModules = new Module[9];

        // Give us a larger number of modules that simply work.
        for (int i=0; i<bulkModules.length; i++) {
            bulkModules[i] =
                    context.mock(Module.class, "bulkModule[" + i + "]");
        }
        context.checking(new Expectations() {{
            for (Module module : bulkModules) {
                one(module).init(peerGroup, id, null);
                allowing(module).startApp(with(any(String[].class)));
                will(returnValue(Module.START_OK));
            }

            one(module1).init(peerGroup, id, null);
            for (int i=0; i<15; i++) {
                one(module1).startApp(with(any(String[].class)));
                will(returnValue(Module.START_AGAIN_STALLED));

                one(module1).startApp(with(any(String[].class)));
                will(returnValue(Module.START_AGAIN_PROGRESS));
            }
            one(module1).startApp(with(any(String[].class)));
                will(returnValue(Module.START_OK));

            allowing(listener).moduleLifecycleStateUpdated(
                    with(any(ModuleLifecycleTracker.class)),
                    with(any(ModuleLifecycleState.class)));
        }});

        for (Module module : bulkModules) {
            manager.addModule(module, peerGroup, id, null, new String[0]);
        }
        manager.addModule(module1, peerGroup, id, null, new String[0]);
        manager.start();

        context.assertIsSatisfied();
    }

    @Test
    public void testDisabledModule() throws Exception {
        context.checking(new Expectations() {{
            one(module1).init(peerGroup, id, null);
            one(listener).moduleLifecycleStateUpdated(
                    with(any(ModuleLifecycleTracker.class)),
                    with(equal(ModuleLifecycleState.INITIALIZED)));

            one(module1).startApp(with(any(String[].class)));
                will(returnValue(Module.START_DISABLED));

            one(listener).moduleLifecycleStateUpdated(
                    with(any(ModuleLifecycleTracker.class)),
                    with(equal(ModuleLifecycleState.DISABLED)));
            one(managerListener).moduleDisabled(
                    with(any(ModuleLifecycleManager.class)),
                    with(any(ModuleLifecycleTracker.class)));
        }});

        manager.addModule(module1, peerGroup, id, null, new String[0]);
        assertEquals(1, manager.getModuleCount());

        manager.init();
        assertEquals(1, manager.getModuleCount());

        manager.start();
        assertEquals(0, manager.getModuleCount());

        context.assertIsSatisfied();
    }

    @Test
    public void testRemoveModule() throws Exception {
        context.checking(new Expectations() {{
            one(module1).init(peerGroup, id, null);
            one(listener).moduleLifecycleStateUpdated(
                    with(any(ModuleLifecycleTracker.class)),
                    with(equal(ModuleLifecycleState.INITIALIZED)));

            one(module1).startApp(with(any(String[].class)));
                will(returnValue(Module.START_OK));
            one(listener).moduleLifecycleStateUpdated(
                    with(any(ModuleLifecycleTracker.class)),
                    with(equal(ModuleLifecycleState.STARTED)));

            one(module1).stopApp();
            one(listener).moduleLifecycleStateUpdated(
                    with(any(ModuleLifecycleTracker.class)),
                    with(equal(ModuleLifecycleState.STOPPED)));
        }});

        manager.addModule(module1, peerGroup, id, null, new String[0]);
        assertEquals(1, manager.getModuleCount());

        manager.init();
        assertEquals(1, manager.getModuleCount());

        manager.start();
        assertEquals(1, manager.getModuleCount());

        manager.removeModule(module1, true);
        assertEquals(0, manager.getModuleCount());

        context.assertIsSatisfied();
    }

}
