/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc. All rights reserved.
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

package net.jxta.test.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * Extension of the {@code File} class which registers itself for recursive
 * deletion upon finalization and/or shutdown hook execution.  This provides
 * a simple, temporary location for other applications to leverage.
 */
public class TempDir extends File {

    /**
     * Serializer version ID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * List of all files to delete on shutdown.
     */
    private static final Set<File> toDelete = new HashSet<File>();

    /**
     * Shutdown hook to run on JVM shutdown.  A call to <code>shutdown</code>
     * prior to JVM exit will deregister this hook.
     */
    private static final Thread shutdownHook = createShutdownHook();

    ///////////////////////////////////////////////////////////////////////////
    // Constructors:

    /**
     * Passthrough constructor to expose standard {@code File} operations.
     * If a file/directory already exists at the path specified, it will be
     * deleted.
     * 
     * @param parent The parent abstract pathname
     * @param child The child pathname string
     */
    public TempDir(final File parent, final String child) {
        super(parent, child);
        checkDirectory();
    }

    /**
     * Passthrough constructor to expose standard {@code File} operations.
     * If a file/directory already exists at the path specified, it will be
     * deleted.
     * 
     * @param pathname A pathname string
     */
    public TempDir(final String pathname) {
        super(pathname);
        checkDirectory();
    }

    /**
     * Passthrough constructor to expose standard {@code File} operations.
     * If a file/directory already exists at the path specified, it will be
     * deleted.
     * 
     * @param parent The parent pathname string
     * @param child The child pathname string
     */
    public TempDir(final String parent, final String child) {
        super(parent, child);
        checkDirectory();
    }

    /**
     * Passthrough constructor to expose standard {@code File} operations.
     * If a file/directory already exists at the path specified, it will be
     * deleted.
     * 
     * @param uri An absolute, hierarchical URI with a scheme equal to "file",
     *  a non-empty path component, and undefined authority, query, and
     *  fragment components
     */
    public TempDir(final URI uri) {
        super(uri);
        checkDirectory();
    }

    /**
     * Constructor which creates a temporary directory using the {@code File}
     * provided.  If a file/directory already exists at the path specified,
     * it will be deleted.
     * 
     * @param dir file to use  as temp directory
     * @throws IOException if the direcotry could not be created
     */
    public TempDir(File dir) throws IOException {
        super(dir.toURI());
        checkDirectory();
    }

    /**
     * Constructor which creates a temporary directory of an arbitrary name,
     * leveraging {@code File.createTempFile()} to create a unique directory.
     * 
     * @throws IOException if the direcotry could not be created
     */
    public TempDir() throws IOException {
        super(File.createTempFile(
                TempDir.class.getName() + "-", ".tmp").toURI());
        checkDirectory();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Public methods:

    /**
     * {@inheritDoc}
     * 
     * Recursively deletes the temporary directory and all of it's contents.
     * 
     * @return true if and only if the file or directory is successfully
     *  deleted; false otherwise
     */
    @Override
    public boolean delete() {
        return clear() && super.delete();
    }

    /**
     * Clears the directory of all files and subdirectories.
     * 
     * @return true if and only if the file or directory is successfully
     *  cleared out; false otherwise
     */
    public boolean clear() {
        boolean result = true;
        File[] children = listFiles();
        if (children != null) {
            for (File child : children) {
                result &= recursiveDelete(child);
            }
        }
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected methods:

    /**
     * Finalizer which simply calls <code>delete()</code> in an attempt to
     * cleanup when abandoned.
     */
    @Override
    protected void finalize() {
        delete();
        synchronized(toDelete) {
            // De-register the shutdown hook reference
            toDelete.remove(this);
            if (toDelete.size() == 0) {
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } catch (IllegalStateException isx) {
                    // Ignore.  We're already shutting down.
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods:

    /**
     * Creates and registers a shutdown thread to hopefully ensure that
     * all data files are removed from the local system.
     * 
     * @return shutdown thread instance
     */
    private static Thread createShutdownHook() {
        Thread hook = new Thread(new Runnable() {
            /**
             * Call the shutdown method.
             */
            public void run() {
                synchronized(toDelete) {
                    for (File file : toDelete) {
                        file.delete();
                    }
                }
            }
        });
        hook.setName(
                TempDir.class.getSimpleName() + " shutdown hook");
        return hook;
    }

    /**
     * Recursively deletes the specified file/directory.
     * 
     * @param file file or directory to delete
     * @return true if delete was successful, false otherwise
     */
    private boolean recursiveDelete(final File file) {
        boolean result = true;
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                result &= recursiveDelete(child);
            }
        }
        result = result &= file.delete();
        return result;
    }

    /**
     * Double checks that the directory is ready to roll.
     */
    private void checkDirectory() {
        if (exists()) {
            if (!delete()) {
                throw(new IllegalStateException(
                        "Temp directory exists but could not be deleted: "
                        + getPath()));
            }
        }
        if (!mkdirs()) {
            throw(new IllegalStateException(
                    "Temp directory could not be created: "
                    + getPath()));
        }
        if (!isDirectory()) {
            throw(new IllegalStateException(
                    "Temp directory is not a directory: "
                    + getPath()));
        }
        if (!canRead()) {
            throw(new IllegalStateException(
                    "Temp directory is not readable: "
                    + getPath()));
        }
        if (!canWrite()) {
            throw(new IllegalStateException(
                    "Temp directory is not writable: "
                    + getPath()));
        }
        /* Java 6 only
        if (!canExecute()) {
            throw(new IllegalStateException(
                    "Temp directory is not executable: "
                    + this.getPath()));
        }
         */

        // Add it to the shutdown hook's cleanup list
        synchronized(toDelete) {
            toDelete.add(this);
            if (toDelete.size() == 1) {
                Runtime.getRuntime().addShutdownHook(shutdownHook);
            }
        }
    }

}
