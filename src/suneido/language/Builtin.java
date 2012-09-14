/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.lang.invoke.MethodHandle;

import suneido.SuException;

class Builtin {
	
	static SuCallable method(MethodHandle mh, FunctionSpec params) {
		if (params == null)
			return new MethodN(mh, null);
		switch (params.nParams()) {
		case 0:
			return new Method0(mh, params);
		case 1:
			return new Method1(mh, params);
		case 2:
			return new Method2(mh, params);
		case 3:
			return new Method3(mh, params);
		case 4:
			return new Method4(mh, params);
		default:
			return new MethodN(mh, params);
		}
	}

	private static abstract class Method extends SuCallable {
		protected final MethodHandle mh;
		
		Method(MethodHandle mh, FunctionSpec params) {
			this.mh = mh;
			this.params = params;
		}

		@Override
		public String typeName() {
			return "Method";
		}

		@Override
		public abstract Object eval(Object self, Object... args);
		
	}

	private static class MethodN extends Method {
		
		MethodN(MethodHandle mh, FunctionSpec params) {
			super(mh, params);
		}

		@Override
		public Object eval(Object self, Object... args) {
			if (params != null)
				args = Args.massage(params, args);
			try {
				return mh.invoke(self, args);
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static final class Method0 extends Method {

		Method0(MethodHandle mh, FunctionSpec params) {
			super(mh, params);
		}

		@Override
		public Object eval(Object self, Object... args) {
			Args.massage(params, args);
			return eval0(self);
		}

		@Override
		public Object eval0(Object self) {
			try {
				return mh.invoke(self);
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private static final class Method1 extends Method {

		Method1(MethodHandle mh, FunctionSpec params) {
			super(mh, params);
		}

		@Override
		public Object eval(Object self, Object... args) {
			args = Args.massage(params, args);
			return eval1(self, args[0]);
		}

		@Override
		public Object eval0(Object self) {
			return eval1(self, fillin(0));
		}

		@Override
		public Object eval1(Object self, Object a) {
			try {
				return mh.invoke(self, a);
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private static final class Method2 extends Method {

		Method2(MethodHandle mh, FunctionSpec params) {
			super(mh, params);
		}

		@Override
		public Object eval(Object self, Object... args) {
			args = Args.massage(params, args);
			return eval2(self, args[0], args[1]);
		}

		@Override
		public Object eval0(Object self) {
			return eval2(self, fillin(0), fillin(1));
		}

		@Override
		public Object eval1(Object self, Object a) {
			return eval2(self, a, fillin(1));
		}

		@Override
		public Object eval2(Object self, Object a, Object b) {
			try {
				return mh.invoke(self, a, b);
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private static final class Method3 extends Method {

		Method3(MethodHandle mh, FunctionSpec params) {
			super(mh, params);
		}

		@Override
		public Object eval(Object self, Object... args) {
			args = Args.massage(params, args);
			return eval3(self, args[0], args[1], args[2]);
		}

		@Override
		public Object eval0(Object self) {
			return eval3(self, fillin(0), fillin(1), fillin(2));
		}

		@Override
		public Object eval1(Object self, Object a) {
			return eval3(self, a, fillin(1), fillin(2));
		}

		@Override
		public Object eval2(Object self, Object a, Object b) {
			return eval3(self, a, b, fillin(2));
		}

		@Override
		public Object eval3(Object self, Object a, Object b, Object c) {
			try {
				return mh.invoke(self, a, b, c);
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static final class Method4 extends Method {

		Method4(MethodHandle mh, FunctionSpec params) {
			super(mh, params);
		}

		@Override
		public Object eval(Object self, Object... args) {
			args = Args.massage(params, args);
			return eval4(self, args[0], args[1], args[2], args[3]);
		}

		@Override
		public Object eval0(Object self) {
			return eval4(self, fillin(0), fillin(1), fillin(2), fillin(3));
		}

		@Override
		public Object eval1(Object self, Object a) {
			return eval4(self, a, fillin(1), fillin(2), fillin(3));
		}

		@Override
		public Object eval2(Object self, Object a, Object b) {
			return eval4(self, a, b, fillin(2), fillin(3));
		}

		@Override
		public Object eval3(Object self, Object a, Object b, Object c) {
			return eval4(self, a, b, c, fillin(3));
		}

		@Override
		public Object eval4(Object self, Object a, Object b, Object c, Object d) {
			try {
				return mh.invoke(self, a, b, c, d);
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}
}
