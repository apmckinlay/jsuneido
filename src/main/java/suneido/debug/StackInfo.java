/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.lang.annotation.Annotation;

/**
 * <p>
 * Container for local variable information.
 * </p>
 *
 * <p>
 * If full debugging support is enabled, this class receives local variables for
 * relevant stack frames from the JVMTI agent, which is either a
 * {@link suneido.boot.Platform platform}-appropriate native {@code jsdebug}
 * library, or the {@code jdwp} agent communicating with a JDI client being run
 * side-by-side with Suneido in this JVM.
 * </p>
 *
 * <p>
 * If full debugging support is not enabled, this class never contains any
 * meaningful data. Any accessor function will return the equivalent of an empty
 * collection.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140813
 */
final class StackInfo {

	//
	// DATA
	//

	public String[][] localsNames;
	public Object[][] localsValues;
	public Annotation[] annotations;
	public int[] lineNumbers;
	public boolean isInitialized;

	//
	// CONSTRUCTORS
	//

	public StackInfo() {
		isInitialized = false;
		fetchInfo();
	}

	//
	// ACCESSORS
	//

	public boolean isInitialized() {
		return isInitialized && null != localsNames && null != localsValues
				&& null != annotations && null != lineNumbers;
	}

	//
	// INTERNALS
	//

	private void fetchInfo() {
		// Placeholder for breakpoint
	}
}
