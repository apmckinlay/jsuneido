/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates a Suneido stack trace, including any local variable data that is
 * available.
 *
 * @author Victor Schappert
 * @since 20140903
 */
public abstract class Callstack implements Iterable<Frame> {

	//
	// DATA
	//

	private List<Frame> frames = null;

	//
	// INTERNALS
	//

	protected abstract List<Frame> makeFrames();

	//
	// CONSTANTS
	//

	/**
	 * An empty callstack for use by {@CallstackProvider} instances whose stack
	 * trace is always empty. At the moment this is used to implement the
	 * Suneido {@code "block:continue"} and {@code "block:break"} exceptions.
	 */
	public static final Callstack EMPTY = new Callstack() {
		@Override
		protected List<Frame> makeFrames() {
			return Collections.emptyList();
		}
	};

	//
	// ACCESSORS
	//

	public synchronized List<Frame> frames() {
		if (null == frames) {
			frames = Collections.unmodifiableList(makeFrames());
		}
		return frames;
	}

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
