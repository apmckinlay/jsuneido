/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.util.Util.array;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import suneido.SuException;

/**
 * Stores the parameters for function and methods.
 * Created by {@link ClassGen}.
 * Used by {@link Args}.
 * @see BlockSpec
 */
@Immutable
public class FunctionSpec {
	final String name;
	final boolean atParam;
	final String[] params;
	final String[] dynParams;
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
		this(null, params, noDefaults, false, null, params.length);
	}
	public FunctionSpec(String[] params, Object... defaults) {
		this(null, params, defaults, false, null, params.length);
	}
	public FunctionSpec(String name, String[] params, Object[] defaults,
			boolean atParam) {
		this(name, params, defaults, atParam, null, params.length);
	}
	public FunctionSpec(String name, String[] params, Object[] defaults,
			boolean atParam, String[] dynParams) {
		this(name, params, defaults, atParam, dynParams, params.length);
	}
	public FunctionSpec(String name, String[] params, Object[] defaults,
			boolean atParam, String[] dynParams, int nLocals) {
		this.name = name;
		this.params = params;
		this.dynParams = dynParams;
		this.defaults = defaults;
		this.atParam = atParam;
		this.nLocals = nLocals;
		assert !atParam || (params.length == 1 && defaults.length == 0);
	}
	
	private static final Splitter splitter = Splitter.on(',').trimResults();
	
	/**
	 * Create a FunctionSpec from the string description.
	 * Used for @Params annotations on built-in methods.
	 * Handles default values of:
	 * <li>? for NA
	 * <li>$ for Integer.MAX_VALUE
	 * <li>true or false
	 * <li>decimal integers
	 * <li>unquoted or single quoted strings</li>
	 * <p>WARNING: Doesn't handle commas in string defaults
	 */
	public static FunctionSpec from(String spec) {
		switch (spec) {
		case "":
			return noParams;
		case "string":
			return string;
		case "value":
			return value;
		case "value,value":
		case "value, value":
			return value2;
		case "block":
			return block;
		}
		Iterable<String> iter = splitter.split(spec);
		int np = 0;
		int nd = 0;
		for (String s : iter) {
			++np;
			if (s.contains("="))
				++nd;
		}
		String params[] = new String[np];
		Object defaults[] = new Object[nd];
		int ip = 0;
		int id = 0;
		for (String s : iter) {
			int e = s.indexOf('=');
			if (e == -1)
				params[ip++] = s;
			else {
				params[ip++] = s.substring(0, e).trim();
				defaults[id++] = valueOf(s.substring(e + 1).trim());
			}
		}
		return new FunctionSpec(params, defaults);
	}
	
	private static Object valueOf(String s) {
		if (s.equals("true") || s.equals("false"))
			return Boolean.valueOf(s);
		if (s.equals("NA"))
			return NA;
		if (s.equals("INTMAX"))
			return Integer.MAX_VALUE;
		try {
			return Integer.valueOf(s);
		} catch (NumberFormatException e) {
		}
		if (s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'')
			s = s.substring(1, s.length() - 1);
		return s;
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
			if (isDynParam(params[i]))
				sb.append("_");
			sb.append(params[i]);
			if (i >= params.length - defaults.length && i < params.length)
				sb.append("=").append(defaults[j++]);
		}
		sb.append(")");
		return sb.toString();
	}

	boolean isDynParam(String name) {
		if (dynParams != null)
			for (String dp : dynParams)
				if (dp.equals(name))
					return true;
		return false;
	}

	public Object defaultFor(int i) {
		assert i < params.length;
		if (i < params.length - defaults.length)
			throw new SuException("missing argument(s)");
		return defaults[i - (params.length - defaults.length)];
	}
	
	public int nParams() {
		return params.length;
	}

}
