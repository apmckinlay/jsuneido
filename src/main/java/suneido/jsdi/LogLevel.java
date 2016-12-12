package suneido.jsdi;

/**
 * <p>
 * Enumerates dynamic logging verbosity levels available in the DLL.
 * </p>
 *
 * <p>
 * Not all log levels will necessarily be available. The maximum log level
 * depends on the static log threshold set in the DLL.
 * </p>
 *
 * @author Victor Schappert
 * @see JSDI#getLogThreshold()
 * @see JSDI#setLogThreshold(LogLevel)
 * @since 20140730
 */
@DllInterface
public enum LogLevel {

	NONE,
	FATAL,
	ERROR,
	WARN,
	INFO,
	DEBUG,
	TRACE;
	
}
