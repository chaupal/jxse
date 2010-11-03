/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
 *
 *  The Sun Project JXTA(TM) Software License
 *
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice, 
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *
 *  3. The end-user documentation included with the redistribution, if any, must 
 *     include the following acknowledgment: "This product includes software 
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must 
 *     not be used to endorse or promote products derived from this software 
 *     without prior written permission. For written permission, please contact 
 *     Project JXTA at http://www.jxta.org.
 *
 *  5. Products derived from this software may not be called "JXTA", nor may 
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN 
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United 
 *  States and other countries.
 *
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of 
 *  the license in source files.
 *
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */

package net.jxta.impl.endpoint;

import junit.framework.*;

import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.document.*;
import net.jxta.peergroup.WorldPeerGroupFactory;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.TransportAdvertisement;

import net.jxta.impl.protocol.HTTPAdv;
import net.jxta.impl.protocol.PlatformConfig;
import net.jxta.impl.protocol.TCPAdv;
import org.junit.Ignore;

import java.util.Enumeration;
import java.util.Vector;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;

@Ignore("JXTA Configurator required")
public class XportConfTest extends TestCase {

    static PeerGroup pg;
    static int count;

    public XportConfTest(java.lang.String testName) throws net.jxta.exception.PeerGroupException {

        super(testName);
    }

    /**
     * Loads the Platform Config from the named file.
     *
     * @param  file the file containing the Platform Config to be loaded.
     **/
    private PlatformConfig loadConfig(File file) throws Exception {

        PlatformConfig advertisement = null;
        FileInputStream advStream = null;
        XMLDocument advDocument = null;

        try {

            advStream = new FileInputStream(file);
            advDocument = (XMLDocument) StructuredDocumentFactory.newStructuredDocument( MimeMediaType.XMLUTF8, advStream );

            advertisement = (PlatformConfig)
                    AdvertisementFactory.newAdvertisement(advDocument);

        } finally {
            try {
                if (advStream != null) {
                    advStream.close();
                }
                advStream = null;
            } catch (Exception ignored) {
                ;
            }
        }

        return advertisement;
    }

    private void saveConfig(ConfigParams advertisement, File file) throws Exception {

        FileOutputStream out = null;

        try {
            out = new FileOutputStream(file);
            Document aDoc = advertisement.getDocument(MimeMediaType.XMLUTF8);

            aDoc.sendToStream(out);
        } finally {
            if (null != out) {
                out.close();
            }
            out = null;
        }
    }

    private void fixTcp(TCPAdv xpAdv) throws Exception {
        xpAdv.setPublicAddressOnly(true);
        xpAdv.setServer("1.1.1.1:1");
        xpAdv.setServerEnabled(true);
    }

    private void fixHttp(HTTPAdv xpAdv) throws Exception {
        xpAdv.setPublicAddressOnly(true);
        xpAdv.setServer("1.1.1.1:1");
        xpAdv.setServerEnabled(true);
    }

    private void removeRelay(ConfigParams config) throws Exception {
        StructuredTextDocument param = (StructuredTextDocument)
                config.getServiceParam(PeerGroup.relayProtoClassID);

        param.appendChild(param.createElement("isOff"));

        config.putServiceParam(PeerGroup.relayProtoClassID, param);
    }

    private TCPAdv extractTcp(ConfigParams config) throws Exception {

        Element param = config.getServiceParam(PeerGroup.tcpProtoClassID);

        Enumeration tcpChilds = param.getChildren(TransportAdvertisement.getAdvertisementType());

        // get the TransportAdv
        if (tcpChilds.hasMoreElements()) {
            param = (Element) tcpChilds.nextElement();
            Attribute typeAttr = ((Attributable) param).getAttribute("type");

            if (!TCPAdv.getAdvertisementType().equals(typeAttr.getValue())) {
                throw new IllegalArgumentException("transport adv is not a " + TCPAdv.getAdvertisementType());
            }

            if (tcpChilds.hasMoreElements()) {
                throw new IllegalArgumentException("Multiple transport advs detected for tcp");
            }
        } else {
            throw new IllegalArgumentException(TransportAdvertisement.getAdvertisementType() + " could not be located");
        }

        Advertisement paramsAdv = AdvertisementFactory.newAdvertisement((XMLElement) param);

        if (!(paramsAdv instanceof TCPAdv)) {
            throw new IllegalArgumentException("Provided Advertisement was not a " + TCPAdv.getAdvertisementType());
        }

        return (TCPAdv) paramsAdv;
    }

    private HTTPAdv extractHttp(ConfigParams config) throws Exception {

        Element param = config.getServiceParam(PeerGroup.httpProtoClassID);

        Enumeration httpChilds = param.getChildren(TransportAdvertisement.getAdvertisementType());

        // get the TransportAdv
        if (httpChilds.hasMoreElements()) {
            param = (Element) httpChilds.nextElement();
            Attribute typeAttr = ((Attributable) param).getAttribute("type");

            if (!HTTPAdv.getAdvertisementType().equals(typeAttr.getValue())) {
                throw new IllegalArgumentException(
                        "transport adv is not a " + HTTPAdv.getAdvertisementType() + "(= " + typeAttr.getValue());
            }

            if (httpChilds.hasMoreElements()) {
                throw new IllegalArgumentException("Multiple transport advs detected for http");
            }

        } else {
            throw new IllegalArgumentException("configuration did not contain http advertisement");
        }

        Advertisement paramsAdv = AdvertisementFactory.newAdvertisement((XMLElement) param);

        if (!(paramsAdv instanceof HTTPAdv)) {
            throw new IllegalArgumentException("Provided Advertisement was not a " + HTTPAdv.getAdvertisementType());
        }

        return (HTTPAdv) paramsAdv;
    }

    private void insertTcp(TCPAdv tcpAdv, ConfigParams config) throws Exception {

        StructuredDocument parm = StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");

        StructuredDocumentUtils.copyElements(parm, parm, (StructuredDocument) tcpAdv.getDocument(MimeMediaType.XMLUTF8));
        config.putServiceParam(PeerGroup.tcpProtoClassID, parm);
    }

    private void insertHttp(HTTPAdv httpAdv, ConfigParams config) throws Exception {

        StructuredDocument parm = StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");

        StructuredDocumentUtils.copyElements(parm, parm, (StructuredDocument) httpAdv.getDocument(MimeMediaType.XMLUTF8));
        config.putServiceParam(PeerGroup.httpProtoClassID, parm);
    }

    private void fixConfig() throws Exception {

        File jxtaHomeDir = new File(pg.getStoreHome());

        File configFile = new File(jxtaHomeDir, "PlatformConfig");

        ConfigParams config = loadConfig(configFile);

        removeRelay(config);

        TCPAdv tcpAdv = extractTcp(config);
        HTTPAdv httpAdv = extractHttp(config);

        fixTcp(tcpAdv);
        fixHttp(httpAdv);

        insertTcp(tcpAdv, config);
        insertHttp(httpAdv, config);

        saveConfig(config, configFile);
    }

    private void restoreConfig() throws Exception {

        File jxtaHomeDir = new File(pg.getStoreHome());

        File configFileSaved = new File(jxtaHomeDir, "PlatformConfig.saved");
        File configFile = new File(jxtaHomeDir, "PlatformConfig");

        ConfigParams config = loadConfig(configFileSaved);

        saveConfig(config, configFile);
    }

    private void backupConfig() throws Exception {

        File jxtaHomeDir = new File(pg.getStoreHome());

        File configFileSaved = new File(jxtaHomeDir, "PlatformConfig.saved");
        File configFile = new File(jxtaHomeDir, "PlatformConfig");

        ConfigParams config;

        try {
            config = loadConfig(configFileSaved);
            return; // already good. Do not risk replacing the backup.
        } catch (Exception e) {
            config = loadConfig(configFile);
        }
        saveConfig(config, configFileSaved);
    }

    @Override
    public void setUp() throws Exception {

        synchronized (XportConfTest.class) {
            try {
                if (count++ > 0) {
                    return;
                }
                final PeerGroup wpg = new WorldPeerGroupFactory().getInterface();
                // Create one for nothing. Just to make sure the config
                // is created.
                System.setProperty("net.jxta.tls.password", "password");
                System.setProperty("net.jxta.tls.principal", "password");
                pg = PeerGroupFactory.newNetPeerGroup(wpg);

                // Throw that one away.
//                pg.unref();

                // Fix the config and start for good.
                backupConfig();
                fixConfig();

                System.setProperty("net.jxta.tls.password", "password");
                System.setProperty("net.jxta.tls.principal", "password");
                pg = PeerGroupFactory.newNetPeerGroup(wpg);
            } catch (Exception e) {
                if (pg != null) {
//                    pg.unref();
                }
                restoreConfig();
                throw e;
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        synchronized (XportConfTest.class) {
            if (--count > 0) {
                return;
            }
//            pg.unref();
            pg = null;
            restoreConfig();
            System.out.flush();
            System.err.flush();
        }
    }

    private Enumeration getEndpointAddresses(PeerAdvertisement peerAdv) {
	
        // Get its EndpointService advertisement
        TextElement endpParam = (TextElement)
                peerAdv.getServiceParam(PeerGroup.endpointClassID);
	
        if (endpParam == null) {
            return null;
        }

        RouteAdvertisement route = null;

        try {
            Enumeration paramChilds = endpParam.getChildren(RouteAdvertisement.getAdvertisementType());
            Element param = null;

            if (paramChilds.hasMoreElements()) {
                param = (Element) paramChilds.nextElement();
            }
            route = (RouteAdvertisement) 
                    AdvertisementFactory.newAdvertisement((XMLElement) param);
        } catch (Exception ex) {
            return null;
        }

        if (route == null) {
            return null;
        }

        Vector addrs = new Vector();

        try {
            for (Enumeration e = route.getDest().getEndpointAddresses(); e.hasMoreElements();) {
                addrs.add(new EndpointAddress((String) e.nextElement()));
            }
        } catch (Exception e) {
            return null;
        }

        if (addrs.size() == 0) {
            return null;
        }

        return Collections.enumeration(addrs);
    }

    public void testPubAddressOnly() throws Exception {
        PeerAdvertisement newPadv = pg.getPeerAdvertisement();

        Enumeration endps = getEndpointAddresses(newPadv);

        assertFalse("There should be exactly 4 endpoint addresses : " + newPadv, endps == null);

        assertTrue("There should be exactly 4 addresses : " + newPadv, endps.hasMoreElements());

        Object oneEndp = endps.nextElement();

        assertTrue("There should be exactly 4 addresses : " + newPadv, endps.hasMoreElements());

        oneEndp = endps.nextElement();

        assertTrue("There should be exactly 4 addresses : " + newPadv, endps.hasMoreElements());

        oneEndp = endps.nextElement();

        assertTrue("There should be exactly 4 addresses : " + newPadv, endps.hasMoreElements());

        oneEndp = endps.nextElement();

        assertFalse("There should be exactly 4 address : " + newPadv, endps.hasMoreElements());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(XportConfTest.class);

        return suite;
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
