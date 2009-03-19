package suneido.language;

import suneido.SuValue;

public class FunctionSpec {
	/** parameter names followed by local variable names */
	final String[] locals;
	/** default parameter values followed by constants */
	final SuValue[] constants;
	final int nparams;
	final int ndefaults;

	public FunctionSpec(String[] locals, int nparams, SuValue[] constants,
			int ndefaults) {
		this.locals = locals;
		this.constants = constants;
		this.nparams = nparams;
		this.ndefaults = ndefaults;
		assert nparams <= locals.length;
		assert ndefaults <= constants.length;
	}

}
