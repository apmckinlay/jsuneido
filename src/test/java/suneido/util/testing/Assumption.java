/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util.testing;

import org.junit.Assume;

import suneido.boot.Platform;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDI;

/**
 * <p>
 * Contains methods which can be used to conditionally prevent JUnit tests from
 * running if the assumptions on which the tests' correctness depend are not
 * true.
 * </p>
 * <p>
 * See: <a href="">Conditionally ignoring tests in JUnit 4</a>
 * </p>
 * 
 * @author Victor Schappert
 * @since 20130711
 */
public final class Assumption {

	/**
	 * <p>
	 * States the assumption that the executing JVM is a 32-bit executable
	 * running on Windows.
	 * </p>
	 * <p>
	 * The purpose of method is to prevent tests running which depend on loading
	 * a required 32-bit DLL (<em>eg</em> jsdi). References to this assumption
	 * <strong>should be removed</strong> as soon as a 64-bit DLL is available.
	 * </p>
	 * <p>
	 * See: <a href="http://stackoverflow.com/a/2062036/1911388">How can I tell
	 * if I'm running in 64-bit JVM or 32-bit JVM...?</a>
	 * </p>
	 * 
	 * @author Victor Schappert
	 * @since 20130711
	 * @see #jvmIs64BitOnWindows()
	 * @see #jvmIsOnWindows()
	 */
	public static void jvmIs32BitOnWindows() {
		Assume.assumeTrue(Platform.WIN32_X86 == Platform.getPlatform());
	}

	/**
	 * /**
	 * <p>
	 * States the assumption that the executing JVM is a 64-bit executable
	 * running on Windows.
	 * </p>
	 *
	 * @author Victor Schappert
	 * @since 20140803
	 * @see #jvmIs32BitOnWindows()
	 * @see #jvmIsOnWindows()
	 */
	public static void jvmIs64BitOnWindows() {
		Assume.assumeTrue(Platform.WIN32_AMD64 == Platform.getPlatform());
	}

	/**
	 * <p>
	 * States the assumption that the executing JVM is running on Windows.
	 * </p>
	 *
	 * @author Victor Schappert
	 * @since 20140730
	 * @see #jvmIs32BitOnWindows()
	 * @see #jvmIs64BitOnWindows()
	 */
	public static void jvmIsOnWindows() {
		switch (Platform.getPlatform()) {
		case WIN32_X86:
		case WIN32_AMD64:
			Assume.assumeTrue(true);
			break;
		default:
			Assume.assumeTrue(false);
		}
	}

	/**
	 * <p>
	 * States the assumption that the JSDI component of Suneido is available.
	 * </p>
	 *
	 * @author Victor Schappert
	 * @since 20140827
	 */
	@DllInterface
	public static void jsdiIsAvailable() {
		Assume.assumeTrue(JSDI.isInitialized());
	}
}
