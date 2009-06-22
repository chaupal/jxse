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

/*
 * Design notes:
 *    An alternative approach would be to use individual flags, such as
 *    an enumeration with an EnumSet.  After experimenting with both approaches,
 *    I selected this one as the best, even though the helper method needed
 *    to be created to achieve bit-like operation on the "locating" bit.  The
 *    main reason for this was that the valid combinations of "bits" can be
 *    checked at compile time, versus allowing poor transfer implementations
 *    to do whatever they see fit.  Point-of-use code also cleaned up quite
 *    a bit when using this approach.
 */

/**
 * Enumeration used to represent the remote source location status provided by
 * TransferListener events.  Content source location is the process of
 * locating potential remote data sources for a specific desired Content
 * instance.  The definition of "enough" and "many" remote sources is left
 * up to each ContentProvider/ContentTransfer implementation.
 * 
 * @see net.jxta.content.ContentTransfer
 * @see net.jxta.content.ContentProvider
 */
public enum ContentSourceLocationState {

    /**
     * Source location is not currently running and too few remote sources
     * have been identified to attempt retrieval.
     */
    NOT_LOCATING(false, false, false),

    /**
     * Source location is not currently running but enough remote sources
     * have been identified to attempt retrieval, though more would be better.
     */
    NOT_LOCATING_HAS_ENOUGH(false, true, false),

    /**
     * Source location is not currently running but many remote sources
     * have been identified and retrieval can proceed optimally.  Locating
     * more sources would have diminishing returns at this point.
     */
    NOT_LOCATING_HAS_MANY(false, true, true),

    /**
     * Source location is currently running but too few remote sources
     * have yet been identified to attempt retrieval.
     */
    LOCATING(true, false, false),

    /**
     * Source location is currently running and enough remote sources
     * have been identified to attempt retrieval, though more would be better.
     */
    LOCATING_HAS_ENOUGH(true, true, false),

    /**
     * Source location is currently running and many remote sources
     * have been identified such that retrieval can proceed optimally.
     * Locating more sources would have diminishing returns at this point.
     */
    LOCATING_HAS_MANY(true, true, true);

    /**
     * Flag indicating that the state is actively locating more remote sources.
     */
    private final boolean amLocating;

    /**
     * Flag indicating that enough remote sources have been identified to
     * attempt retrieval.
     */
    private final boolean haveEnough;

    /**
     * Flag indicating that an optimal number of remote sources have been
     * identified.
     */
    private final boolean haveMany;

    /**
     * Constructor.
     *
     * @param locating true if the state represents active location activity
     * @param enough true if "enough" sources are on hand
     * @param many true if "many" sources have been identified
     */
    ContentSourceLocationState(boolean locating, boolean enough, boolean many) {
        amLocating = locating;
        haveEnough = enough;
        haveMany = many;
    }

    /**
     * Determines whether or not remote source location is actively proceeding
     * for this state.
     *
     * @return true if remote source location is taking place, false otherwise
     */
    public boolean isLocating() {
        return amLocating;
    }

    /**
     * Determines whether or not enough remote sources have been identified
     * to attempt retrieval.
     *
     * @return true if enough remote sources have been identified, false
     *  otherwise
     */
    public boolean hasEnough() {
        return haveEnough;
    }

    /**
     * Determines whether or not an optimal number of remote sources have been
     * identified.
     *
     * @return true if many remote sources have been identified, false
     *  otherwise
     */
    public boolean hasMany() {
        return haveMany;
    }

    /**
     * Returns the state which is equivalent to the current state, only
     * with source location enabled or disabled, depending on the
     * value of the locating parameter.
     *
     * @param locating true to enable the locating flag, false otherwise
     * @return new state, possibly unchanged
     */
    public ContentSourceLocationState getEquivalent(boolean locating) {
        switch(this) {
            case LOCATING:
                if (locating) {
                    return this;
                } else {
                    return NOT_LOCATING;
                }
            case LOCATING_HAS_ENOUGH:
                if (locating) {
                    return this;
                } else {
                    return NOT_LOCATING_HAS_ENOUGH;
                }
            case LOCATING_HAS_MANY:
                if (locating) {
                    return this;
                } else {
                    return NOT_LOCATING_HAS_MANY;
                }
            case NOT_LOCATING:
                if (locating) {
                    return LOCATING;
                } else {
                    return this;
                }
            case NOT_LOCATING_HAS_ENOUGH:
                if (locating) {
                    return LOCATING_HAS_ENOUGH;
                } else {
                    return this;
                }
            case NOT_LOCATING_HAS_MANY:
                if (locating) {
                    return LOCATING_HAS_MANY;
                } else {
                    return this;
                }
            default:
                // Should never happen
                if (locating) {
                    return LOCATING;
                } else {
                    return NOT_LOCATING;
                }
        }
    }

}
