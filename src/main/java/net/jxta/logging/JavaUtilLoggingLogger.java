package net.jxta.logging;

import java.util.logging.Level;

public class JavaUtilLoggingLogger implements Logger {

	// Convert "xx {} yy {} zz" to "xx {0} yy {1} zz"
	static String slf4jFormatToJULFormat(final String slf4jFormat) {
		if (slf4jFormat == null) {
			return null;
		}
		final StringBuilder sb = new StringBuilder();
		int markerCount = 0;
		for (int i = 0; i < slf4jFormat.length(); i++) {
			final char ch = slf4jFormat.charAt(i);
			sb.append(ch);
			if (ch == '{') {
				if (i != slf4jFormat.length() - 1) {
					if (slf4jFormat.charAt(i + 1) == '}') {
						sb.append(markerCount ++);
						sb.append('}');
						i++;
					}
				}
			}
		}
		return sb.toString();
	}

	private final java.util.logging.Logger logger;
	
	public JavaUtilLoggingLogger(final String className) {
		logger = java.util.logging.Logger.getLogger(className);
	}
	
	public JavaUtilLoggingLogger(final Class<?> loggerClass) {
		this(loggerClass.getName());
	}

	public boolean isDebugEnabled() {
		return logger.isLoggable(Level.FINE);
	}

	public void debug(final String message) {
		logger.fine(message);
	}

	public void debugParams(String format, Object... params) {
		logger.log(Level.FINE, JavaUtilLoggingLogger.slf4jFormatToJULFormat(format), params);
	}

	public void info(final String message, final Throwable t) {
		logger.log(Level.INFO, message, t);
	}

	public void infoParams(final String format, final Object... params) {
		logger.log(Level.INFO, JavaUtilLoggingLogger.slf4jFormatToJULFormat(format), params);
	}

	public boolean isInfoEnabled() {
		return logger.isLoggable(Level.INFO);
	}

	public void info(final String message) {
		logger.info(message);
	}

	public void warn(final String message) {
		logger.warning(message);
	}

	public void warn(final String message, final Throwable t) {
		logger.log(Level.WARNING, message, t);
	}

	public void warnParams(final String format, final Object... params) {
		logger.log(Level.WARNING, JavaUtilLoggingLogger.slf4jFormatToJULFormat(format), params);
	}

	public boolean isWarnEnabled() {
		return logger.isLoggable(Level.WARNING);
	}

	public void error(final String message) {
		logger.severe(message);
	}
	
	public void error(final String message, final Throwable t) {
		logger.log(Level.SEVERE, message, t);
	}

	public boolean isErrorEnabled() {
		return logger.isLoggable(Level.SEVERE);
	}

	public boolean isConfigEnabled() {
		// TODO work out what to do here - just log as fine for now
		return logger.isLoggable(Level.FINE);
	}

	public void config(final String message) {
		// TODO work out what to do here - just log as fine for now
		logger.fine(message);
	}
}
