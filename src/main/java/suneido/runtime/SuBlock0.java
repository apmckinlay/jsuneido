/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

public class SuBlock0 extends SuBlock {

	public SuBlock0(Object block, Object self, Object[] locals) {
		super(block, self, locals);
	}

	@Override
	public Object call0() {
		return block.eval(self, locals);
	}

	@Override
	public Object eval0(Object newSelf) {
		return block.eval(newSelf, locals);
	}

}
