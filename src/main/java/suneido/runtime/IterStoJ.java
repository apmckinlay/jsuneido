/* Copyright 2017 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import com.google.common.collect.AbstractIterator;

/** Adapt iterator from Suneido to Java style */
public class IterStoJ extends AbstractIterator<Object> {
	private final Object iter;

	public IterStoJ(Object iter) {
		this.iter = iter;
	}

	@Override
	protected Object computeNext() {
		Object x = Ops.invoke0(iter, "Next");
		return x != iter ? x : endOfData();
	}
}

