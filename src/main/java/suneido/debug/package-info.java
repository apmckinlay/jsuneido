/**
 * <p>
 * This package contains the classes required to support Suneido debugging
 * features, including the {@code Locals()} built-in and the
 * {@code exception.Callstack()} member function.
 * </p>
 *
 * <p>
 * The debug package performs the following tasks.
 * <ul>
 * <li>
 * Locate {@link suneido.boot.Platform platform}-appropriate native JVMTI
 * {@code jsdebug} library, if any, required to support full debugging.
 * </li>
 * <li>
 * Run JDI client thread(s) that communicate with JDWP server in cases where
 * full debugging support is run using the {@code jdwp} JVMTI agent rather than
 * a platform-appropriate {@code jsdebug} library.
 * </li>
 * <li>
 * Receive local variables from JVMTI agent via {@link suneido.debug.Locals}
 * class.
 * </li>
 * <li>
 * Convert stack traces from Java to Suneido.
 * </li>
 * </ul>
 * </p>
 *
 * @author Victor Schappert
 * @since 20140813
 */
package suneido.debug;