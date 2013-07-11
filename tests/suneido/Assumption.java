package suneido;

import org.junit.Assume;

/**
 * <p>
 * Contains methods which can be used to conditionally prevent JUnit tests from
 * running if the assumptions on which the tests' correctness depend are not
 * true.
 * </p>
 * <p>
 * See: <a href="">Conditionally ignoring tests in JUnit 4</a>
 * </p>
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
	 * @author Victor Schappert
	 * @since 20130711
	 */
	public static void jvmIs32BitOnWindows() {
		Assume.assumeTrue(
			"32".equals(System.getProperty("sun.arch.data.model")) &&
			-1 < System.getProperty("os.name").indexOf("Windows")
		);
	}
}
