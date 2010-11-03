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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import net.jxta.content.Content;
import net.jxta.content.ContentID;
import net.jxta.content.ContentProviderSPI;
import net.jxta.content.ContentSourceLocationState;
import net.jxta.content.ContentTransfer;
import net.jxta.content.ContentTransferAggregatorEvent;
import net.jxta.content.ContentTransferAggregatorListener;
import net.jxta.content.ContentTransferEvent;
import net.jxta.content.ContentTransferListener;
import net.jxta.content.ContentTransferState;
import net.jxta.content.TransferException;
import net.jxta.document.Document;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.protocol.ContentShareAdvertisement;
import net.jxta.test.util.JUnitRuleMockery;
import net.jxta.test.util.TempDir;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test the workings of the TransferAggregator class.
 */
public class TransferAggregatorTest {
    private static Logger LOG =
            Logger.getLogger(TransferAggregatorTest.class.getName());
    private static final TempDir TEMP_DIR;

    private TransferAggregator aggregator;
    private ContentTransferAggregatorListener aggListener;
    private ContentTransferListener listener;
    private ContentProviderSPI provider1;
    private ContentProviderSPI provider2;
    private ContentProviderSPI provider3;
    private ContentProviderSPI provider4;
    private List<ContentProviderSPI> providers =
            new CopyOnWriteArrayList<ContentProviderSPI>();
    private ContentTransfer transfer1;
    private ContentTransfer transfer2;
    private ContentTransfer transfer3;
    private ContentTransfer transfer4;
    private List<ContentTransfer> transfers =
            new CopyOnWriteArrayList<ContentTransfer>();

    private ContentTransfer selected;
    private ContentTransfer standby;

    private Content content;

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    /**
     * Default constructor.
     */
    public TransferAggregatorTest() {
    }

    static {
        try {
            TEMP_DIR = new TempDir();
        } catch (IOException iox) {
            throw(new IllegalStateException(
                    "Could not intiialize temp dir", iox));
        }
    }

    @Before
    public void setUp() throws Exception {
        LOG.info("===========================================================");
        TEMP_DIR.clear();
        listener = context.mock(ContentTransferListener.class);
        aggListener = context.mock(ContentTransferAggregatorListener.class);
        transfer1 = context.mock(ContentTransfer.class, "transfer1");
        transfer2 = context.mock(ContentTransfer.class, "transfer2");
        transfer3 = context.mock(ContentTransfer.class, "transfer3");
        transfer4 = context.mock(ContentTransfer.class, "transfer4");
        transfers.add(transfer1);
        transfers.add(transfer2);
        transfers.add(transfer3);
        transfers.add(transfer4);
        provider1 = context.mock(ContentProviderSPI.class, "provider1");
        provider2 = context.mock(ContentProviderSPI.class, "provider2");
        provider3 = context.mock(ContentProviderSPI.class, "provider3");
        provider4 = context.mock(ContentProviderSPI.class, "provider4");
        providers.add(provider1);
        providers.add(provider2);
        providers.add(provider3);
        providers.add(provider4);

        PeerGroupID peerGroupID = IDFactory.newPeerGroupID();
        ContentID contentID = IDFactory.newContentID(peerGroupID, true);
        Document document = StructuredDocumentFactory.newStructuredDocument(
                MimeMediaType.TEXTUTF8, "foo", "bar");
        content = new  Content(contentID, null, document);
    }

    @After
    public void tearDown() {
        Thread.yield();
        System.out.flush();
        System.err.flush();
    }

    @Test
    public void testConstructionWithNoProviders() throws TransferException {
        context.checking(new Expectations() {{
            one(provider1).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(null));

            one(provider2).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(null));

            one(provider3).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(null));

            one(provider4).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(null));
        }});

        try {
            aggregator = new TransferAggregator(null,
                providers, (ContentShareAdvertisement) null);
            fail("TransferException was not thrown");
        } catch (TransferException transx) {
            /*
             * For some reason @Test(expected=TransferException.class)
             * is not working...
             */
        }
    }

    @Test
    public void testConstruction() throws Exception {
        context.checking(new Expectations() {{
            one(provider1).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(transfer1));

            one(provider2).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(transfer2));

            one(provider3).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(transfer3));

            one(provider4).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(transfer4));

            one(transfer1).addContentTransferListener(
                    with(any(TransferAggregator.class)));
            one(transfer2).addContentTransferListener(
                    with(any(TransferAggregator.class)));
            one(transfer3).addContentTransferListener(
                    with(any(TransferAggregator.class)));
            one(transfer4).addContentTransferListener(
                    with(any(TransferAggregator.class)));
        }});

        aggregator = new TransferAggregator(null,
                providers, (ContentShareAdvertisement) null);
        aggregator.addContentTransferAggregatorListener(aggListener);
        aggregator.addContentTransferAggregatorListener(
                new ContentTransferAggregatorListener() {
            public void selectedContentTransfer(
                    ContentTransferAggregatorEvent ctaEvent) {
                selected = ctaEvent.getDelegateContentTransfer();
            }

            public void updatedContentTransferList(
                    ContentTransferAggregatorEvent ctaEvent) {
                // Ignore
            }

        });
        aggregator.addContentTransferListener(listener);

        context.assertIsSatisfied();
    }

    @Test
    public void testRandomization() throws Exception {
        int last = -1;
        int same = 0;
        int total = 0;

        for (int i=0; i<10; i++) {
            testConstruction();
            List<ContentTransfer> list = new ArrayList<ContentTransfer>(
                    aggregator.getContentTransferList());

            assertTrue(list.contains(transfer1));
            assertTrue(list.contains(transfer2));
            assertTrue(list.contains(transfer3));
            assertTrue(list.contains(transfer4));
            assertEquals(4, list.size());

            int value = 0;
            for (ContentTransfer transfer : list) {
                value *= 10;
                if (transfer == transfer1) {
                    value += 1;
                } else if (transfer == transfer2) {
                    value += 2;
                } else if (transfer == transfer3) {
                    value += 3;
                } else {
                    value += 4;
                }
            }
            LOG.info("Last : " + last);
            LOG.info("Value: " + value);

            if (last > 0) {
                total++;
                if (last == value) {
                    same++;
                }
            }
            last = value;
        }

        assertTrue("Element ordering was not sufficiently random (same=" +
                same + ", total=" + total + ")", ((same / total) < 0.5F));

        context.assertIsSatisfied();
    }

    @Test
    public void testStartSourceLocation() throws Exception {
        testConstruction();
        List<ContentTransfer> xfers = new ArrayList<ContentTransfer>(
                aggregator.getContentTransferList());
        final ContentTransfer first = xfers.remove(0);
        final ContentTransfer second = xfers.remove(0);
        final ContentTransfer third = xfers.remove(0);
        final Sequence firstSeq = context.sequence("selected transfer");
        final Sequence secondSeq = context.sequence("standby1 transfer");
        final Sequence thirdSeq = context.sequence("standby2 transfer");
        standby = second;

        context.checking(new Expectations() {{
            one(first).getTransferState();
            will(returnValue(ContentTransferState.PENDING));
            inSequence(firstSeq);

            one(aggListener).selectedContentTransfer(
                    with(any(ContentTransferAggregatorEvent.class)));
            inSequence(firstSeq);

            one(first).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.NOT_LOCATING));
            inSequence(firstSeq);

            one(first).startSourceLocation();
            inSequence(firstSeq);

            one(second).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.NOT_LOCATING));
            inSequence(secondSeq);

            one(second).startSourceLocation();
            inSequence(secondSeq);

            one(third).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.NOT_LOCATING));
            inSequence(thirdSeq);

            one(third).startSourceLocation();
            inSequence(thirdSeq);

        }});

        aggregator.startSourceLocation();

        LOG.info("selected = " + selected);
        LOG.info("standby  = " + standby);

        assertSame("selected",
                first, selected);
        assertSame("getCurrentContentTransfer",
                first, aggregator.getCurrentContentTransfer());

        context.assertIsSatisfied();
    }

    @Test
    public void testSelectedLocationStateHasEnough() throws Exception {
        testStartSourceLocation();
        final ContentTransferEvent ctEvent =
                new ContentTransferEvent.Builder(selected)
                .locationCount(100)
                .locationState(ContentSourceLocationState.LOCATING_HAS_ENOUGH)
                .transferState(ContentTransferState.PENDING)
                .build();

        context.checking(new Expectations() {{
            one(listener).contentLocationStateUpdated(
                    with(any(ContentTransferEvent.class)));
            one(selected).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.LOCATING_HAS_ENOUGH));
            // Location keeps going...
        }});

        aggregator.contentLocationStateUpdated(ctEvent);

        context.assertIsSatisfied();
    }

    @Test
    public void testSelectedLocationStateHasMany() throws Exception {
        testStartSourceLocation();
        final ContentTransferEvent ctEvent =
                new ContentTransferEvent.Builder(selected)
                .locationCount(100)
                .locationState(ContentSourceLocationState.LOCATING_HAS_MANY)
                .transferState(ContentTransferState.PENDING)
                .build();

        context.checking(new Expectations() {{
            one(listener).contentLocationStateUpdated(
                    with(any(ContentTransferEvent.class)));
            one(selected).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.LOCATING_HAS_MANY));
            one(selected).stopSourceLocation();
        }});

        aggregator.contentLocationStateUpdated(ctEvent);

        context.assertIsSatisfied();
    }

    @Test
    public void testStandbyLocationStateHasEnough() throws Exception {
        testStartSourceLocation();
        final ContentTransferEvent ctEvent =
                new ContentTransferEvent.Builder(standby)
                .locationCount(100)
                .locationState(ContentSourceLocationState.LOCATING_HAS_ENOUGH)
                .transferState(ContentTransferState.PENDING)
                .build();

        context.checking(new Expectations() {{
            one(standby).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.LOCATING_HAS_ENOUGH));
            one(standby).stopSourceLocation();
        }});

        aggregator.contentLocationStateUpdated(ctEvent);

        context.assertIsSatisfied();
    }

    @Test
    public void testStandbyLocationStateHasMany() throws Exception {
        testStartSourceLocation();
        final ContentTransferEvent ctEvent =
                new ContentTransferEvent.Builder(standby)
                .locationCount(100)
                .locationState(ContentSourceLocationState.LOCATING_HAS_MANY)
                .transferState(ContentTransferState.PENDING)
                .build();

        context.checking(new Expectations() {{
            one(standby).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.LOCATING_HAS_MANY));
            one(standby).stopSourceLocation();
        }});

        aggregator.contentLocationStateUpdated(ctEvent);

        context.assertIsSatisfied();
    }

    @Test
    public void testStandbyTransferCompletion() throws Exception {
        testStartSourceLocation();
        final ContentTransferEvent ctEvent =
                new ContentTransferEvent.Builder(standby)
                .locationCount(100)
                .locationState(ContentSourceLocationState.LOCATING_HAS_MANY)
                .transferState(ContentTransferState.COMPLETED)
                .build();

        context.checking(new Expectations() {{
            // Ignore everything except selected and standby
            for (ContentTransfer transfer : transfers) {
                if (transfer != selected && transfer != standby) {
                    ignoring(transfer);
                }
            }

            // Ignore basic transfer events
            ignoring(listener);

            // contentTransferStateUpdated handling on out-of-band success
            one(standby).getContent();
            will(returnValue(content));
            one(aggListener).selectedContentTransfer(
                    with(any(ContentTransferAggregatorEvent.class)));

            // all transfers should be cancelled for cleanup purposes
            one(standby).removeContentTransferListener(aggregator);
            one(standby).cancel();
            one(selected).removeContentTransferListener(aggregator);
            one(selected).cancel();
        }});

        aggregator.contentTransferStateUpdated(ctEvent);

        context.assertIsSatisfied();
    }

    @Test
    public void testTransferCancelled() throws Exception {
        testStartSourceLocation();

        context.checking(new Expectations() {{
            one(selected).startTransfer(with(any(File.class)));
            one(transfer1).cancel();
            one(transfer2).cancel();
            one(transfer3).cancel();
            one(transfer4).cancel();
        }});

        File dest = new File(TEMP_DIR, "content");
        aggregator.startTransfer(dest);
        aggregator.cancel();

        context.assertIsSatisfied();
    }

    @Test
    public void testDontReturnToPending() throws Exception {
        testStartSourceLocation();

        final ContentTransferEvent failedEvent =
                new ContentTransferEvent.Builder(selected)
                .locationCount(100)
                .locationState(ContentSourceLocationState.LOCATING)
                .transferState(ContentTransferState.FAILED)
                .build();

        final ContentTransferEvent stalledEvent =
                new ContentTransferEvent.Builder(selected)
                .locationCount(100)
                .locationState(ContentSourceLocationState.LOCATING)
                .transferState(ContentTransferState.STALLED)
                .build();

        context.checking(new Expectations() {{
            // Ignore everything except selected and standby
            for (ContentTransfer transfer : transfers) {
                if (transfer != selected && transfer != standby) {
                    allowing(transfer).getSourceLocationState();
                    will(returnValue(ContentSourceLocationState.NOT_LOCATING_HAS_MANY));
                }
            }

            ignoring(aggListener);

            one(selected).startTransfer(with(any(File.class)));
            one(listener).contentTransferStateUpdated(
                    with(any(ContentTransferEvent.class)));

            // On failure, the getContent is called to extract the exception
            one(selected).getContent();
            will(throwException(new TransferException("Ignored")));
            one(selected).cancel();

            // Next batter up...
            one(standby).getTransferState();
            will(returnValue(ContentTransferState.PENDING));
            one(standby).startTransfer(with(any(File.class)));

            allowing(standby).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.LOCATING_HAS_ENOUGH));

        }});

        File dest = new File(TEMP_DIR, "content");
        aggregator.startTransfer(dest);
        assertEquals(ContentTransferState.PENDING,
                aggregator.getTransferState());

        aggregator.contentTransferStateUpdated(stalledEvent);
        assertEquals(ContentTransferState.STALLED,
                aggregator.getTransferState());

        // The FAILED event should have been absorbed
        aggregator.contentTransferStateUpdated(failedEvent);
        assertEquals(ContentTransferState.STALLED,
                aggregator.getTransferState());

        context.assertIsSatisfied();
    }

    @Test
    public void testAllTransfersFail() throws Exception {
        testStartSourceLocation();

        context.checking(new Expectations() {{
            // Ignore everything except selected and standby
            for (ContentTransfer transfer : transfers) {
                // Each transfer is started once
                one(transfer).startTransfer(with(any(File.class)));

                // Each transfer fails once
                one(transfer).getContent();
                will(throwException(new TransferException("Ignored")));
                one(transfer).cancel();

                if (transfer != selected) {
                    one(transfer).getTransferState();
                    will(returnValue(ContentTransferState.PENDING));
                }

                allowing(transfer).getSourceLocationState();
                will(returnValue(ContentSourceLocationState.LOCATING_HAS_ENOUGH));

                allowing(transfer).stopSourceLocation();
            }

            ignoring(aggListener);

            one(listener).contentTransferStateUpdated(
                    with(any(ContentTransferEvent.class)));
        }});

        File dest = new File(TEMP_DIR, "content");
        aggregator.startTransfer(dest);
        assertEquals(ContentTransferState.PENDING,
                aggregator.getTransferState());

        // The FAILED event should have been absorbed
        aggregator.contentTransferStateUpdated(
            new ContentTransferEvent.Builder(
                    aggregator.getCurrentContentTransfer())
                .locationCount(100)
                .locationState(ContentSourceLocationState.LOCATING)
                .transferState(ContentTransferState.FAILED)
                .build());
        assertEquals(ContentTransferState.PENDING,
                aggregator.getTransferState());

        // The FAILED event should have been absorbed
        aggregator.contentTransferStateUpdated(
            new ContentTransferEvent.Builder(
                    aggregator.getCurrentContentTransfer())
                .locationCount(100)
                .locationState(ContentSourceLocationState.LOCATING)
                .transferState(ContentTransferState.FAILED)
                .build());
        assertEquals(ContentTransferState.PENDING,
                aggregator.getTransferState());

        // The FAILED event should have been absorbed
        aggregator.contentTransferStateUpdated(
            new ContentTransferEvent.Builder(
                    aggregator.getCurrentContentTransfer())
                .locationCount(100)
                .locationState(ContentSourceLocationState.LOCATING)
                .transferState(ContentTransferState.FAILED)
                .build());
        assertEquals(ContentTransferState.PENDING,
                aggregator.getTransferState());

        // This is the last transfer instance, so the failure is exposed
        aggregator.contentTransferStateUpdated(
            new ContentTransferEvent.Builder(
                    aggregator.getCurrentContentTransfer())
                .locationCount(100)
                .locationState(ContentSourceLocationState.LOCATING)
                .transferState(ContentTransferState.FAILED)
                .build());
        assertEquals(ContentTransferState.FAILED,
                aggregator.getTransferState());

        context.assertIsSatisfied();
    }

}
