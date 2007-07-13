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

package net.jxta.impl.peergroup;


import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.XMLElement;
import net.jxta.id.IDFactory;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;

import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Not actually an advertisement, but often acts as part of one.
 *
 * @deprecated This internal class will eventually be removed. It has
 *             several problems which make it difficult to support. (The most obvious that
 *             it provides poor abstraction and provides references to its' own internal
 *             data structures). This class is expected to be replaced by a public API class
 *             performing a similar function though such an alternative is not yet
 *             available. You are encouraged to copy this code into your own application
 *             or service if you depend upon it.
 */
@Deprecated
public class StdPeerGroupParamAdv {

    /**
     *  Logger
     */
    private static final Logger LOG = Logger.getLogger(StdPeerGroupParamAdv.class.getName());

    private static final String paramTag = "Parm";
    private static final String protoTag = "Proto";
    private static final String appTag = "App";
    private static final String svcTag = "Svc";
    private static final String mcidTag = "MCID";
    private static final String msidTag = "MSID";

    private static final String miaTag = ModuleImplAdvertisement.getAdvertisementType();

    // In the future we should be able to manipulate all modules regardless
    // of their kind, but right now it helps to keep them categorized
    // as follows.
    private final Map<ModuleClassID, Object> servicesTable = new HashMap<ModuleClassID, Object>();
    private final Map<ModuleClassID, Object> protosTable = new HashMap<ModuleClassID, Object>();
    private final Map<ModuleClassID, Object> appsTable = new HashMap<ModuleClassID, Object>();

    public StdPeerGroupParamAdv() {}

    public StdPeerGroupParamAdv(Element root) {
        if (!XMLElement.class.isInstance(root)) {
            throw new IllegalArgumentException(getClass().getName() + " only supports XMLElement");
        }

        initialize((XMLElement) root);
    }

    /**
     * Return the services entries described in this Advertisement.
     * <p/>
     * The result (very unwisely) is the internal hashmap of this
     * Advertisement. Modifying it results in changes to this Advertisement.
     * For safety the Map should be copied before being modified.
     * @return the services entries described in this Advertisement.
     */
    public Map<ModuleClassID, Object> getServices() {
        return servicesTable;
    }

    /**
     * Return the protocols (message transports) entries described in this Advertisement.
     * <p/>
     * The result (very unwisely) is the internal hashmap of this
     * Advertisement. Modifying it results in changes to this Advertisement.
     * For safety the Map should be copied before being modified.
     * @return  the protocols (message transports) entries described in this Advertisement.
     */
    public Map<ModuleClassID, Object> getProtos() {
        return protosTable;
    }

    /**
     * Return the application entries described in this Advertisement.
     * <p/>
     * The result (very unwisely) is the internal hashmap of this
     * Advertisement. Modifying it results in changes to this Advertisement.
     * For safety the Map should be copied before being modified.
     * @return the application entries described in this Advertisement.
     */
    public Map<ModuleClassID, Object> getApps() {
        return appsTable;
    }

    /**
     * Replaces the table of services described by this Advertisement. All
     * existing entries are lost.
     *
     * @param servicesTable the services table
     */
    public void setServices(Map<ModuleClassID, Object> servicesTable) {
        if (servicesTable == this.servicesTable) {
            return;
        }

        this.servicesTable.clear();

        if (null != servicesTable) {
            this.servicesTable.putAll(servicesTable);
        }
    }

    /**
     * Replaces the table of protocols described by this Advertisement. All
     * existing entries are lost.
     *
     * @param protosTable the protocol table
     */
    public void setProtos(Map<ModuleClassID, Object> protosTable) {
        if (protosTable == this.protosTable) {
            return;
        }

        this.protosTable.clear();

        if (null != protosTable) {
            this.protosTable.putAll(protosTable);
        }
    }

    /**
     * Replaces the table of applications described by this Advertisement. All
     * existing entries are lost.
     *
     * @param appsTable the application table
     */
    public void setApps(Map<ModuleClassID, Object> appsTable) {
        if (appsTable == this.appsTable) {
            return;
        }

        this.appsTable.clear();

        if (null != appsTable) {
            this.appsTable.putAll(appsTable);
        }
    }

    private void initialize(XMLElement doc) {

        if (!doc.getName().equals(paramTag)) {
            throw new IllegalArgumentException("Can not construct " + getClass().getName() + "from doc containing a " + doc.getName());
        }

        // set defaults
        int appCount = 0;
        Enumeration modules = doc.getChildren();

        while (modules.hasMoreElements()) {
            Map<ModuleClassID, Object> theTable;

            XMLElement module = (XMLElement) modules.nextElement();
            String tagName = module.getName();

            if (tagName.equals(svcTag)) {
                theTable = servicesTable;
            } else if (tagName.equals(appTag)) {
                theTable = appsTable;
            } else if (tagName.equals(protoTag)) {
                theTable = protosTable;
            } else {
                continue;
            }

            ModuleSpecID specID = null;
            ModuleClassID classID = null;
            ModuleImplAdvertisement inLineAdv = null;

            try {
                if (module.getTextValue() != null) {
                    specID = (ModuleSpecID)
                            IDFactory.fromURI(new URI(module.getTextValue()));
                }

                // Check for children anyway.
                Enumeration fields = module.getChildren();

                while (fields.hasMoreElements()) {
                    XMLElement field = (XMLElement) fields.nextElement();

                    if (field.getName().equals(mcidTag)) {
                        classID = (ModuleClassID) IDFactory.fromURI(new URI(field.getTextValue()));
                        continue;
                    }
                    if (field.getName().equals(msidTag)) {
                        specID = (ModuleSpecID) IDFactory.fromURI(new URI(field.getTextValue()));
                        continue;
                    }
                    if (field.getName().equals(miaTag)) {
                        inLineAdv = (ModuleImplAdvertisement) AdvertisementFactory.newAdvertisement(field);
                    }
                }
            } catch (Exception any) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, "Broken entry; skipping", any);
                }
                continue;
            }

            if (inLineAdv == null && specID == null) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.warning("Insufficent entry; skipping");
                }
                continue;
            }

            Object theValue;

            if (inLineAdv == null) {
                theValue = specID;
            } else {
                specID = inLineAdv.getModuleSpecID();
                theValue = inLineAdv;
            }
            if (classID == null) {
                classID = specID.getBaseClass();
            }

            // For applications, the role does not matter. We just create
            // a unique role ID on the fly.
            // When outputing the add we get rid of it to save space.

            if (theTable == appsTable) {
                // Only the first (or only) one may use the base class.
                if (classID == PeerGroup.applicationClassID) {
                    if (appCount++ != 0) {
                        classID = IDFactory.newModuleClassID(classID);
                    }
                }
            }
            theTable.put(classID, theValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Document getDocument(MimeMediaType encodeAs) {
        StructuredDocument doc = StructuredDocumentFactory.newStructuredDocument(encodeAs, paramTag);

        outputModules(doc, servicesTable, svcTag);
        outputModules(doc, protosTable, protoTag);
        outputModules(doc, appsTable, appTag);

        return doc;
    }

    private void outputModules(StructuredDocument doc, Map<ModuleClassID, Object> modulesTable, String mainTag) {

        for (Map.Entry<ModuleClassID, Object> entry : modulesTable.entrySet()) {
            ModuleClassID mcid = entry.getKey();
            Object val = entry.getValue();
            Element m;

            // For applications, we ignore the role ID. It is not meaningfull,
            // and a new one is assigned on the fly when loading this adv.

            if (val instanceof Advertisement) {
                m = doc.createElement(mainTag);
                doc.appendChild(m);

                if (modulesTable != appsTable && !mcid.equals(mcid.getBaseClass())) {
                    // It is not an app and there is a role ID. Output it.

                    Element i = doc.createElement(mcidTag, mcid.toString());

                    m.appendChild(i);
                }

                StructuredDocument advdoc = (StructuredDocument)
                        ((Advertisement) val).getDocument(doc.getMimeType());

                StructuredDocumentUtils.copyElements(doc, m, advdoc);
            } else if (val instanceof ModuleSpecID) {
                if (modulesTable == appsTable || mcid.equals(mcid.getBaseClass())) {
                    // Either it is an app or there is no role ID.
                    // So the specId is good enough.
                    m = doc.createElement(mainTag, val.toString());
                    doc.appendChild(m);
                } else {
                    // The role ID matters, so the classId must be separate.
                    m = doc.createElement(mainTag);
                    doc.appendChild(m);

                    Element i;

                    i = doc.createElement(mcidTag, mcid.toString());
                    m.appendChild(i);

                    i = doc.createElement(msidTag, val.toString());
                    m.appendChild(i);
                }
            } else {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.warning("unsupported class in modules table");
                }
            }
        }
    }
}
