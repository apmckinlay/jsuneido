package suneido.language;


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
		String s = "FunctionSpec(";
		s += name + ", ";
		s += "locals:";
		if (atParam)
			s += " @";
		for (String t : locals)
			s += " " + t;
		s += ", nparams: " + nparams;
		s += ", constants:";
		for (Object x : constants)
			s += " " + (x == null ? "null" : Ops.display(x));
		s += ", ndefaults: " + ndefaults;
		return s + ")";
	}

}
