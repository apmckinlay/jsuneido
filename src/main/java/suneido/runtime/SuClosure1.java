/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

public final class SuClosure1 extends SuClosure {

	public SuClosure1(Object block, Object self, Object[] locals) {
		super(block, self, locals);
	}

	@Override
	public Object call1(Object a) {
		locals[bspec.iparams] = a;
		return block.eval(self, locals);
	}

	@Override
	public Object eval1(Object newSelf, Object a) {
		locals[bspec.iparams] = a;
		return block.eval(newSelf, locals);
	}

}
