/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

/**
 * Used to track self-reference when comparing containers
 */
public class PairStack {
	private final int MAX = 20;
	private final Object[] xs = new Object[MAX];
	private final Object[] ys = new Object[MAX];
	private int n = 0;

	public void push(Object x, Object y) {
		if (n >= MAX)
			throw new RuntimeException("object comparison nesting overflow");
		xs[n] = x;
		ys[n] = y;
		++n;
	}

	public boolean contains(Object x, Object y) {
		for (int i = 0; i < n; ++i)
			if ((xs[i] == x && ys[i] == y) || (xs[i] == y && ys[i] == x))
				return true;
		return false;
	}

	public void pop() {
		--n;
	}
}