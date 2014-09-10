/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.lang.invoke.MethodHandle;

// MAYBE use MethodHandles.insertArguments to add defaults

class Builtin {

	public static final String PACKAGE_NAME = Builtin.class.getPackage()
			.getName();

	static SuCallable method(MethodHandle mh, String valueName, FunctionSpec params) {
		if (params == null)
			return new MethodN(mh, valueName, null);
		switch (params.getParamCount()) {
		case 0:
			return new Method0(mh, valueName, params);
		case 1:
			return new Method1(mh, valueName, params);
		case 2:
			return new Method2(mh, valueName, params);
		case 3:
			return new Method3(mh, valueName, params);
		case 4:
			return new Method4(mh, valueName, params);
		default:
			return new MethodN(mh, valueName, params);
		}
	}

	private static abstract class Method extends SuCallable {
		protected final MethodHandle mh;

		Method(MethodHandle mh, String valueName, FunctionSpec params) {
			this.mh = mh;
			setSource(null, valueName);
			this.params = params;
		}

		@Override
		public String typeName() {
			return "Method";
		}

		@Override
		public String display() {
			return name + " /* builtin method */";
		}

		@Override
		public abstract Object eval(Object self, Object... args);

		@Override
		public Object call(Object... args) {
			return eval(this, args);
		}
		@Override
		public Object call0() {
			return eval0(this);
		}
		@Override
		public Object call1(Object a) {
			return eval1(this, a);
		}
		@Override
		public Object call2(Object a, Object b) {
			return eval2(this, a, b);
		}
		@Override
		public Object call3(Object a, Object b, Object c) {
			return eval3(this, a, b, c);
		}
		@Override
		public Object call4(Object a, Object b, Object c, Object d) {
			return eval4(this, a, b, c, d);
		}

	}

	private static class MethodN extends Method {

		MethodN(MethodHandle mh, String valueName, FunctionSpec params) {
			super(mh, valueName, params);
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

		Method0(MethodHandle mh, String valueName, FunctionSpec params) {
			super(mh, valueName, params);
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

		Method1(MethodHandle mh, String valueName, FunctionSpec params) {
			super(mh, valueName, params);
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

		Method2(MethodHandle mh, String valueName, FunctionSpec params) {
			super(mh, valueName, params);
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

		Method3(MethodHandle mh, String valueName, FunctionSpec params) {
			super(mh, valueName, params);
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

		Method4(MethodHandle mh, String valueName, FunctionSpec params) {
			super(mh, valueName, params);
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

	// functions ---------------------------------------------------------------

	static SuCallable function(MethodHandle mh, String valueName, FunctionSpec params) {
		if (params == null)
			return new FunctionN(mh, valueName, null);
		switch (params.getParamCount()) {
		case 0:
			return new Function0(mh, valueName, params);
		case 1:
			return new Function1(mh, valueName, params);
		case 2:
			return new Function2(mh, valueName, params);
		case 3:
			return new Function3(mh, valueName, params);
		case 4:
			return new Function4(mh, valueName, params);
		default:
			return new FunctionN(mh, valueName, params);
		}
	}

	private static abstract class Function extends SuCallable {
		protected final MethodHandle mh;

		Function(MethodHandle mh, String valueName, FunctionSpec params) {
			this.mh = mh;
			this.params = params;
			setSource(null, valueName);
		}

		@Override
		public String typeName() {
			return "Builtin";
		}

		@Override
		public String display() {
			return name + " /* builtin function */";
		}

		@Override
		public abstract Object call(Object... args);

	}

	private static class FunctionN extends Function {

		FunctionN(MethodHandle mh, String valueName, FunctionSpec params) {
			super(mh, valueName, params);
		}

		@Override
		public Object call(Object... args) {
			if (params != null)
				args = Args.massage(params, args);
			try {
				return mh.invoke(args);
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static final class Function0 extends Function {

		Function0(MethodHandle mh, String valueName, FunctionSpec params) {
			super(mh, valueName, params);
		}

		@Override
		public Object call(Object... args) {
			Args.massage(params, args);
			return call0();
		}

		@Override
		public Object call0() {
			try {
				return mh.invoke();
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}
	private static final class Function1 extends Function {

		Function1(MethodHandle mh, String valueName, FunctionSpec params) {
			super(mh, valueName, params);
		}

		@Override
		public Object call(Object... args) {
			args = Args.massage(params, args);
			return call1(args[0]);
		}

		@Override
		public Object call0() {
			return call1(fillin(0));
		}

		@Override
		public Object call1(Object a) {
			try {
				return mh.invoke(a);
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static final class Function2 extends Function {

		Function2(MethodHandle mh, String valueName, FunctionSpec params) {
			super(mh, valueName, params);
		}

		@Override
		public Object call(Object... args) {
			args = Args.massage(params, args);
			return call2(args[0], args[1]);
		}

		@Override
		public Object call0() {
			return call2(fillin(0), fillin(1));
		}

		@Override
		public Object call1(Object a) {
			return call2(a, fillin(1));
		}

		@Override
		public Object call2(Object a, Object b) {
			try {
				return mh.invoke(a, b);
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static final class Function3 extends Function {

		Function3(MethodHandle mh, String valueName, FunctionSpec params) {
			super(mh, valueName, params);
		}

		@Override
		public Object call(Object... args) {
			args = Args.massage(params, args);
			return call3(args[0], args[1], args[2]);
		}

		@Override
		public Object call0() {
			return call3(fillin(0), fillin(1), fillin(2));
		}

		@Override
		public Object call1(Object a) {
			return call3(a, fillin(1), fillin(2));
		}

		@Override
		public Object call2(Object a, Object b) {
			return call3(a, b, fillin(2));
		}

		@Override
		public Object call3(Object a, Object b, Object c) {
			try {
				return mh.invoke(a, b, c);
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static final class Function4 extends Function {

		Function4(MethodHandle mh, String valueName, FunctionSpec params) {
			super(mh, valueName, params);
		}

		@Override
		public Object call(Object... args) {
			args = Args.massage(params, args);
			return call4(args[0], args[1], args[2], args[3]);
		}

		@Override
		public Object call0() {
			return call4(fillin(0), fillin(1), fillin(2), fillin(3));
		}

		@Override
		public Object call1(Object a) {
			return call4(a, fillin(1), fillin(2), fillin(3));
		}

		@Override
		public Object call2(Object a, Object b) {
			return call4(a, b, fillin(2), fillin(3));
		}

		@Override
		public Object call3(Object a, Object b, Object c) {
			return call4(a, b, c, fillin(3));
		}

		@Override
		public Object call4(Object a, Object b, Object c, Object d) {
			try {
				return mh.invoke(a, b, c, d);
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}

}
