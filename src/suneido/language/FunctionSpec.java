/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.util.Util.array;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;

@ThreadSafe
public class FunctionSpec {
	final String name;
	final boolean atParam;
	final String[] params;
	final Object[] defaults;
	/** used by Args to ensure room in args array for locals */
	final int nLocals;
	static final Object[] noDefaults = new Object[0];

	public static final FunctionSpec noParams =
			new FunctionSpec(new String[0]);
	public static final FunctionSpec value =
			new FunctionSpec("value");
	public static final FunctionSpec value2 =
			new FunctionSpec("value", "value");
	public static final FunctionSpec string =
			new FunctionSpec("string");
	public static final FunctionSpec block =
			new FunctionSpec(array("block"), Boolean.FALSE);
	public static final Object NA = new Object();

	public FunctionSpec(String... params) {
		this(null, params, noDefaults, false, params.length);
	}
	public FunctionSpec(String[] params, Object... defaults) {
		this(null, params, defaults, false, params.length);
	}
	public FunctionSpec(String name, String[] params, Object[] defaults,
			boolean atParam) {
		this(name, params, defaults, atParam, params.length);
	}
	public FunctionSpec(String name, String[] params, Object[] defaults,
			boolean atParam, int nLocals) {
		this.name = name;
		this.params = params;
		this.defaults = defaults;
		this.atParam = atParam;
		this.nLocals = nLocals;
		assert !atParam || (params.length == 1 && defaults.length == 0);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("FunctionSpec(");
		if (name != null)
			sb.append(name).append(", ");
		sb.append("params:");
		if (atParam)
			sb.append(" @");
		for (String t : params)
			sb.append(" ").append(t);
		sb.append(", defaults:");
		for (Object x : defaults)
			sb.append(" ").append(x == null ? "null" : Ops.display(x));
		sb.append(")");
		return sb.toString();
	}

	public String params() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		if (atParam)
			sb.append("@");
		short j = 0;
		for (int i = 0; i < params.length; ++i) {
			if (i != 0)
				sb.append(",");
			sb.append(params[i]);
			if (i >= params.length - defaults.length && i < params.length)
				sb.append("=").append(defaults[j++]);
		}
		sb.append(")");
		return sb.toString();
	}
	public Object defaultFor(int i) {
		assert i < params.length;
		if (i < params.length - defaults.length)
			throw new SuException("missing argument(s)");
		return defaults[i - (params.length - defaults.length)];
	}

}
