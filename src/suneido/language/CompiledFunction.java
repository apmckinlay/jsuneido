package suneido.language;

import suneido.SuValue;

public class CompiledFunction {
	final String[] locals; // first nparams are the params
	final SuValue[] constants; // first ndefaults are the parameter default values
	final int nparams;
	final int ndefaults;

	CompiledFunction(String[] locals, SuValue[] constants, int nparams,
			int ndefaults) {
		this.locals = locals;
		this.constants = constants;
		this.nparams = nparams;
		this.ndefaults = ndefaults;
	}

}
