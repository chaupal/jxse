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

package net.jxta.impl.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import net.jxta.document.Document;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;


/**
 * This class presents a Document interface for document content at the end
 * of a URI.
 */
public class URIDocument implements Document {
    
    /**
     * Buffer size to use during data sink operation.
     */
    private final static int BUFFER_SIZE = 4096;
    
    /**
     * URI which backs this document.
     */
    private final URI uri;
    
    /**
     *  MIME media type of this document.
     */
    private final MimeMediaType type;
    
    /**
     * Create a new File Document.
     * 
     * @param docURI URI of the resource to construct a Document around
     */
    public URIDocument(URI docURI) {
        this(docURI, StructuredDocumentFactory.getMimeTypeForFileExtension(
                getFileExtension(docURI)));
    }
    
    /**
     * Create a new File Document.
     * 
     * @param docURI URI of the resource to construct a Document around
     * @param docType MIME media type of the document
     */
    public URIDocument(URI docURI, MimeMediaType docType) {
        uri = docURI;
        if (docType == null) {
            type = MimeMediaType.AOS;
        } else {
            type = docType.intern();
        }
    }
    
    /**
     *  {@inheritDoc}
     *
     * @return file everything after the last '.' in the filename or the
     *  empty string if the file name does not contain a '.'
     */
    public String getFileExtension() {
        return getFileExtension(uri);
    }
    
    /**
     *  {@inheritDoc}
     */
    public MimeMediaType getMimeType() {
        return type;
    }
    
    /**
     *  {@inheritDoc}
     * 
     * @throws IOException on failure to open a stream to the URI provided.
     *  This may occur if the URI does not have an applicable URL registration.
     */
    public InputStream getStream() throws IOException {
        URL url = uri.toURL();
        return url.openStream();
    }
    
    /**
     *  {@inheritDoc}
     * 
     * @param sink stream to send the document data to
     * @throws IOException on IO error during data sink to the destination
     */
    public void sendToStream(OutputStream sink) throws IOException {
        InputStream source = getStream();
        int c;
        byte[] buf = new byte[BUFFER_SIZE];
        
        do {
            c = source.read(buf);
            
            if (-1 == c) {
                break;
            }
            
            sink.write(buf, 0, c);
        } while (true);
    }
    
    /**
     * Private version of {@code getFileExtension} which allows the URI tttttto
     * passed in.
     * 
     * @param docURI URI to obtain extension for
     * @return everything after the last '.' in the filename, or the
     *  empty string if the file name does not contain a '.'.
     */
    private static String getFileExtension(URI docURI) {
        String fileName = docURI.getPath();
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length()) {
            return "";
        } else {
            return fileName.substring(idx + 1);
        }
    }
    
}
