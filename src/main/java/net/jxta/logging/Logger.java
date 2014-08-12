package net.jxta.logging;

/**
 * Our abstraction for loggers; implementations of this use slf4j and
 * java.util.logging.
 * 
 */
public interface Logger {
	boolean isDebugEnabled();
	void debug(String message);
	void debugParams(String format, Object ... params);

	boolean isInfoEnabled();
	void info(String message);
	void info(String message, Throwable t);
	void infoParams(String format, Object ... params);

	// TODO rename to warn
	boolean isWarningLoggable(); // TODO rename to ..Enabled
	void warning(String message);
	void warning(String message, Throwable t);
	void warnParams(String format, Object ... params); // TODO rename to warningParams

	// TODO rename to error
	boolean isSevereLoggable(); // TODO rename to ..Enabled
	void severe(String message);
	void severe(String message, Throwable t);

	boolean isConfigEnabled();
	void config(String message);
}
