/*
 *  Copyright (c) 2001-2004 Sun Microsystems, Inc. All rights reserved.
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

package net.jxta.logging;

import java.io.PrintStream;
import java.util.logging.Level;

/**
 * This class defines constants for JXTA JSE's logging facilities. In this
 * implementation the constants are initialized based upon the value of the
 * Java System property {@code net.jxta.logging.Logging}. This implementation
 * defines all of the public constants as {@code final} which enables the
 * JVM/JIT to optimize out the logging code when appropriately configured.
 * <p/>
 * <p/>Alternate implementations of this class could;
 * <p/>
 * <ul>
 * <li>Initialize the public constants with manifest constants, ie.
 * {@code true} or {@code false} which would allow the Java compiler to
 * optimize out logging code at compile time.</li>
 * <p/>
 * <li>Remove the {@code final} qualifier from the constants and provide
 * additional methods to dynamically set the logging configuration at runtime.
 * </li>
 * </ul>
 *
 * By default, JXTA uses slf4j API based loggers, with no dependency on any
 * specific slf4j backend, or adapter. You can switch to using
 * java.util.logging directly by defining:
 * <pre>
 * <code>
 * System.setProperty("net.jxta.logging.LoggingImpl", "jul");
 * </code>
 * </pre>
 *   
 * To control the logging level within applications:
 * <pre>
 * <code>
 * System.setProperty("net.jxta.logging.Logging", "FINE");
 * System.setProperty("net.jxta.level", "FINE");
 * </code>
 * </pre>
 * The values of this property are FINE, INFO, CONFIG, WARNING, SEVERE. 
 * 
 * If using java.util.logging, configure the logging with:
 * <pre>
 * <code>
 * System.setProperty("java.util.logging.config.file", "/home/userhome/logging.properties");
 * </code>
 * </pre>
 *
 * Note that under java.util.logging, FINE is the finest level we use; there
 * is no longer any FINER or FINEST logging in JXTA (all usage at these levels
 * has been converted to FINE).
 * 
 * <p/>
 * Sample logging properties :
 * <p/>
 * <pre>
 * <code>
 * # default file output is in user's home directory.
 * java.util.logging.FileHandler.pattern = %h/java%u.log
 * java.util.logging.FileHandler.limit = 50000
 * java.util.logging.FileHandler.count = 1
 * java.util.logging.FileHandler.formatter = java.util.logging.XMLFormatter
 * 
 * # Limit the message that are printed on the console to INFO and above
 * java.util.logging.ConsoleHandler.level = FINE
 * java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter
 *
 * # Facility specific properties.
 * # Provides extra control for each logger.
 * #
 * # For example, set the net.jxta.impi.pipe.PipeResolver logger to only log SEVERE
 * # messages:
 * net.jxta.impi.pipe.PipeResolver.level = FINE
 * </code>
 * </pre>
 */
public final class Logging {

    private final static Logger LOG = getLogger(Logging.class);

    /**
     * The name of the system property from which we will attempt to read our
     * logging implementation configuration.
     */
    public final static String JXTA_LOGGING_IMPL_PROPERTY = "net.jxta.logging.LoggingImpl";
    public static boolean useJUL = false;

    /**
     * The name of the system property from which we will attempt to read our
     * logging level configuration.
     */
    public final static String JXTA_LOGGING_PROPERTY = "net.jxta.logging.Logging";

    /**
     * The default logging level.
     */ 
    private final static Level DEFAULT_LOGGING_LEVEL = Level.INFO;

    /**
     * The logging level for this run.
     */
    public final static Level MIN_SHOW_LEVEL;

    /**
     * Is debug logging enabled?
     */
    public final static boolean SHOW_DEBUG;

    /**
     * Is configuration logging enabled?
     */
    public final static boolean SHOW_CONFIG;

    /**
     * Is informational logging enabled?
     */
    public final static boolean SHOW_INFO;

    /**
     * Is warning logging enabled?
     */
    public final static boolean SHOW_WARNING;

    /**
     * Is error logging enabled?
     */
    public final static boolean SHOW_ERROR;

    /* Initialize the constants */
    static {

        Level setLevel = DEFAULT_LOGGING_LEVEL;

        try {
            String loggingImpl = System.getProperty(JXTA_LOGGING_IMPL_PROPERTY);

            if (null != loggingImpl) {
                useJUL = loggingImpl.equalsIgnoreCase("jul");
            }
        } catch (SecurityException disallowed) {
            LOG.warn("Could not read configuration property.", disallowed);
        }

        try {
            String propertyLevel = System.getProperty(JXTA_LOGGING_PROPERTY);

            if (null != propertyLevel) {
                setLevel = Level.parse(propertyLevel);
            }
        } catch (SecurityException disallowed) {
            LOG.warn("Could not read configuration property.", disallowed);
        }

        // Set the default level for the JXTA packages so that everything below
        // inherits our default.
        MIN_SHOW_LEVEL = setLevel;

        SHOW_DEBUG = MIN_SHOW_LEVEL.intValue() <= Level.FINE.intValue();
        SHOW_CONFIG = MIN_SHOW_LEVEL.intValue() <= Level.CONFIG.intValue();
        SHOW_INFO = MIN_SHOW_LEVEL.intValue() <= Level.INFO.intValue();
        SHOW_WARNING = MIN_SHOW_LEVEL.intValue() <= Level.WARNING.intValue();
        SHOW_ERROR = MIN_SHOW_LEVEL.intValue() <= Level.SEVERE.intValue();

        logCheckedConfig(LOG, "Logging enabled for level : ", MIN_SHOW_LEVEL);

    }

    /**
     * This class is not meant be instantiated.
     */
    private Logging() {}

    /**
     * Factory method for Logger instances.
     * @param className the name of the class to generate a Logger for.
     * @return a Logger, currently a java.util.logging one.
     */
    public static Logger getLogger(final String className) {
    	if (useJUL) {
    		return new JavaUtilLoggingLogger(className);
    	} else {
    		return new SLF4JLogger(className);
    	}
    }

    /**
     * Factory method for Logger instances.
     * @param loggerClass the class to generate a Logger for.
     * @return a Logger, currently a java.util.logging one.
     */
    public static Logger getLogger(final Class<?> loggerClass) {
    	if (useJUL) {
    		return new JavaUtilLoggingLogger(loggerClass.getName());
    	} else {
    		return new SLF4JLogger(loggerClass);
    	}
    }

	private static String logParametersToString(final Object... inMsg) {
		final StringBuffer Msg = new StringBuffer(getCaller(new Exception().getStackTrace())).append('\n');
		for (int i=0;i<inMsg.length;i++) {
			Msg.append(checkForThrowables(inMsg[i]));
		}
		return Msg.toString();
	}

    /**
     * This method checks whether {@code SHOW_CONFIG} is set to {@code true),
     * and whether the provided logger allows config messages. If yes, the
     * message is logged.
     *
     * @param inLog a logger
     * @param inMsg the messages to concatenate
     */
    public static void logCheckedConfig(Logger inLog, Object... inMsg) {

        if (Logging.SHOW_CONFIG && inLog.isConfigEnabled()) {
            inLog.config(logParametersToString(inMsg));
        }

    }

    /**
     * This method checks whether {@code SHOW_DEBUG} is set to {@code true),
     * and whether the provided logger allows debug messages. If yes, the
     * message is logged.
     *
     * @param inLog a logger
     * @param inMsg the messages to concatenate
     */
    public static void logCheckedDebug(Logger inLog, Object... inMsg) {

        if (Logging.SHOW_DEBUG && inLog.isDebugEnabled()) {
            inLog.debug(logParametersToString(inMsg));
        }

    }

    /**
     * This method checks whether {@code SHOW_INFO} is set to {@code true),
     * and whether the provided logger allows info messages. If yes, the
     * message is logged.
     *
     * @param inLog a logger
     * @param inMsg the messages to concatenate
     */
    public static void logCheckedInfo(Logger inLog, Object... inMsg) {

        if (Logging.SHOW_INFO && inLog.isInfoEnabled()) {
            inLog.info(logParametersToString(inMsg));
        }

    }

    /**
     * This method checks whether {@code SHOW_ERROR} is set to {@code true),
     * and whether the provided logger allows error messages. If yes, the
     * message is logged.
     *
     * @param inLog a logger
     * @param inMsg the messages to concatenate
     */
    public static void logCheckedError(Logger inLog, Object... inMsg) {

        if (Logging.SHOW_ERROR && inLog.isErrorEnabled()) {
            inLog.error(logParametersToString(inMsg));
        }

    }

    /**
     * This method checks whether {@code SHOW_WARNING} is set to {@code true),
     * and whether the provided logger allows warnings messages. If yes, the
     * message is logged.
     *
     * @param inLog a logger
     * @param inMsg the messages to concatenate
     */
    public static void logCheckedWarning(Logger inLog, Object... inMsg) {

        if (Logging.SHOW_WARNING && inLog.isWarnEnabled()) {
            inLog.warn(logParametersToString(inMsg));
        }

    }

    /**
     * Retrieves the stack trace as a String if object is a {@code Throwable}.
     *
     * @param inObj
     * @return
     */
    private static String checkForThrowables(Object inObj) {

        // Are we dealing with a throwable?
        if ( inObj instanceof Throwable ) return retrieveStackTrace((Throwable) inObj);

        // Are we dealing with a null?
        if ( inObj == null ) return "";

         // Returning the object.toString()
        return inObj.toString();

    }

    /**
     * Extracts the calling method using the [1] stack trace element, and creates a
     * string containing the line number, package, class and method name.
     *
     * @param inSTE a stack trace
     * @return the coordinates of the calling method
     */
    public static String getCaller(StackTraceElement[] inSTE) {

        if ( inSTE == null ) {
            LOG.error("Can't get caller: null StackTraceElement");
            return null;
        }

        if ( inSTE.length < 2 ) {
            LOG.error("Can't get caller: StackTraceElement length < 2");
            return null;
        }

        StackTraceElement STE = inSTE[1];

        StringBuffer Result = new StringBuffer();
        Result.append("Line ").append(STE.getLineNumber())
                .append(' ').append(STE.getClassName())
                .append('.').append(STE.getMethodName())
                .append("()");

        return Result.toString();

    }

    /**
     * Returns the stack of method calls (excluding this method) as a String.
     * If a print string is provided, the result is printed to it too.
     *
     * @param inPS a print stream or {@code null}
     * @return a string containing current method call
     */
    public static String getMethodCallsTrace(PrintStream inPS) {

        StackTraceElement[] STE = new Exception().getStackTrace();
        StringBuffer Result = new StringBuffer();

        for (int i=1;i<STE.length;i++) {
            Result.append("Line ").append(STE[i].getLineNumber())
                .append(' ').append(STE[i].getClassName())
                .append('.').append(STE[i].getMethodName())
                .append("()\n");
        }

        String Result2 = Result.toString();
        if ( inPS != null ) inPS.println(Result2);

        return Result2;

    }

    /**
     * Retrieves the stack trace from a throwable.
     *
     * @param t a throwable
     * @return the stack trace as a String
     */
    public static String retrieveStackTrace(Throwable t) {

        StackTraceElement[] STE = t.getStackTrace();
        StringBuffer Result = new StringBuffer();

        Result.append(t.toString()).append('\n');

        for (int i=0;i<STE.length;i++) {
            Result.append("Line ").append(STE[i].getLineNumber())
                .append(' ').append(STE[i].getClassName())
                .append('.').append(STE[i].getMethodName())
                .append("()\n");
        }

        return Result.toString();

    }

}
