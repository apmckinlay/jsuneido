/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import suneido.SuValue;

abstract public class SuCallable extends SuValue {
	protected FunctionSpec params;
	//TODO only needed for SuClass.linkMethods and containsBlock
	protected Object[] constants;
	/** used to do super calls by methods and blocks within methods
	 *  set by {@link SuClass}.linkMethods */
	protected SuClass myClass;
	/** used by blockReturnHandler */
	protected boolean isBlock = false;

	@Override
	public SuValue lookup(String method) {
		if (method == "Params")
			return Params;
		return super.lookup(method);
	}

	private static SuValue Params = new SuMethod0() {
		@Override
		public Object eval0(Object self) {
			return ((SuCallable) self).params.params();
		}
	};

	@Override
	public boolean isCallable() {
		return true;
	}

	public Object superInvoke(Object self, String member, Object... args) {
		return myClass.superInvoke(self, member, args);
	}

	@Override
	public String toString() {
		return super.typeName().replace(AstCompile.METHOD_SEPARATOR, '.');
	}

	protected Object defaultFor(int i) {
		assert params != null : "" + this + " has no params";
		return params.defaultFor(i);
	}

	/**
	 * If block return came from one of our blocks, then return the value,
	 * otherwise, re-throw.
	 */
	public Object blockReturnHandler(BlockReturnException e) {
		if (containsBlock(this, e.block))
			return e.returnValue;
		throw e;
	}

	//TODO will this work for nested blocks ? will they be in our constants?
	private boolean containsBlock(SuCallable callable, Object block) {
		for (Object x : callable.constants)
			if (x == block ||
				(isBlock(x) && containsBlock((SuCallable) x, block)))
				return true;
		return false;
	}

	public static boolean isBlock(Object x) {
		return x instanceof SuCallable && ((SuCallable) x).isBlock;
	}

}
