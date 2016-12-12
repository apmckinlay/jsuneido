/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

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
public final class DebugUtil {

	/**
	 * Returns the path to the {@link suneido.boot.Platform platform}
	 * -appropriate jSuneido debug (<strong>jsdebug</strong>) support
	 * library&mdash;which is a JVMTI agent&mdash;or {@code null} if the agent
	 * library cannot be found for any reason.
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
	 * {@code jdwpServerPort} of the local machine.
	 *
	 * @param jdwpServerPort
	 *            Port returned by {@link #getFreeJDWPAgentServerPort()}
	 * @return JVM command-line argument to start {@code jdwp}
	 * @see #getJDWPPortPropertyArg(String)
	 */
	public static String getJDWPAgentArg(String jdwpServerPort) {
		return "-agentlib:jdwp=server=y,suspend=n,transport=dt_socket,address="
				+ jdwpServerPort;
	}

/**
	 * Returns a command-line argument suitable for starting a JVM with the
	 * {@value #JDWP_PORT_PROP_NAME} defined and set to the value given in
	 * {@code jdwpServerPort}. This is useful to communicate to a child JVM
	 * process the port on which a JDI client should listen.
	 *
	 * @param jdwpServerPort Port returned by {@link #getFreeJDWPAgentServerPort()
	 * @return JVM command-line argument to define {@value #JDWP_PORT_PROP_NAME}
	 * @see #getJDWPAgentArg(String)
	 */
	public static String getJDWPPortPropertyArg(String jdwpServerPort) {
		return "-D" + JDWP_PORT_PROP_NAME + "=" + jdwpServerPort;
	}

	/**
	 * <p>
	 * Returns an available port number that a new JVM can use to bind a JDWP
	 * agent server to.
	 * </p>
	 *
	 * <p>
	 * The port number returned is one that <em>appeared to be</em> available at
	 * the time this method ran its availability checks. However, since this
	 * method does not cause the process to bind to the port number returned, it
	 * is conceivable the port may no longer be available either by the time
	 * this method returns or at some future time after the return of the method
	 * but before the method caller is able to cause an attempted bind to the
	 * port. Therefore, the caller's use of the port number <strong>must be
	 * robust</strong> enough to recover from a failure to bind.
	 * </p>
	 *
	 * <p>
	 * 2015-01-02 apm - Unfortunately we are <strong>not</strong> robust.
	 * If the port is in use due to a race jSuneido crashes.
	 * Using an ephemeral port (0/null) seems to work, but not guaranteed.
	 * </p>
	 *
	 * @return Port number in string form
	 * @throw SuInternalError on error
	 * @see #getJDWPAgentClientPort()
	 * @see #getJDWPAgentArg()
	 */
	public static String getFreeJDWPAgentServerPort() {
		try (ServerSocket test = new ServerSocket()) {
			test.setReuseAddress(false);
			test.bind(null);
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
	 * @see #JDWP_PORT_PROP_NAME
	 * @see #getFreeJDWPAgentServerPort()
	 */
	public static String getJDWPAgentClientPort() {
		return System.getProperty(JDWP_PORT_PROP_NAME);
	}

	/**
	 * <p>
	 * Returns whether the user has set a system property to indicate that the
	 * bootstrap system should implement {@link DebugModel#ON full debugging}
	 * using JDWP even if a {@link suneido.boot.Platform platform}-appropriate
	 * jSuneido debug (<strong>jsdebug</strong>) support library is available.
	 * </p>
	 *
	 * <p>
	 * Unless the user explicitly sets {@link #JDWP_FORCE_PROP_NAME}, the value
	 * returned will be <strong>{@code false}</strong>.
	 * </p>
	 *
	 * @return True iff the user has forced full debugging to be done using JDWP
	 */
	public static boolean isJDWPForced() {
		return null != System.getProperty(JDWP_FORCE_PROP_NAME);
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
	public static final String JDWP_PORT_PROP_NAME = "suneido.debug.port";

	/**
	 * <p>
	 * Name of system property used to force {@link DebugModel#ON full
	 * debugging} to be implemented using the JDWP approach rather than using
	 * the {@link suneido.boot.Platform platform}-appropriate jSuneido debug
	 * (<strong>jsdebug</strong>) support library.
	 * </p>
	 *
	 * <p>
	 * This properly is primarily useful for debugging the {@link JDWPAgentClient
	 * JDWP client} in situations where the bootstrapper would normally load
	 * the <strong>jsdebug</strong> agent.
	 * </p>
	 *
	 * @see #isJDWPForced()
	 */
	public static final String JDWP_FORCE_PROP_NAME = "suneido.debug.force-jdwp";

	//
	// INTERNALS
	//

	private static final String LIBRARY_NAME_ROOT = "jsdebug";

	private static String getLibraryName() {
		switch (Platform.getPlatform()) {
		case WIN32_AMD64: // intentional fall-through
		case WIN32_X86: // intentional fall-through
		case LINUX_AMD64:
			return LIBRARY_NAME_ROOT;
		case UNKNOWN_PLATFORM:
			return null;
		default:
			throw SuInternalError.unhandledEnum(Platform.getPlatform());
		}
	}
}
