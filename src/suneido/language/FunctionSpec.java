package suneido.language;

import static suneido.util.Util.array;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;

@ThreadSafe
public class FunctionSpec {
	final String name;
	/** parameter names followed by local variable names */
	final String[] locals;
	/** default parameter values followed by constants */
	final Object[] constants;
	final int nparams;
	final int ndefaults;
	final boolean atParam;
	final static Object[] noConstants = new Object[0];

	public final static FunctionSpec noParams =
			new FunctionSpec(null, new String[0], 0);
	public static final FunctionSpec value =
			new FunctionSpec("value");
	public static final FunctionSpec value2 =
			new FunctionSpec("value", "value");
	public static final FunctionSpec string =
			new FunctionSpec("string");
	public static final FunctionSpec block =
			new FunctionSpec(array("block"), Boolean.FALSE);
	public static final Object NA = new Object();

	public FunctionSpec(String... locals) {
		this(null, locals, locals.length, noConstants, 0, false);
	}
	public FunctionSpec(String[] locals, Object... constants) {
		this(null, locals, locals.length, constants, constants.length, false);
	}
	public FunctionSpec(String name, String[] locals, int nparams) {
		this(name, locals, nparams, noConstants, 0, false);
	}
	public FunctionSpec(String name, String[] locals, int nparams,
			Object[] constants, int ndefaults, boolean atParam) {
		this.name = name;
		this.locals = locals;
		this.nparams = nparams;
		this.constants = constants;
		this.ndefaults = ndefaults;
		this.atParam = atParam;
		assert 0 <= nparams && nparams <= locals.length;
		assert 0 <= ndefaults && ndefaults <= constants.length;
		assert !atParam || (nparams == 1 && ndefaults == 0);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("FunctionSpec(");
		sb.append(name).append(", ");
		sb.append("locals:");
		if (atParam)
			sb.append(" @");
		for (String t : locals)
			sb.append(" ").append(t);
		sb.append(", nparams: ").append(nparams);
		sb.append(", constants:");
		for (Object x : constants)
			sb.append(" ").append(x == null ? "null" : Ops.display(x));
		sb.append(", ndefaults: " + ndefaults);
		sb.append(")");
		return sb.toString();
	}

	public String params() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		if (atParam)
			sb.append("@");
		short j = 0;
		for (int i = 0; i < nparams; ++i) {
			if (i != 0)
				sb.append(",");
			sb.append(locals[i]);
			if (i >= nparams - ndefaults && i < nparams)
				sb.append("=").append(constants[j++]);
		}
		sb.append(")");
		return sb.toString();
	}
	public Object defaultFor(int i) {
		assert i < nparams;
		if (i < nparams - ndefaults)
			throw new SuException("missing argument(s)");
		return constants[i - (nparams - ndefaults)];
	}

}
