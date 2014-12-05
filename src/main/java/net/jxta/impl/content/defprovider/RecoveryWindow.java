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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import net.jxta.logging.Logger;
import net.jxta.logging.Logging;

/**
 * This class wraps data access to an InputStream data source, allowing the
 * streamed data source to be treated as a limited random-access data source.
 * All data retreived from the stream is softly referenced, allowing the user
 * to re-retrieve data which was has been previously retrieved.  This provides
 * a recovery window which can be used to serve data request retries while
 * not (re)opening the original stream.
 */
public class RecoveryWindow {
    private static final Logger LOG =
            Logging.getLogger(RecoveryWindow.class.getName());

    /**
     * Maximum number of bytes per node.
     */
    private static final int maxNodeLength =
            Integer.getInteger(RecoveryWindow.class.getName()
            + ".maxNodeLength", 10 * 1024).intValue();

    private InputStream in;
    private Node head;

    /**
     * Node to be used in unidirectional, soft referenced, linked list.
     */
    private static class Node {
        public long offset;
        public byte[] data;
        public Reference<Node> previous;
        public Node next;
    }

    /**
     * Creates a new recovery window, wrapping the source stream provided.
     */
    public RecoveryWindow(InputStream in) {
        this.in = in;
    }

    /**
     * Returns the node which is at or before the starting offset supplied.
     * If the source stream has not yet read the entire span of data requested,
     * the source stream will be read, creating nodes along the way.  If the
     * beginning of the data is no longer available, an IOException will be
     * thrown.
     */
    public synchronized int getData(long offset, int length, OutputStream out)
    throws IOException {
        Node node = head;
        Node last = null;
        byte[] tooBig;
        int adjustedLen;
        int idx;
        int len;
        int read;
        int totalRead;
        int written = 0;

        // LOGGING: was Finer
        Logging.logCheckedDebug(LOG, "Data request: offset=", offset, ", length=", length);

        try {

            // Walk backwards through exiting nodes, looking for an appropriate
            // starting point.
            if (head != null) {

                // LOGGING: was Finest
                Logging.logCheckedDebug(LOG, "Walking backwards to find starting node");

                idx=0;

                while (node != null) {

                    // LOGGING: was Finest
                    Logging.logCheckedDebug(LOG, "Now at node: offset=", node.offset, ", length=", node.data.length);

                    if (node.offset <= offset) {
                        // Beginning of chain found.
                        break;
                    }

                    idx++;
                    node = (Node) node.previous.get();

                }

                if (node == null) {

                    // Cannot recover data that far back.
                    // LOGGING: was Finest
                    Logging.logCheckedDebug(LOG, "Data requested extends beyond recovery window");
                    throw(new IOException("Data requested extends beyond recovery window"));

                }

                // LOGGING: was Finest
                Logging.logCheckedDebug(LOG, "Walked backwards ", idx, " nodes from head");

            }

            // Walk forwards through nodes that we do have
            // LOGGING: was Finest
            Logging.logCheckedDebug(LOG, "Beginning forward walk");

            while (node != null) {

                // LOGGING: was Finest
                Logging.logCheckedDebug(LOG, "Now at node: offset=", node.offset, ", length=", node.data.length);

                idx = (int) (offset - node.offset);
                adjustedLen = node.data.length - idx;
                len = (adjustedLen > length) ? length : adjustedLen;

                if (len > 0) {

                    // LOGGING: was Finest
                    Logging.logCheckedDebug(LOG, "Writing: idx=", idx, ", len=", len );

                    out.write(node.data, idx, len);
                    written += len;
                    offset += len;
                    length -= len;

                }

                if (length == 0) {

                    // No more data is required.
                    // Already, know you, that which you need.
                    // LOGGING: was Finest
                    Logging.logCheckedDebug(LOG, "Request fulfilled.  written=", written);

                    return written;

                }

                if (node.next == node) {

                    // This is EOF.  We're done.
                    // LOGGING: was Finest
                    Logging.logCheckedDebug(LOG, "EOF encountered.  written=", -written);

                    return -written;

                }

                last = node;
                node = node.next;

            }

            // Now try to read the data we dont have
            // Walk forwards through nodes that we do have
            // LOGGING: was Finest
            Logging.logCheckedDebug(LOG, "Beginning new data reads");

            while (length > 0) {

                // Figure out where this node starts and ends
                idx = (int) ((last == null) ? 0 : (last.offset + last.data.length));

                if (idx < offset) {

                    // We need to save and pass data before the requested position
                    len = (int) (offset - idx);

                } else {

                    // We can collect the desired data
                    len = length;

                }

                if (len > maxNodeLength) {
                    len = maxNodeLength;
                }

                // LOGGING: was Finest
                Logging.logCheckedDebug(LOG, "Now at: offset=", idx, ", length=", len);

                // Allocate and link the new node
                node = new Node();
                node.data = new byte[len];
                node.offset = idx;

                if (last != null) {
                    node.previous = new SoftReference<Node>(last);
                    last.next = node;
                }

                head = node;

                // LOGGING: was Finest
                Logging.logCheckedDebug(LOG, "Allocated new data node.  offset=", node.offset,
                            ", length=", node.data.length);

                // Read in the node's data
                totalRead = 0;

                while (totalRead < node.data.length) {

                    read = in.read(node.data, totalRead, node.data.length - totalRead);

                    if (read < 0) {

                        // EOF.  Create resized copy of data and stop.
                        tooBig = node.data;
                        node.data = new byte[totalRead];
                        System.arraycopy(tooBig, 0, node.data, 0, totalRead);
                        node.next = node;

                        // LOGGING: was Finest
                        Logging.logCheckedDebug(LOG, "Reallocating node.  offset=", node.offset,
                                    ", len=", node.data.length);

                    } else {

                        totalRead += read;

                    }

                }

                // Write the node's data
                if (idx == offset) {

                    // LOGGING: was Finest
                    Logging.logCheckedDebug(LOG, "Writing node data.  offset=", node.offset,
                                ", len=", node.data.length);

                    out.write(node.data);
                    written += node.data.length;
                    offset += node.data.length;
                    length -= node.data.length;

                }

                if (node.next == node) {

                    // This was EOF.  We're done.
                    // LOGGING: was Finest
                    Logging.logCheckedDebug(LOG, "EOF encountered.  written=", -written);
                    return -written;

                }

                last = node;

            }

            // LOGGING: was Finest
            Logging.logCheckedDebug(LOG, "Request fulfilled.  written=", written);

        } catch (Exception x) {

            Logging.logCheckedDebug(LOG, "Uncaught exception\n", x);

        }
        return written;
    }

    /**
     * Releases all resources.
     */
    public void close() throws IOException {
        try {
            head = null;
            in.close();
        } finally {
            in = null;
        }
    }

}
