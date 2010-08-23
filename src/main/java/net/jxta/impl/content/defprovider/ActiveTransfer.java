/*
 *  The Sun Project JXTA(TM) Software License
 *  
 *  Copyright (c) 2001-2007 Sun Microsystems, Inc. All rights reserved.
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

 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *  
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */

package net.jxta.impl.content.defprovider;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import net.jxta.content.Content;
import net.jxta.document.Document;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;

/**
 * A node being tracked by the ActiveTransferTracker class.  This class acts
 * as a session object of sorts, maintaining client/request specific
 * information for use in the near future.
 */
public class ActiveTransfer {
    /**
     * Amount of time which must elapse before a client will be considered
     * inactive - forfeiting it's slot - in seconds.
     */
    private static final long CLIENT_TIMEOUT =
            Long.getLong(ActiveTransfer.class.getName()
            + ".clientTimeout", 45).intValue() * 1000;

    /**
     * Timeout used for output pipe resolution, in seconds.
     */
    private static final int PIPE_TIMEOUT =
            Integer.getInteger(ActiveTransfer.class.getName()
            + ".pipeTimeout", 10).intValue() * 1000;

    /**
     * OutputPipe to send response data to.
     */
    private final OutputPipe destPipe;

    /**
     * Recovery window used to maintain temporary references to recent data
     * in the event the client needs to retry.
     */
    private final RecoveryWindow window;
    
    /**
     * The share which we are serving.
     */
    private final DefaultContentShare share;

    /**
     * The last time data was requested from this transfer client.
     */
    private long lastAccess = System.currentTimeMillis();

    /**
     * Constructs a new transfer client node.
     */
    public ActiveTransfer(
            PeerGroup peerGroup,
            DefaultContentShare toShare,
            PipeAdvertisement destination) throws IOException {

        // Setup a pipe to the source
        PipeService pipeService = peerGroup.getPipeService();
        destPipe = pipeService.createOutputPipe(destination, PIPE_TIMEOUT);

        share = toShare;
        Content content = toShare.getContent();
        Document doc = content.getDocument();
        BufferedInputStream in =  new BufferedInputStream(doc.getStream());
        window = new RecoveryWindow(in);
    }

    /**
     * Attempt to get the data specified for the destination given.
     *
     * @param offset position in the file of the beginning of the data
     * @param length number of bytes desired
     * @param out stream to write the data to
     * @return negative X when X bytes have been copied and EOF has been
     *      reached, positive X when X bytes have been copied and EOF was
     *      not reached, or 0 if no bytes could be copied.
     * @throws IOException when a problem arises working with IO
     */
    public synchronized int getData(
            long offset, int length, OutputStream out)
            throws IOException {
        int result;

        result = window.getData(offset, length, out);
        lastAccess = System.currentTimeMillis();

        return result;
    }

    /**
     * Determines whether or not this session has been idle for too long.
     *
     * @return true if the session is idle, false if it has been reasonably
     *  active
     */
    public synchronized boolean isIdle() {
        return (System.currentTimeMillis() - lastAccess) > CLIENT_TIMEOUT;
    }

    /**
     * Close out this session.
     *
     * @throws IOException when IO problem arises
     */
    public synchronized void close() throws IOException {
        window.close();
        destPipe.close();
    }

    /**
     * Gets the output pipe for sending responses back to this client.
     *
     * @return output pipe
     */
    public OutputPipe getOutputPipe() {
        return destPipe;
    }
    
    /**
     * Gets the ContentShare object that this transfer session is
     * serving.
     * 
     * @return content share instance
     */
    public DefaultContentShare getContentShare() {
        return share;
    }
}
