package suneido.runtime;

/**
 * <p>
 * Ancestor class for all built-in functions.
 * </p>
 *
 * <p>
 * <strong>NOTE:</strong> It will <em>rarely</em> be necessary to derive a
 * subclass from this class directly. Unless there is a reason why 
 * {@link BuiltinMethods} will not work, please use
 * {@link BuiltinMethods#functions(Class)} instead of deriving a subclass from
 * this class.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140914
 */
public abstract class SuBuiltinFunction extends SuBuiltinBase {

	public SuBuiltinFunction(String name, FunctionSpec params) {
		super(CallableType.BUILTIN_FUNCTION, name, params);
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public abstract Object call(Object... args);
}
