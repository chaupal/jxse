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

package net.jxta.content;

/**
 * Enumeration used to represent the transfer state provided by
 * TransferListener events.
 */
public enum ContentTransferState {

    /**
     * Transfer has not yet started.
     */
    PENDING(false, false),

    /**
     * Transfer has started and is in progress.
     */
    RETRIEVING(true, false),

    /**
     * Transfer has started but has stalled and is no longer making progress.
     * Once retrieval recommences, the state should be transitioned back to
     * an active state, such as RETRIEVING.  Use of this state is informational
     * only and is considered optional.
     */
    STALLED(true, false),

    /**
     * Transfer has started, non-terminally failed, and needs to be retried
     * in order to proceed further.  Once retrieval recommences, the state
     * should be transitioned back to an active state, such as RETRIEVING.
     * Use of this state is informational only and is considered optional.
     */
    RETRYING(true, false),

    /**
     * The transfer has started and completed successfully.  The Content is
     * ready to be retrieved from the transfer object.
     */
    COMPLETED(false, true),

    /**
     * The transfer was started but encountered a terminal failure, failing
     * to complete Content retrieval successfully.  Retrying is not likely to
     * result in a successful retrieval.
     */
    FAILED(false, true),

    /**
     * The transfer was programmatically cancelled.  This can occur before,
     * during, or after the retrieval attempt.
     */
    CANCELLED(false, true);

    /**
     * Flag indicating that retrival is underway.
     */
    private final boolean amRetrieving;

    /**
     * Flag indicating that the current state is a final state.
     */
    private final boolean amFinished;

    /**
     * Constructor.
     *
     * @param retrieving true if state represents active retrieval
     * @param finished true if state represents a final/terminal state
     */
    ContentTransferState(boolean retrieving, boolean finished) {
        amRetrieving = retrieving;
        amFinished = finished;
    }

    /**
     * Determines whether or not the current state reflects a state in which
     * Content retrieval is taking place.
     *
     * @return true if state represents active retrieval
     */
    public boolean isRetrieving() {
        return amRetrieving;
    }

    /**
     * Determines whether or not the current state reflects a final state,
     * either of success or failure.
     *
     * @return true is this state represents a final/terminal state
     */
    public boolean isFinished() {
        return amFinished;
    }

    /**
     * True if and only if the state is COMPLETED.
     *
     * @return true if state is COMPLETED, false otherwise
     */
    public boolean isSuccessful() {
        if (this == COMPLETED) {
            return true;
        } else {
            return false;
        }
    }

}
