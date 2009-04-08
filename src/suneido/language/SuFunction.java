package suneido.language;

import suneido.SuException;

/**
 * an SuFunction is implemented as a class with the definition in the
 * method-less invoke
 *
 * @author Andrew McKinlay
 */
abstract public class SuFunction extends SuClass {

	public SuFunction() {
		super(false); // don't need vars
	}

	@Override
	public Object invoke(String method, Object... args) {
		if (method == "Type")
			return "function";
		// TODO other standard methods on functions e.g. Params
		else
			throw unknown_method(method);
	}

	@Override
	public SuClass newInstance() {
		throw new SuException("cannot create instances of functions");
	}

}
