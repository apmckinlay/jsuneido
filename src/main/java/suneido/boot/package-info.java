/**
 * <p>
 * This package is intended to contain the minimum set of classes to start an
 * instance of the JVM that is running Suneido.
 * </p>
 *
 * <p>
 * The "technical" entry-point class is the bootstrap class, {@link Bootstrap}
 * whereas the "logical" entry-point class is {@link suneido.Suneido Suneido}.
 * In some cases, the bootstrap class may simply pass control over to
 * {@link suneido.Suneido#main(String[]) Suneido.main(String[])}. In other
 * cases, the bootstrap class may need to treat the current JVM as a "bootstrap"
 * JVM and start a second JVM to actually run the "logical" entry-point. This is
 * because some debug options require loading a low-level native agent library
 * at JVM startup using the JVM's {@code -agentpath} command-line option.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140813
 */
package suneido.boot;