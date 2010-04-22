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

import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.TextElement;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.logging.Logging;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;


/**
 * General compatibility utility library for centralizing default
 * compatibility information.  This class should not be used by applications.
 * It is merely a centralized location for the implementation classes to
 * use.  There are other, supported ways of accessing this information for
 * application use.
 */
public final class CompatibilityUtils {
    
    /**
     * Logger.
     */
    private static final Logger LOG =
            Logger.getLogger(CompatibilityUtils.class.getName());

    /**
     * Package URI to use in the default ModuleImplAdvertisement.
     */
    private static final String MODULE_IMPL_STD_URI =
            "http://download.java.net/jxta/jxta-jxse/latest/jnlp/lib/jxta.jar";
    
    /**
     * Default provider name to use in the default ModuleImplAdvertisement.
     */
    private static final String MODULE_IMPL_STD_PROVIDER = "sun.com";
    
    /**
     * The Specification title and Specification version we require.
     */
    private static final String STD_COMPAT_FORMAT = "Efmt";
    private static final String STD_COMPAT_FORMAT_VALUE = "JRE1.5";
    private static final String STD_COMPAT_BINDING = "Bind";
    private static final String STD_COMPAT_BINDING_VALUE = "V2.0 Ref Impl";
    
    /**
     * Prevent construction.
     */
    private CompatibilityUtils() {
        // Empty
    }
    
    /**
     * This method exists only to support the deprecated StdPeerGroup
     * MODULE_IMPL_STD_URI field.  Do not use.
     * 
     * @return default module impl package URI
     * @deprecated will be removed in 2.8
     */
    @Deprecated
    public static String getDefaultPackageURI() {
        return MODULE_IMPL_STD_URI;
    }

    /**
     * This method exists only to support the deprecated StdPeerGroup
     * MODULE_IMPL_STD_PROVIDER field.  Do not use.
     * 
     * @return default module impl provider
     * @deprecated will be removed in 2.8
     */
    @Deprecated
    public static String getDefaultProvider() {
        return MODULE_IMPL_STD_PROVIDER;
    }

    /**
     * Given all the required information, constructs and returns a
     * simple ModuleImplAdvertisement.  This is a bit of an odd duck for
     * being in this library, but it makes sense if you think about it as
     * constructing a default, compatible, implementation advertisement.
     * 
     * @param msid ModuleSpecID of the Module
     * @param className Class name of the Module implementation
     * @param description description of the Module
     * @return ModuleImplAdvertisement
     */
    public static ModuleImplAdvertisement createModuleImplAdvertisement(
            ModuleSpecID msid, String className, String description) {
        ModuleImplAdvertisement implAdv = (ModuleImplAdvertisement)
        AdvertisementFactory.newAdvertisement(
                ModuleImplAdvertisement.getAdvertisementType());
        
        implAdv.setModuleSpecID(msid);
        implAdv.setCompat(createDefaultCompatStatement());
        implAdv.setCode(className);
        implAdv.setUri(MODULE_IMPL_STD_URI);
        implAdv.setProvider(MODULE_IMPL_STD_PROVIDER);
        implAdv.setDescription(description);
        
        return implAdv;
    }
    
    /**
     * Create a default module compatibility statement.  This method should
     * not be used by applications to obtain a compatibility statement.
     * The supported way for an application to create a compatibility statement
     * is to obtain a default PeerGroup implementation advertisement and
     * extract it's compatibility statement.
     * 
     * @return compatibility statement document
     */
    @SuppressWarnings("unchecked")
    public static XMLDocument createDefaultCompatStatement() {
        XMLDocument doc = (XMLDocument)
                StructuredDocumentFactory.newStructuredDocument(
                MimeMediaType.XMLUTF8, "Comp");
        XMLElement e = doc.createElement(STD_COMPAT_FORMAT, STD_COMPAT_FORMAT_VALUE);
        doc.appendChild(e);
        
        e = doc.createElement(STD_COMPAT_BINDING, STD_COMPAT_BINDING_VALUE);
        doc.appendChild(e);
        return doc;
    }
    
    /**
     * Evaluates if the given compatibility statement makes the module that
     * bears it is loadable by this group.
     *
     * @param compat The compatibility statement being tested.
     * @return {@code true} if we are compatible with the provided statement
     *  otherwise {@code false}.
     */
    public static boolean isCompatible(Element compat) {
        boolean formatOk = false;
        boolean bindingOk = false;
        
        if(!(compat instanceof TextElement)) {
            return false;
        }
        
        try {
            Enumeration<TextElement> hisChildren = ((TextElement)compat).getChildren();
            int i = 0;
            while (hisChildren.hasMoreElements()) {
                // Stop after 2 elements; there shall not be more.
                if (++i > 2) {
                    return false;
                }
                
                TextElement e = hisChildren.nextElement();
                String key = e.getKey();
                String val = e.getValue().trim();
                
                if (STD_COMPAT_FORMAT.equals(key)) {
                    Package javaLangPackage = Package.getPackage("java.lang");
                    
                    boolean specMatches;
                    String version;
                    
                    if (val.startsWith("JDK") || val.startsWith("JRE")) {
                        specMatches = true;
                        version = val.substring(3).trim(); // allow for spaces.
                    } else if (val.startsWith(javaLangPackage.getSpecificationTitle())) {
                        specMatches = true;
                        version = val.substring(javaLangPackage.getSpecificationTitle().length()).trim(); // allow for spaces.
                    } else {
                        specMatches = false;
                        version = null;
                    }

                    formatOk = specMatches && javaLangPackage.isCompatibleWith(version);

                } else if (STD_COMPAT_BINDING.equals(key) && STD_COMPAT_BINDING_VALUE.equals(val)) {

                    bindingOk = true;

                } else {

                    Logging.logCheckedWarning(LOG, "Bad element in compatibility statement : ", key);
                    return false; // Might as well stop right now.

                }
            }

        } catch (Exception any) {

            Logging.logCheckedWarning(LOG, "Failure handling compatibility statement\n", any);
            return false;

        }
        
        return formatOk && bindingOk;
    }
    
}
