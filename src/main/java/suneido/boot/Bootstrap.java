package suneido.boot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;

import suneido.SuInternalError;
import suneido.Suneido;
import suneido.debug.Debug;
import suneido.util.Filter;
import suneido.util.FilteredStreamEchoer;

import com.google.common.base.Joiner;

/**
 * <p>
 * Technical entry-point to start Suneido runtime. Will pass control over to the
 * "logical" entry point, {@link suneido.Suneido Suneido}, either in the current
 * JVM or after starting a new JVM.
 * </p>
 *
 * <p>
 * The code for this class should be as small as possible and refer to as few
 * other classes as possible to minimize classloading in the bootstrap JVM if we
 * will have to start Suneido in a newly started JVM anyway.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140813
 */
public final class Bootstrap {

	/**
	 * Entry-point.
	 *
	 * @param args
	 *            Command-line arguments
	 */
	public static void main(String[] args) {
		// If the bootstrip skip flag is set, proceed directly to Suneido
		if (null != System.getProperty(SKIP_BOOT_PROPERTY_NAME)) {
			runSuneidoInThisJVM(args);
			// Otherwise, run the bootstrap process...
		} else {
			String debugOption = DEBUG_OPTION_ALL;
			// Determine the debug option
			for (int k = 0; k < args.length; ++k) {
				final String arg = args[k];
				// Don't attempt to process anything that's not a -debug option
				if (arg.isEmpty() || OPTION_INDICATOR != arg.charAt(0))
					continue;
				if (OPTIONS_END.equals(arg))
					break;
				if (DEBUG_OPTION.equals(arg)) {
					++k;
					if (k < args.length) {
						debugOption = args[k];
					}
				}
			}
			// Process the debug option
			if (DEBUG_OPTION_ALL.equals(debugOption)) {
				try {
					ArrayList<String> fatals = new ArrayList<String>();
					// Start a new JVM, collecting the exit code. Echo the new
					// process's stdout to our stdout, but filter out phrases
					// relating to the bootstrapping process itself. Echo the
					// new process's stderr to our stderr, and try to determine
					// whether error messages printed to stderr indicate a fatal
					// for the new JVM startup that we can recover from by
					// falling back to running Suneido from this JVM.
					int exitCode = runSuneidoInNewJVM(args, true,
							(String x) -> {
								return !isFilterableOutput(x);
							}, (String x) -> {
								if (isFatalErrorPreventingNewJVMStartup(x))
									fatals.add(x);
								return true; // Always echo stderr
						});
					if (0 != exitCode && !fatals.isEmpty()) {
						for (String fatal : fatals) {
							Suneido.errlog("bootstrapper can't start new JVM: "
									+ fatal);
						}
						runSuneidoInThisJVM(args); // Fall back
					} else {
						System.exit(exitCode);
					}
				} catch (SuInternalError e) {
					Suneido.errlog("bootstrapper can't start new JVM", e);
					runSuneidoInThisJVM(args); // Fall back
				}
			} else if (validateDebugOption(debugOption)) {
				runSuneidoInThisJVM(args);
			} else {
				exitWithError();
			}
		}
	}

	/**
	 * <p>
	 * Synchronously runs Suneido as a child process in its own JVM and returns
	 * the process exit code.
	 * </p>
	 * TODO !! FINISH DOCUMENTING
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! TODO !! NEED
	 * TO TAKE FULL DEBUGGING OPTIONS *OUT* OF SUB-JVM... !!!! TODO !! VERIFY
	 * THAT ExePath BUILTIN WORKS ...
	 * 
	 * @param args
	 * @param isFullDebugging
	 * @return
	 */
	public static int runSuneidoInNewJVM(String[] args,
			boolean isFullDebugging, Filter<String> stdoutFilter,
			Filter<String> stderrFilter) {
		final ProcessBuilder builder = runSuneidoInNewJVMBuilder(args,
				isFullDebugging);
		final ArrayList<Thread> echoThreads = new ArrayList<Thread>(2);
		if (null == stdoutFilter) {
			builder.redirectInput(Redirect.INHERIT);
		}
		if (null == stderrFilter) {
			builder.redirectError(Redirect.INHERIT);
		}
		try {
			Process process = builder.start();
			startEchoThread(echoThreads, process.getInputStream(),
					stdoutFilter, System.out); // Only if not inheriting stdout
			startEchoThread(echoThreads, process.getErrorStream(),
					stderrFilter, System.err); // Only if not inheriting stderr
			for (final Thread thread : echoThreads) {
				thread.join();
			}
			return process.waitFor();
		} catch (IOException e) {
			throw new SuInternalError("failed to run \""
					+ Joiner.on(' ').join(builder.command(), e));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SuInternalError("interrupted while running \""
					+ Joiner.on(' ').join(builder.command(), e));
		}
	}

	//
	// INTERNALS
	//

	private static final String SKIP_BOOT_PROPERTY_NAME = "suneido.boot.skip";

	private static void runSuneidoInThisJVM(String[] args) {
		// Classloader will not load suneido.Suneido until we get here.
		Suneido.main(args);
	}

	private static void exitWithError() {
		System.exit(-1);
	}

	private static boolean validateDebugOption(String debugOption) {
		return DEBUG_OPTION_STACK.equals(debugOption)
				|| DEBUG_OPTION_NONE.equals(debugOption);
	}

	private static ArrayList<String> getJVMArguments() {
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		List<String> jvmArguments = runtimeMxBean.getInputArguments(); // non-modifiable
		// We need to convert from a non-modifiable list to a modifiable list
		ArrayList<String> result = new ArrayList<String>(jvmArguments.size());
		// While converting into non-modifiable list, strip out all the agent
		// arguments if they are present. This is required *among other reasons*
		// because if our (the current) JVM process was started with the JDWP
		// agent and it bound t a given IP port, the new JVM we are going to
		// start will error out if it attempts to bind to the same port. Another
		// reason if that if you are trying to debug the parent process in, say,
		// Eclipse, you don't want to deal with the child process erroring out
		// due to a duplicate port. Another reason is there's no point loading
		// agents unless needed.
		for (final String jvmArgument : jvmArguments) {
			if (!jvmArgument.startsWith("-agent")) {
				result.add(jvmArgument);
			}
		}
		return result;
	}

	private static String makeJavaCmd() {
		String javaHome = System.getProperty("java.home");
		String javaBin = javaHome + File.separator + "bin" + File.separator
				+ "java";
		return javaBin;
	}

	private static String[] makeJavaArgs(String javaCmd,
			List<String> jvmArguments, String[] args) {
		int n = 1 + jvmArguments.size() + 3 + args.length;
		final String[] result = new String[n];
		result[0] = javaCmd;
		int k = 1;
		for (final String jvmArgument : jvmArguments) {
			result[k++] = jvmArgument;
		}
		final String classPath = System.getProperty("java.class.path");
		result[k++] = "-cp";
		result[k++] = classPath;
		// Skip bootstrap altogether and just load the main Suneido class
		result[k++] = Suneido.class.getName();
		// Add command-line arguments to the Suneido program
		for (final String suneidoArgument : args) {
			result[k++] = suneidoArgument;
		}
		return result;
	}

	private static ProcessBuilder runSuneidoInNewJVMBuilder(String[] args,
			boolean isFullDebugging) {
		final ArrayList<String> jvmArgs = getJVMArguments();
		// Start the JVM with the appropriate agent if full debugging support is
		// required.
		if (isFullDebugging) {
			final File jvmtiAgentPath = Debug.getJVMTIAgentPath();
			String jvmAgentArg = null;
			if (null == jvmtiAgentPath) {
				jvmAgentArg = Debug.getJDWPAgentArg();
			} else {
				jvmAgentArg = "-agentpath:" + jvmtiAgentPath.getAbsolutePath();
			}
			jvmArgs.add(jvmAgentArg);
		}
		// Create the full process builder argument list.
		String[] allArgs = makeJavaArgs(makeJavaCmd(), jvmArgs, args);
		// Return the process builder.
		return new ProcessBuilder(allArgs);
	}

	private static void startEchoThread(ArrayList<Thread> echoThreads,
			InputStream in, Filter<String> filter, PrintStream out) {
		if (null != filter) {
			final Thread t = new Thread(new FilteredStreamEchoer(in, filter,
					out));
			echoThreads.add(t);
			t.start();
		}
	}

	private static boolean isFilterableOutput(String message) {
		return message
				.startsWith("Listening for transport dt_socket at address:");
	}

	private static boolean isFatalErrorPreventingNewJVMStartup(String message) {
		if (message.startsWith("FATAL")) {
			if (message.startsWith("FATAL ERROR in native method: JDWP"))
				return true;
		}
		return false;
	}

	//
	// Constants
	//

	/**
	 * As the first character of a command-line argument, indicates that the
	 * argument is intended to be an option.
	 */
	public static final char OPTION_INDICATOR = '-';
	/**
	 * On the command line, marks the end of options.
	 */
	public static final String OPTIONS_END = "--";
	/**
	 * On the command line, indicates that the next argument is one of
	 * {@link #DEBUG_OPTION_LOCALS}, {@link #DEBUG_OPTION_STACK},
	 * {@link #DEBUG_OPTION_NONE}.
	 */
	public static final String DEBUG_OPTION = "-debug";
	/**
	 * <p>
	 * On the command line, indicates that full debugging support should be
	 * turned on.
	 * </p>
	 *
	 * <p>
	 * Full debugging support has the following ramifications:
	 * <ul>
	 * <li>
	 * the Suneido {@code Locals()} built-in provides accurate information on
	 * local variables;</li>
	 * <li>
	 * exception stack-traces are "human readable" (or perhaps it would be
	 * better to say readable by a Suneido programmer): internal Java stack
	 * frames are eliminated and Suneido stack frames are given their Suneido
	 * names, not their internal Java names<sup>&dagger;</sup>;</li>
	 * <li>
	 * the bootstrapper must start another JVM to run {@link suneido.Suneido}
	 * because it needs to load the appropriate debugging support JVMTI agent
	 * (either the platform-appropriate native {@code jsdebug} library or, if no
	 * such library available on the platform, the {@code jdwp} agent);</li>
	 * <li>
	 * the Suneido runtime may run slower than it would without full debugging
	 * support (particularly if the {@code jdwp} agent needs to be used) because
	 * there is some overhead associated with JVMTI and, if the {@code jdwp}
	 * agent needs to be used, the JDI debug "client" thread(s) and transport
	 * over sockets; and</li>
	 * <li>
	 * if for some reason the bootstrapper can't start a fresh JVM with the
	 * appropriate JVMTI agent loaded, it will log an error and fall back to a
	 * simpler debug model.</li>
	 * </ul>
	 * <sup>&dagger;</sup>: <em>This evidently involves a small amount of
	 * information loss the sense that suppressed internal Java stack frames
	 * may produce some useful information about the state of the program</em>.
	 * </p>
	 * 
	 * @see #DEBUG_OPTION
	 * @see #DEBUG_OPTION_STACK
	 * @see #DEBUG_OPTION_NONE
	 */
	public static final String DEBUG_OPTION_ALL = "all";
	/**
	 * <p>
	 * On the command line, indicates that the only debugging support should be
	 * conversion of the stack trace to "human readable" format (in the sense
	 * described in the documentation for {@link #DEBUG_OPTION_ALL}).
	 * </p>
	 *
	 * <p>
	 * This is an intermediate debug option between {@link #DEBUG_OPTION_ALL}
	 * and {@link #DEBUG_OPTION_NONE}. It has negligible performance impact and
	 * may be useful for providing some information to Suneido programmers.
	 * However, it does involve net information loss on a systems level when
	 * compared to {@link #DEBUG_OPTION_NONE} because internal Java stack frames
	 * are hidden from the call stack that is available in Suneido.
	 * </p>
	 *
	 * <p>
	 * Under this option, the Suneido {@code Locals()} built-in and all related
	 * ways of determing local variables will return {@code #()}.
	 * </p>
	 *
	 * @see #DEBUG_OPTION
	 */
	public static final String DEBUG_OPTION_STACK = "stack";
	/**
	 * <p>
	 * On the command-line, indicates that no extra debugging support should be
	 * enabled.
	 * </p>
	 *
	 * <p>
	 * Under this option, the Suneido {@code Locals()} built-in and all related
	 * ways of determing local variables will return {@code #()}. The stack
	 * trace presented to the Suneido programmer will be the full Java stack
	 * trace without any modification.
	 * </p>
	 */
	public static final String DEBUG_OPTION_NONE = "none";
}
