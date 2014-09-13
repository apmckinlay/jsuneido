/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

public final class SuClosure4 extends SuClosure {

	public SuClosure4(Object block, Object self, Object[] locals) {
		super(block, self, locals);
	}

	@Override
	public Object call4(Object a, Object b, Object c, Object d) {
		int i = bspec.iparams;
		locals[i] = a;
		locals[++i] = b;
		locals[++i] = c;
		locals[++i] = d;
		return block.eval(self, locals);
	}

	@Override
	public Object eval4(Object newSelf, Object a, Object b, Object c, Object d) {
		int i = bspec.iparams;
		locals[i] = a;
		locals[++i] = b;
		locals[++i] = c;
		locals[++i] = d;
		return block.eval(newSelf, locals);
	}

}
