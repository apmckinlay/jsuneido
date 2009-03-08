package suneido.language;

import suneido.*;

/**
 * an SuFunction is implemented as a class with the definition in the
 * method-less invoke
 *
 * @author Andrew McKinlay
 */
abstract public class SuFunction extends SuClass {

	SuFunction() {
		super(false); // don't need vars
	}

	@Override
	abstract public SuValue invoke(SuValue... args);

	@Override
	public SuValue invoke(String method, SuValue... args) {
		if (method == "Type")
			return SuString.valueOf("function");
		// TODO other standard methods on functions e.g. Params
		else {
			unknown_method(method);
			return null;
		}
	}

	@Override
	public SuClass newInstance(SuValue... args) {
		throw new SuException("cannot create instances of functions");
	}

}
