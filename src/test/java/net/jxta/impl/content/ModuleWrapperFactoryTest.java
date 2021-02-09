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
import net.jxta.content.Content;
import net.jxta.content.ContentService;
import net.jxta.platform.Module;
import net.jxta.service.Service;
import net.jxta.test.util.JUnitRuleMockery;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test the workings of the ModuleWrapperFactory class.
 */
public class ModuleWrapperFactoryTest {
    private static final Logger LOG =
            Logger.getLogger(ModuleWrapperFactoryTest.class.getName());
    private Module module;
    private Service service;
    private ContentService contentService;

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    /**
     * Default constructor.
     */
    public ModuleWrapperFactoryTest() {
    }

    @Before
    public void setUp() {
        LOG.info("===========================================================");
        module = context.mock(Module.class);
        service = context.mock(Service.class);
        contentService = context.mock(ContentService.class);
    }

    @After
    public void tearDown() {
        System.out.flush();
    }

    @Test
    public void testNewModule() throws Exception {
        context.checking(new Expectations() {{
            // Expect nothing
        }});

        Module proxy = ModuleWrapperFactory.newWrapper(module);
        proxy.init(null, null, null);
        int result = proxy.startApp(null);
        assertEquals("startApp return value", Module.START_OK, result);
        proxy.stopApp();

        context.assertIsSatisfied();
    }

    @Test
    public void testNewService() throws Exception {
        context.checking(new Expectations() {{
            oneOf(service).getImplAdvertisement();
//            one(service).getInterface();
        }});

        Service proxy = ModuleWrapperFactory.newWrapper(service);
        proxy.init(null, null, null);
        int result = proxy.startApp(null);
        assertEquals("startApp return value", Module.START_OK, result);
        proxy.stopApp();

        proxy.getImplAdvertisement();
//        proxy.getInterface();

        context.assertIsSatisfied();
    }

    @Test
    public void testNewWrapper() throws Exception {
        context.checking(new Expectations() {{
            oneOf(contentService).getImplAdvertisement();
//            one(contentService).getInterface();
            oneOf(contentService).shareContent(with(aNull(Content.class)));
        }});

        ContentService proxy = 
                (ContentService) ModuleWrapperFactory.newWrapper(
                new Class[] { ContentService.class },
                contentService);
        proxy.init(null, null, null);
        int result = proxy.startApp(null);
        assertEquals("startApp return value", Module.START_OK, result);
        proxy.stopApp();

        proxy.getImplAdvertisement();
//        proxy.getInterface();
        proxy.shareContent(null);

        context.assertIsSatisfied();
    }

}
