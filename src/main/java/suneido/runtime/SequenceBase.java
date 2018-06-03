/* Copyright 2015 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.nio.ByteBuffer;

import suneido.SuContainer;
import suneido.SuException;
import suneido.SuValue;

/**
 * Base class for Sequence (wrapping built-in iterators)
 * and SuSequence (wrapping user defined iterators).
 * Derived classes must define instantiate, copy, iter, and iterator
 * and must set duped in iter and iterator
 */
public abstract class SequenceBase extends SuContainer {
	private static final BuiltinMethods methods =
			new BuiltinMethods("sequence", Methods.class, "Sequences");
	protected boolean instantiated = false;
	protected boolean duped = false;

	abstract protected void instantiate(); // only SequenceBase should call this

	/** called when not instantiated */
	abstract protected SuValue copy();

	/** to get a Suneido compatible iterator with Next */
	abstract protected Object iter();

	abstract protected boolean infinite();

	@Override
	public SuValue lookup(String method) {
		SuValue x;

		if (override(method) && null != (x = methods.getMethod(method)))
			return x;
		else {
			ck_instantiate();
			return super.lookup(method);
		}
	}

	private boolean override(String method) {
		return method == "Instantiated?" || (! instantiated && ! duped);
	}

	// public so ProGuard won't strip it
	public static class Methods {
		public static Object Iter(Object self) {
			return ((SequenceBase) self).iter();
		}

		public static Object InstantiatedQ(Object self) {
			return ((SequenceBase) self).instantiated;
		}

		public static Object Copy(Object self) {
			// avoid two copies (instantiate & copy)
			// for common usage: for m in ob.Members().Copy()
			SequenceBase seq = (SequenceBase) self;
			if (seq.instantiated)
				return new SuContainer(seq);
			if (seq.infinite())
				throw new SuException("can't instantiate infinite sequence");
			return seq.copy();
		}

		@Params("string = ''")
		public static Object Join(Object self, Object a) {
			String sep = Ops.toStr(a);
			StringBuilder sb = new StringBuilder();
			for (Object x : (SequenceBase) self) {
				if (Ops.isString(x))
					sb.append(x.toString());
				else
					sb.append(Ops.display(x));
				sb.append(sep);
			}
			if (sb.length() > 0)
				sb.delete(sb.length() - sep.length(), sb.length());
			return sb.toString();
		}

	}

	protected SuContainer ck_instantiate() {
		if (instantiated)
			return this;
		if (infinite())
			throw new SuException("can't instantiate infinite sequence");
		instantiate();
		instantiated = true;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		ck_instantiate();
		return super.equals(other);
	}

	@Override
	public int hashCode() {
		ck_instantiate();
		return super.hashCode();
	}

	@Override
	public String toString() {
		ck_instantiate();
		return super.toString();
	}

	@Override
	public void pack(ByteBuffer buf) {
		ck_instantiate();
		super.pack(buf);
	}

	@Override
	public int packSize(int nest) {
		ck_instantiate();
		return super.packSize(nest);
	}

	@Override
	public Object get(Object key) {
		ck_instantiate();
		return super.get(key);
	}

	@Override
	public Object rangeTo(int i, int j) {
		ck_instantiate();
		return super.rangeTo(i, j);
	}

	@Override
	public Object rangeLen(int i, int n) {
		ck_instantiate();
		return super.rangeLen(i, n);
	}

	@Override
	public SuContainer toContainer() {
		ck_instantiate();
		return this;
	}

	@Override
	public String typeName() {
		return "Object";
	}

}
