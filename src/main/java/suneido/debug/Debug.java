package suneido.debug;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import suneido.SuInternalError;
import suneido.boot.NativeLibrary;
import suneido.boot.Platform;

/**
 * Contains methods used to control the Suneido debug system.
 *
 * @author Victor Schappert
 * @since 20140813
 */
public final class Debug {

	/**
	 * Returns the path to the {@link suneido.boot.Platform platform}
	 * -appropriate jSuneido debug support library&mdash;which is a JVMTI
	 * agent&mdash;or {@code null} if the agent library cannot be found for any
	 * reason.
	 *
	 * @return Debug support JVMTI agent path, or {@code null}
	 */
	public static File getJVMTIAgentPath() {
		String libraryName = getLibraryName();
		if (null == libraryName) {
			return null;
		}
		return NativeLibrary.persistLibrary(libraryName);
	}

	/**
	 * Returns a command-line argument suitable for starting the JVM with the
	 * {@code jdwp} JVMTI agent running in server mode on IP port
	 * {@value #JDWP_PORT} of the local machine.
	 *
	 * @return JVM command-line argument to start {@code jdwp}
	 * @SEE {@link #getJDWPAgentServerPort()}
	 */
	public static String getJDWPAgentArg() {
		return "-agentlib:jdwp=server=y,suspend=n,transport=dt_socket,address=localhost:"
				+ getJDWPAgentServerPort();
	}

	/**
	 * <p>
	 * Returns an available port number that a new JVM can use to bind a JDWP
	 * agent server to.
	 * </p>
	 *
	 * <p>
	 * The port number returned is one that <em>appeared to be</em> available
	 * at the time this method ran its availability checks. However, since this
	 * method does not cause the process to bind to the port number returned,
	 * it is conceivable the port may no longer be available either by the time
	 * this method returns or at some future time after the return of the method
	 * but before the method caller is able to cause an attempted bind to the
	 * port. Therefore, the caller's use of the port number <strong>must be
	 * robust</strong> enough to recover from a failure to bind.
	 * </p>
	 *
	 * @return Port number in string form
	 * @throw SuInternalError If it is not possible to locate any open port
	 * @see #getJDWPAgentClientPort() 
	 * @see #getJDWPAgentArg()
	 */
	public static String getJDWPAgentServerPort() {
		// First attempt: use the default port
		try (ServerSocket test = new ServerSocket(JDWP_DEFAULT_PORT)) {
			return Integer.toString(JDWP_DEFAULT_PORT);
		} catch (Exception e) {
			// Squelch
		}
		// Second attempt: use any available port
		try (ServerSocket test = new ServerSocket(0)) {
			return Integer.toString(test.getLocalPort());
		} catch (IOException e) {
			throw new SuInternalError(
					"I/O error while looking for open jdwp socket", e);
		} catch (SecurityException e) {
			throw new SuInternalError(
					"security error while looking for jdwp socket", e);
		}
	}

	/**
	 * <p>
	 * Returns the IP port on the local machine where the JDWP agent server is
	 * listening. This value is {@code null} if there is no JDWP agent
	 * listening.
	 * </p>
	 * 
	 * @return Port number in string form, or {@code null}
	 * @see #DEBUG_PORT_PROP_NAME
	 * @see #getJDWPAgentServerPort()
	 */
	public static String getJDWPAgentClientPort() {
		return System.getProperty(DEBUG_PORT_PROP_NAME);
	}

	//
	// CONSTANTS
	//

	/**
	 * <p>
	 * Name of system property used to communicate the IP port on the local
	 * machine where the JDWP agent server is listening (<em>ie</em> where the
	 * JDI client should listen).
	 * </p>
	 *
	 * @see #getJDWPAgentClientPort()
	 */
	public static final String DEBUG_PORT_PROP_NAME = "suneido.debug.port";

	//
	// INTERNALS
	//

	private static final String LIBRARY_NAME_ROOT = "jsdebug";

	private static final int JDWP_DEFAULT_PORT = 3457;

	private static String getLibraryName() {
		switch (Platform.getPlatform()) {
		case WIN32_AMD64:	// intentional fall-through
		case WIN32_X86:		// intentional fall-through
		case LINUX_AMD64:
			return LIBRARY_NAME_ROOT;
		case UNKNOWN_PLATFORM:
			return null;
		default:
			throw SuInternalError.unhandledEnum(Platform.getPlatform());
		}
	}
}
