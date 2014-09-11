/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;


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
	public boolean[] isCall; // false -> eval, true -> call (no self)
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
				&& null != isCall && null != lineNumbers;
	}

	//
	// INTERNALS
	//

	private void fetchInfo() {
		// Placeholder for breakpoint
	}

	@SuppressWarnings("unused")
	private void alloc(int numFrames) {
		// Called from jsdebug agent or JDWP client. The reason for using a
		// method to do this rather than just having the agent/client set the
		// fields directly is that when using JDI to ask the JDWP agent to
		// allocate a new array, there's no clean way to prevent the new array
		// from being garbage collected before you can assign it to a field of
		// the StackInfo instance to make it reachable. If instead we tell JDWP
		// to call a method on a reachable StackInfo instance, we can make sure
		// the new arrays are always reachable.
		this.localsNames = new String[numFrames][];
		this.localsValues = new Object[numFrames][];
		this.isCall = new boolean[numFrames];
		this.lineNumbers = new int[numFrames];
	}

	@SuppressWarnings("unused")
	private void allocFrame(int frameIndex, int numLocals) {
		// See note in alloc(int) above.
		this.localsNames[frameIndex] = new String[numLocals];
		this.localsValues[frameIndex] = new Object[numLocals];
	}
}
