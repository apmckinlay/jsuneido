/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static suneido.util.Util.array;

import javax.annotation.concurrent.Immutable;

import suneido.SuException;
import suneido.compiler.ClassGen;

import com.google.common.base.Splitter;

/**
 * <p>
 * Stores the parameters for callable entities: functions, methods, bare blocks,
 * and blocks that are closures.
 * </p>
 * 
 * <p>
 * Created by {@link ClassGen}.
 * </p>
 *
 * <p>
 * Used by:
 * <ul>
 * <li>
 * {@link Args} to convert arguments from the format used by the caller to the
 * format required by the callee's formal parameters;
 * </li>
 * <li>
 * {@link suneido.debug.CallstackAll} to decipher the local variables in a
 * call stack frame.
 * </li>
 * </ul>
 * </p> 
 *
 * @author Andrew McKinlay, Victor Schappert
 * @see ArgsArraySpec
 * @see BlockSpec
 */
@Immutable
public class FunctionSpec {

	//
	// DATA
	//

	final String name;
	final boolean atParam;
	final String[] paramNames;
	final String[] dynParams;
	final Object[] defaults;

	//
	// CONSTANTS
	//

	public static final String[] NO_VARS = new String[0];

	static final Object[] NO_DEFAULTS = new Object[0];

	public static final FunctionSpec NO_PARAMS =
			new FunctionSpec(new String[0]);
	public static final FunctionSpec VALUE =
			new FunctionSpec("value");
	public static final FunctionSpec VALUE2 =
			new FunctionSpec("value1", "value2");
	public static final FunctionSpec STRING =
			new FunctionSpec("string");
	public static final FunctionSpec NUMBER =
			new FunctionSpec("number");
	public static final FunctionSpec NUMBER2 =
			new FunctionSpec("number1, number2");
	public static final FunctionSpec BLOCK =
			new FunctionSpec(array("block"), Boolean.FALSE);
	public static final Object NA = new Object();

	//
	// CONSTRUCTORS
	// 

	public FunctionSpec(String... params) {
		this(null, params, NO_DEFAULTS, false, null, params.length);
	}
	public FunctionSpec(String[] params, Object... defaults) {
		this(null, params, defaults, false, null, params.length);
	}
	public FunctionSpec(String name, String[] params, Object[] defaults,
			boolean atParam, String[] dynParams) {
		this(name, params, defaults, atParam, dynParams, params.length);
	}
	public FunctionSpec(String name, String[] params, Object[] defaults,
			boolean atParam, String[] dynParams, int nLocals) {
		this.name = name;
		this.paramNames = params;
		this.dynParams = dynParams;
		this.defaults = defaults;
		this.atParam = atParam;
		assert !atParam || (params.length == 1 && defaults.length == 0);
	}

	//
	// INTERNALS
	//

	private static final Splitter splitter = Splitter.on(',').trimResults();

	private static Object valueOf(String s) {
		switch (s) {
		case "true":
		case "false":
			return Boolean.valueOf(s);
		case "NA":
			return NA;
		case "INTMAX":
			return Integer.MAX_VALUE;
		case "null":
			return null;
		}
		try {
			return Integer.valueOf(s);
		} catch (NumberFormatException e) {
		}
		if (s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'')
			s = s.substring(1, s.length() - 1);
		return s;
	}

	protected static Object[][]paramsNamesAndDefaults(String spec) {
		Iterable<String> iter = splitter.split(spec);
		int np = 0;
		int nd = 0;
		for (String s : iter) {
			++np;
			if (s.contains("="))
				++nd;
		}
		String paramNames[] = new String[np];
		Object defaults[] = new Object[nd];
		int ip = 0;
		int id = 0;
		for (String s : iter) {
			int e = s.indexOf('=');
			if (e == -1)
				paramNames[ip++] = s;
			else {
				paramNames[ip++] = s.substring(0, e).trim();
				defaults[id++] = valueOf(s.substring(e + 1).trim());
			}
		}
		return new Object[][] { paramNames, defaults };
	}

	//
	// ACCESSORS
	//

	public String params() {
		StringBuilder sb = new StringBuilder("(");
		if (atParam)
			sb.append("@");
		short j = 0;
		for (int i = 0; i < paramNames.length; ++i) {
			if (i != 0)
				sb.append(",");
			if (isDynParam(paramNames[i]))
				sb.append("_");
			sb.append(paramNames[i]);
			if (i >= paramNames.length - defaults.length && i < paramNames.length)
				sb.append("=").append(defaults[j++]);
		}
		return sb.append(")").toString();
	}

	boolean isDynParam(String name) {
		if (dynParams != null)
			for (String dp : dynParams)
				if (dp.equals(name))
					return true;
		return false;
	}

	public Object defaultFor(int i) {
		assert i < paramNames.length;
		if (i < paramNames.length - defaults.length)
			throw new SuException("missing argument(s)");
		return defaults[i - (paramNames.length - defaults.length)];
	}

	public int getParamCount() {
		return paramNames.length;
	}

	public String getParamName(int index) {
		return paramNames[index];
	}

	/** used by Args to ensure room in args array for locals */
	public int getAllLocalsCount() {
		return getParamCount();
	}

	//
	// STATIC METHODS
	//

	/**
	 * <p>
	 * Create a FunctionSpec from the string description.
	 * </p>
	 *
	 * <p>
	 * Used for {@link Params @Params} annotations on built-in methods.
	 * </p>
	 *
	 * <p>
	 * Handles default values of:
	 * <ul>
	 * <li>? for NA</li>
	 * <li>$ for Integer.MAX_VALUE</li>
	 * <li>true or false</li>
	 * <li>decimal integers</li>
	 * <li>unquoted or single quoted strings</li>
	 * </ul>
	 * </p>
	 * 
	 * <p><strong>WARNING</strong>: Doesn't handle commas in string defaults</p>
	 *
	 * @param spec String description of parameters
	 */
	public static FunctionSpec from(String spec) {
		switch (spec) {
		case "":
			return NO_PARAMS;
		case "value":
			return VALUE;
		case "value1,value2":
		case "value1, value2":
			return VALUE2;
		case "string":
			return STRING;
		case "number":
			return NUMBER;
		case "number1,number2":
		case "number1, number2":
			return NUMBER2;
		case "block":
			return BLOCK;
		}
		Object[][] namesAndDefs = paramsNamesAndDefaults(spec);
		return new FunctionSpec((String[])namesAndDefs[0], namesAndDefs[1]);
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("FunctionSpec(");
		if (name != null)
			sb.append(name).append(", ");
		sb.append("params:");
		if (atParam)
			sb.append(" @");
		for (String t : paramNames)
			sb.append(" ").append(t);
		sb.append(", defaults:");
		for (Object x : defaults)
			sb.append(" ").append(x == null ? "null" : Ops.display(x));
		sb.append(")");
		return sb.toString();
	}
}
