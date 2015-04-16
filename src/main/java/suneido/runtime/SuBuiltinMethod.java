/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

/**
 * <p>
 * Ancestor class for all built-in methods.
 * </p>
 *
 * <p>
 * <strong>NOTE:</strong> It will <em>rarely</em> be necessary to derive a
 * subclass from this class directly. Unless there is a reason why
 * {@link BuiltinMethods} will not work, please use
 * {@link BuiltinMethods#methods(String, Class)} instead of deriving a subclass
 * from this class.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140914
 */
public abstract class SuBuiltinMethod extends SuBuiltinBase {

	//
	// CONSTRUCTORS
	//

	public SuBuiltinMethod(String name, FunctionSpec params) {
		super(CallableType.BUILTIN_METHOD, name, params);
	}

	//
	// ANCESTOR CLASS: SuValue
	//

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
