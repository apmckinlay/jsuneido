package suneido.runtime;

/**
 * <p>
 * Specialization of {@link SuBuiltinMethod} for built-in methods which have
 * no arguments (except {@code this}).
 * </p>
 *
 * <p>
 * <strong>NOTE:</strong> Please do not derive from this class until you are
 * certain that {@link BuiltinMethods#methods(String, Class)} will not
 * adequately install the built-in methods you are adding.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140914
 */
abstract class SuBuiltinMethod0 extends SuBuiltinMethod {

	//
	// CONSTRUCTORS
	//

	SuBuiltinMethod0(String name) {
		super(name, FunctionSpec.NO_PARAMS);
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public Object eval(Object self, Object... args) {
		Args.massage(params, args);
		return eval0(self);
	}
}
