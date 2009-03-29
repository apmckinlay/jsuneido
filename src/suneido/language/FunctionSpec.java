package suneido.language;

import suneido.SuValue;

public class FunctionSpec {
	final String name;
	/** parameter names followed by local variable names */
	final String[] locals;
	/** default parameter values followed by constants */
	final SuValue[] constants;
	final int nparams;
	final int ndefaults;

	public FunctionSpec(String name, String[] locals, int nparams,
			SuValue[] constants, int ndefaults) {
		this.name = name;
		this.locals = locals;
		this.nparams = nparams;
		this.constants = constants;
		this.ndefaults = ndefaults;
		assert 0 <= nparams && nparams <= locals.length;
		assert 0 <= ndefaults && ndefaults <= constants.length;
	}

	@Override
	public String toString() {
		String s = "FunctionSpec(";
		s += "locals:";
		for (String t : locals)
			s += " " + t;
		s += ", nparams: " + nparams;
		s += ", constants:";
		for (SuValue x : constants)
			s += " " + x;
		s += ", ndefaults: " + ndefaults;
		return s + ")";
	}

}
