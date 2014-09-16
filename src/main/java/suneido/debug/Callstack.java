/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Encapsulates a Suneido stack trace, including any local variable data that is
 * available.
 *
 * @author Victor Schappert
 * @since 20140903
 */
@ThreadSafe
public abstract class Callstack implements Iterable<Frame> {

	//
	// DATA
	//

	private List<Frame> frames = null;
	protected final Throwable throwable;

	//
	// CONSTRUCTORS
	//

	protected Callstack(Throwable throwable) {
		this.throwable = throwable;
	}

	//
	// INTERNALS
	//

	protected abstract List<Frame> makeFrames(); // Called a maximum of one times

	//
	// CONSTANTS
	//

	/**
	 * An empty callstack for use by {@CallstackProvider} instances whose stack
	 * trace is always empty. At the moment this is used to implement the
	 * Suneido {@code "block:continue"} and {@code "block:break"} exceptions.
	 */
	public static final Callstack EMPTY = new Callstack(null) {
		@Override
		protected List<Frame> makeFrames() {
			return Collections.emptyList();
		}
	};

	//
	// ACCESSORS
	//

	/**
	 * Returns a read-only list of the stack frames belonging to this stack
	 * trace.
	 *
	 * @return List of stack frames
	 * @see #size()
	 */
	public synchronized List<Frame> frames() {
		if (null == frames) {
			frames = Collections.unmodifiableList(makeFrames());
		}
		return frames;
	}

	/**
	 * Returns the number of frames in the {@link #frames()} list.
	 *
	 * @return Number of frames in this stack trace
	 * @see #frames()
	 */
	public final int size() {
		return frames().size();
	}

	//
	// INTERFACE: Iterable
	//

	@Override
	public Iterator<Frame> iterator() {
		return frames().iterator();
	}
}
