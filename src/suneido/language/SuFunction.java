package suneido.language;

/**
 * an SuFunction is implemented as a class with the definition in a "call"
 * method
 * 
 * @author Andrew McKinlay
 */
abstract public class SuFunction extends SuCallable {

	@Override
	public Object invoke(String method, Object... args) {
		if (method == "Type")
			return "function";
		// TODO other standard methods on functions e.g. Params
		else
			throw unknown_method(method);
	}

}
