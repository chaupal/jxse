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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import junit.framework.TestCase;
import net.jxta.content.ContentID;
import net.jxta.content.ContentProviderEvent;
import net.jxta.content.ContentProviderListener;
import net.jxta.content.ContentProviderSPI;
import net.jxta.content.ContentShare;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the workings of the EventAggregator class.
 */
@RunWith(JMock.class)
public class EventAggregatorTest extends TestCase {
    private static Logger LOG =
            Logger.getLogger(EventAggregatorTest.class.getName());
    private EventAggregator aggregator;
    private ContentProviderSPI provider;
    private ContentProviderListener listener;
    private ContentShare share;
    private List<ContentShare> shares =
            new CopyOnWriteArrayList<ContentShare>();
    private List<ContentProviderSPI> providers =
            new CopyOnWriteArrayList<ContentProviderSPI>();
    private List<ContentProviderListener> listeners =
            new CopyOnWriteArrayList<ContentProviderListener>();
    private ContentProviderEvent cpEventShared;
    private ContentProviderEvent cpEventUnshared;
    private ContentProviderEvent cpEventFound;
    private ContentID contentID;

    private Mockery context = new Mockery();

    /**
     * Default constructor.
     */
    public EventAggregatorTest() {
    }

    @Before
    @Override
    public void setUp() {
        LOG.info("===========================================================");
        provider = context.mock(ContentProviderSPI.class);
        providers.clear();
        providers.add(provider);

        listener = context.mock(ContentProviderListener.class);
        listeners.clear();
        listeners.add(listener);

        share = context.mock(ContentShare.class);
        shares.clear();
        shares.add(share);

        PeerGroupID peerGroupID = IDFactory.newPeerGroupID();
        contentID = IDFactory.newContentID(peerGroupID, true);

        cpEventShared =
                new ContentProviderEvent(provider, contentID);
        cpEventUnshared =
                new ContentProviderEvent(provider, shares);
        cpEventFound =
                new ContentProviderEvent(provider, contentID, shares, false);
    }

    @After
    @Override
    public void tearDown() {
        System.out.flush();
    }

    @Test
    public void testContentShared() throws Exception {
        context.checking(new Expectations() {{
            one(provider).addContentProviderListener(
                    with(any(EventAggregator.class)));
            one(listener).contentShared(cpEventShared);
        }});

        aggregator = new EventAggregator(listeners, providers);
        aggregator.contentShared(cpEventShared);

        context.assertIsSatisfied();
    }

    @Test
    public void testContentUnshared() throws Exception {
        context.checking(new Expectations() {{
            one(provider).addContentProviderListener(
                    with(any(EventAggregator.class)));
            one(listener).contentUnshared(cpEventUnshared);
        }});

        aggregator = new EventAggregator(listeners, providers);
        aggregator.contentUnshared(cpEventUnshared);

        context.assertIsSatisfied();
    }

    @Test
    public void testDispatchFindRequest() throws Exception {
        context.checking(new Expectations() {{
            one(provider).addContentProviderListener(
                    with(any(EventAggregator.class)));
            one(provider).findContentShares(
                    with(any(Integer.class)), with(any(EventAggregator.class)));

            exactly(3).of(listener).contentSharesFound(
                    with(any(ContentProviderEvent.class)));
            will(returnValue(true));
        }});

        aggregator = new EventAggregator(listeners, providers);
        aggregator.dispatchFindRequest(3);
        assertTrue(aggregator.contentSharesFound(cpEventFound));
        assertTrue(aggregator.contentSharesFound(cpEventFound));
        assertFalse(aggregator.contentSharesFound(cpEventFound));
        assertFalse(aggregator.contentSharesFound(cpEventFound));

        context.assertIsSatisfied();
    }

}
