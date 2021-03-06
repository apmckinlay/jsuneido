/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

public final class SuClosure3 extends SuClosure {

	public SuClosure3(Object block, Object self, Object[] locals) {
		super(block, self, locals);
	}

	@Override
	public Object call3(Object a, Object b, Object c) {
		int i = bspec.iparams;
		locals[i] = a;
		locals[++i] = b;
		locals[++i] = c;
		return wrapped.eval(self, locals);
	}

	@Override
	public Object eval3(Object newSelf, Object a, Object b, Object c) {
		int i = bspec.iparams;
		locals[i] = a;
		locals[++i] = b;
		locals[++i] = c;
		return wrapped.eval(newSelf, locals);
	}

}
