/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.util.Arrays;
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

	private List<Frame> frames = null;

	//
	// INTERNALS
	//

	protected abstract Frame[] makeFrames();

	//
	// ACCESSORS
	//

	public synchronized List<Frame> frames() {
		if (null == frames) {
			frames = Collections.unmodifiableList(Arrays.asList(makeFrames()));
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
