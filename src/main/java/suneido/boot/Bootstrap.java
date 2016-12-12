/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.boot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;

import com.google.common.base.Joiner;

import suneido.SuInternalError;
import suneido.Suneido;
import suneido.debug.DebugUtil;
import suneido.util.Errlog;
import suneido.util.LineBufferingByteConsumer;

/**
 * <p>
 * Initial entry-point to start Suneido runtime. Will pass control over to the
 * "logical" entry point, {@link suneido.Suneido Suneido}, either in the current
 * JVM or in a new JVM it starts.
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
		// If the bootstrap skip flag is set, proceed directly to Suneido
		if (null != System.getProperty(SKIP_BOOT_PROPERTY_NAME)) {
			runSuneidoInThisJVM(args);
			// Otherwise, run the bootstrap process...
		} else {
			String debugOption = DEBUG_OPTION_OFF;
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
			if (DEBUG_OPTION_ON.equals(debugOption)) {
				final StderrEchoer stderrEchoer = new StderrEchoer();
				try (LineBufferingByteConsumer stderrConsumer =
						new LineBufferingByteConsumer(stderrEchoer)) {
					// Start a new JVM, collecting the exit code. Echo the new
					// process's stdout to our stdout, but filter out phrases
					// relating to the bootstrapping process itself. Echo the
					// new process's stderr to our stderr, and try to determine
					// whether error messages printed to stderr indicate a fatal
					// for the new JVM startup that we can recover from by
					// falling back to running Suneido from this JVM.
					int exitCode = runSuneidoInNewJVM(args, true,
							(byte[] b, int n) -> { System.out.write(b, 0, n); },
							stderrConsumer);
					if (0 != exitCode) {
						if (!stderrEchoer.fatalErrors.isEmpty()) {
							for (String fatal : stderrEchoer.fatalErrors) {
								Errlog.error("bootstrapper can't start new JVM: "
										+ fatal);
							}
							runSuneidoInThisJVM(args); // Fall back
						} else {
							System.exit(exitCode);
						}
					}
				} catch (SuInternalError e) {
					Errlog.error("bootstrapper can't start new JVM", e);
					runSuneidoInThisJVM(args); // Fall back
				}
			} else {
				runSuneidoInThisJVM(args);
			}
		}
	}

	/**
	 * <p>
	 * Synchronously runs Suneido as a child process in its own JVM and returns
	 * the process exit code. This method therefore blocks until the child
	 * process completes.
	 * </p>
	 *
	 * <p>
	 * The child process standard I/O streams are handled in the following way:
	 * <dt>
	 * <code>stdin</code></dt>
	 * <dd>
	 * The child process {@link Redirect#INHERIT inherits} this process'
	 * standard input stream at an operating system level. In most conceivable
	 * situations, this means the child process will simply receive and process
	 * all the input on the parent process' <code>stdin</code>.</dd>
	 * <dt>
	 * <code>stdout</code></dt>
	 * <dd>
	 * The child process inherits this process' standard output stream iff
	 * {@code stdoutConsumer} is {@code null}. If {@code stdoutConsumer} is not
	 * {@code null}, a thread in this process reads the child process' standard
	 * output stream and passes the bytes to {@code stdoutConsumer}. This
	 * behaviour is useful if this JVM needs to examine the child process
	 * output.
	 * <dt>
	 * <code>stderr</code></dt>
	 * <dd>
	 * The child process' standard error stream is treated the same as
	 * {@code stdout} , except that the relevant consumer is
	 * {@code stderrConsumer}.
	 * </p>
	 *
	 * @param args
	 *            Command-line arguments for {@link Suneido#main(String[])
	 *            Suneido.main(...)} in the child JVM
	 * @param isFullDebugging
	 *            Whether full debugging is required in the child process (this
	 *            may result in the child process being started with an
	 *            appropriate JVMTI agent)
	 * @param stdoutConsumer
	 *            Should be {@code null} if the child process should inherit
	 *            this process' standard error stream, or a non-{@code null}
	 *            value if this process should simply read the child's
	 *            {@code stdout} stream and pass the bytes to
	 *            {@code stdoutConsumer}
	 * @param stderrConsumer
	 *            Should be {@code null} if the child process should inherit
	 *            this process' standard error stream, or a non-{@code null}
	 *            value if this process should simply read the child's
	 *            {@code stderr} stream and pass the bytes to
	 *            {@code stderrConsumer}
	 * @return Exit code of the child JVM
	 * @throws SuInternalError
	 *             If starting the child JVM or reading one of its streams
	 *             causes an input/output exception or if the current thread is
	 *             interrupted while waiting for the child process to start or
	 *             while waiting for a stream reading thread to finish
	 */
	public static int runSuneidoInNewJVM(String[] args,
			boolean isFullDebugging, ObjIntConsumer<byte[]> stdoutConsumer,
			ObjIntConsumer<byte[]> stderrConsumer) {
		String[] allArgs = runSuneidoInNewJVMArgs(args, isFullDebugging);
		ProcessBuilder builder = new ProcessBuilder(allArgs);
		builder.redirectInput(Redirect.INHERIT);
		if (null == stdoutConsumer) {
			builder.redirectOutput(Redirect.INHERIT);
		}
		if (null == stderrConsumer) {
			builder.redirectError(Redirect.INHERIT);
		}
		try {
			Process process = builder.start();
			ObjIntConsumer<byte[]> syncConsumer = null;
			InputStream syncConsumerStream = null;
			Thread asyncConsumer = null;
			if (null != stdoutConsumer) {
				syncConsumer = stdoutConsumer;
				syncConsumerStream = process.getInputStream();
				if (null != stderrConsumer) {
					asyncConsumer = asynchronouslyConsume(stderrConsumer,
							process.getErrorStream());
				}
			} else if (null != stderrConsumer) {
				syncConsumer = stderrConsumer;
				syncConsumerStream = process.getErrorStream();
			}
			if (null != syncConsumer) {
				synchronouslyConsume(syncConsumer, syncConsumerStream);
			}
			if (null != asyncConsumer) {
				asyncConsumer.join();
			}
			return process.waitFor();
		} catch (IOException e) {
			throw new SuInternalError("failed to run \""
					+ Joiner.on(' ').join(builder.command(), e) + '"');
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SuInternalError("interrupted while running \""
					+ Joiner.on(' ').join(builder.command(), e) + '"');
		}
	}

	//
	// INTERNALS
	//

	static final String SKIP_BOOT_PROPERTY_NAME = "suneido.boot.skip";

	private static void runSuneidoInThisJVM(String[] args) {
		// Classloader will not load suneido.Suneido until we get here.
		Suneido.main(args);
	}

	private static ArrayList<String> getJVMArguments() {
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		List<String> jvmArguments = runtimeMxBean.getInputArguments(); // non-modifiable
		// We need to convert from a non-modifiable list to a modifiable list
		ArrayList<String> result = new ArrayList<String>(jvmArguments.size());
		// While converting into non-modifiable list, strip out all the agent
		// arguments if they are present. This is required *among other reasons*
		// because if our (the current) JVM process was started with the JDWP
		// agent and it was bound to a given IP port, the new JVM we are going to
		// start will error out if it attempts to bind to the same port. Another
		// reason if that if you are trying to debug the parent process in, say,
		// Eclipse, you don't want to deal with the child process erroring out
		// due to a duplicate port. Another reason is there's no point loading
		// agents unless needed. Also strip out any attempt to set the JDWP
		// debug port property.
		// 20150706 apm - also strip out vfprintf, abort, and exit
		// which are added by the JNI JVM invocation api
		// which appears to be used by the Windows service launcher
		for (final String jvmArgument : jvmArguments) {
			if (!jvmArgument.startsWith("-agent")
					&& !jvmArgument.startsWith("-D"
							+ DebugUtil.JDWP_PORT_PROP_NAME)
					&& !jvmArgument.startsWith("-D"
							+ DebugUtil.JDWP_FORCE_PROP_NAME)
					&& !jvmArgument.equals("vfprintf")
					&& !jvmArgument.equals("abort")
					&& !jvmArgument.equals("exit")) {
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

	public static String[] runSuneidoInNewJVMArgs(String[] args, boolean isFullDebugging) {
		final ArrayList<String> jvmArgs = getJVMArguments();
		// Start the JVM with the appropriate agent if full debugging support is
		// required.
		if (isFullDebugging) {
			final File jvmtiAgentPath = DebugUtil.isJDWPForced() ? null
					: DebugUtil.getJVMTIAgentPath();
			if (null == jvmtiAgentPath) {
				String jdwpServerPort = DebugUtil.getFreeJDWPAgentServerPort();
				jvmArgs.add(DebugUtil.getJDWPAgentArg(jdwpServerPort));
				jvmArgs.add(DebugUtil.getJDWPPortPropertyArg(jdwpServerPort));
			} else {
				jvmArgs.add("-agentpath:" + jvmtiAgentPath.getAbsolutePath());
			}
		}
		// Create the full process builder argument list.
		String[] allArgs = makeJavaArgs(makeJavaCmd(), jvmArgs, args);
		return allArgs;
	}

	private static Thread asynchronouslyConsume(
			ObjIntConsumer<byte[]> consumer, InputStream inputStream) {
		Thread thread = new Thread(
				() -> {
					try {
						synchronouslyConsume(consumer, inputStream);
					} catch (IOException e) {
						Errlog.error(
								"caught IOException in asynchronous stream consumer thread",
								e);
					}
				});
		thread.start();
		return thread;
	}

	private static void synchronouslyConsume(ObjIntConsumer<byte[]> consumer,
			InputStream inputStream) throws IOException {
		final byte[] buffer = new byte[1024];
		while (true) {
			final int available = inputStream.available();
			if (available < 2) {
				int b = inputStream.read();
				if (b < 0) {
					consumer.accept(buffer, 0);
					return; // EOF
				} else {
					buffer[0] = (byte) b;
					consumer.accept(buffer, 1);
				}
			} else {
				final int read = inputStream.read(buffer, 0,
						Math.min(available, buffer.length));
				if (read < 0) {
					consumer.accept(buffer, 0);
					return; // EOF
				} else {
					assert 0 < read;
					consumer.accept(buffer, read);
				}
			}
		}
	}

	private static final class StderrEchoer implements Consumer<CharSequence> {
		public boolean printedAtLeastOneLine = false;
		public final ArrayList<String> fatalErrors = new ArrayList<String>();

		@Override
		public void accept(CharSequence message) {
			if (null != message) {
				final String strMessage = message.toString();
				if (printedAtLeastOneLine) {
					System.err.println();
				} else {
					printedAtLeastOneLine = true;
				}
				System.err.print(strMessage);
				System.err.flush();
				if (strMessage.startsWith("FATAL") ||
						strMessage.startsWith("JDWP exit error")) {
					fatalErrors.add(strMessage);
				}
			}
		}
	}

	//
	// CONSTANTS
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
	 * {@link #DEBUG_OPTION_ON} or {@link #DEBUG_OPTION_OFF}.
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
	 * information loss in the sense that suppressed internal Java stack frames
	 * may produce some useful information about the state of the program</em>.
	 * </p>
	 *
	 * @see #DEBUG_OPTION
	 * @see #DEBUG_OPTION_OFF
	 */
	public static final String DEBUG_OPTION_ON = "on";

	/**
	 * <p>
	 * On the command-line, indicates that no extra debugging support should be
	 * enabled.
	 * </p>
	 *
	 * <p>
	 * Under this option, the Suneido {@code Locals()} built-in and all related
	 * ways of determining local variables will return {@code #()}. The stack
	 * trace presented to the Suneido programmer will be the full Java stack
	 * trace without any modification.
	 * </p>
	 *
	 * @see #DEBUG_OPTION
	 * @see #DEBUG_OPTION_ON
	 */
	public static final String DEBUG_OPTION_OFF = "off";
}
