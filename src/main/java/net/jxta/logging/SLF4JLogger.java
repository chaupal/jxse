package net.jxta.logging;

public class SLF4JLogger implements Logger {
	private final org.slf4j.Logger logger;

	public SLF4JLogger(final String className) {
		logger = org.slf4j.LoggerFactory.getLogger(className);
	}
	
	public SLF4JLogger(final Class<?> loggerClass) {
		this(loggerClass.getName());
	}
	
	public boolean isFineEnabled() {
		return logger.isDebugEnabled();
	}

	public void fine(final String message) {
		logger.debug(message);
	}

	public void fineParams(String format, Object... params) {
		logger.debug(format, params);
	}

	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	public void info(final String message, final Throwable t) {
		logger.info(message, t);
	}

	public void infoParams(final String format, final Object... params) {
		logger.info(format, params);
	}
	
	public void info(final String message) {
		logger.info(message);
	}

	public void warning(final String message) {
		logger.warn(message);
	}
	
	public void warning(final String message, final Throwable t) {
		logger.warn(message, t);
	}

	public boolean isWarningLoggable() {
		return logger.isWarnEnabled();
	}

	public void warnParams(final String format, final Object... params) {
		logger.warn(format, params);
	}

	public void severe(final String message) {
		logger.error(message);
	}

	public void severe(final String message, final Throwable t) {
		logger.error(message, t);
	}

	public boolean isSevereLoggable() {
		return logger.isErrorEnabled();
	}

	public boolean isConfigEnabled() {
		// TODO work out what to do here - just log as debug for now
		return logger.isDebugEnabled();
	}

	public void config(final String message) {
		// TODO work out what to do here - just log as debug for now
		logger.debug(message);
	}
}
