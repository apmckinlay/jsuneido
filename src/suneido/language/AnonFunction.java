package suneido.language;

/** used to create anonymous function values
 * anonymous functions compile to methods in their containing class
 * Note: top level named library functions are different see {@link SuFunction}
 *
 * @author Andrew McKinlay
 */
public class AnonFunction extends SuMethod {

	public AnonFunction(String method) {
		super(null, method);
	}

	@Override
	public String typeName() {
		return "Function";
	}

	@Override
	public String toString() {
		return "aFunction";
	}

}
