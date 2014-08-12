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

	boolean isWarnEnabled();
	void warn(String message);
	void warn(String message, Throwable t);
	void warnParams(String format, Object ... params);

	boolean isErrorEnabled();
	void error(String message);
	void error(String message, Throwable t);

	boolean isConfigEnabled();
	void config(String message);
}
