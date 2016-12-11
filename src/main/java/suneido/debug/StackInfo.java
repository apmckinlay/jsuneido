/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;


/**
 * <p>
 * Repository for stack trace information.
 * </p>
 *
 * <p>
 * If full debugging support is enabled, calling {@link #fetchInfo()} causes
 * this class to receive local variables, line numbers, and other state for
 * relevant stack frames from the JVMTI agent, which is either a
 * {@link suneido.boot.Platform platform}-appropriate native {@code jsdebug}
 * library, or the {@code jdwp} agent communicating with a JDI client being run
 * side-by-side with Suneido in this JVM.
 * </p>
 *
 * <p>
 * If full debugging support is not enabled, this class never contains any
 * meaningful data.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140813
 */
final class StackInfo {

	//
	// DATA
	//

	public final int id;
	public String[][] localsNames;
	public Object[][] localsValues;
	public boolean[] isCall; // false -> eval, true -> call (no self)
	public int[] lineNumbers;
	public boolean isInitialized;

	//
	// CONSTRUCTORS
	//

	StackInfo() {
		this(-1);
	}

	StackInfo(int id) {
		this.id = id;
		this.isInitialized = false;
	}

	//
	// ACCESSORS
	//

	public boolean isInitialized() {
		return isInitialized && null != localsNames && null != localsValues
				&& null != isCall && null != lineNumbers;
	}

	//
	// INTERNALS
	//

	public StackInfo fetchInfo() {
		// Placeholder for breakpoint
		return this;
	}
}
