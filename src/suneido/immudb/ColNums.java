/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.Arrays;

class ColNums {
	private final int[] cols;
	private final int hashCode;

	ColNums(int[] cols) {
		this.cols = cols;
		hashCode = Arrays.hashCode(cols);
	}

	@Override
	public boolean equals(Object that) {
		return (that instanceof ColNums)
				? Arrays.equals(cols, ((ColNums) that).cols)
				: false;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

}
