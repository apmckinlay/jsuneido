package suneido.runtime;

/**
 * <p>
 * Ancestor class of all built-in methods and functions.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140914
 * @see Builtin
 * @see BuiltinMethods
 * @see SuBuiltinMethod
 */
abstract class SuBuiltinBase extends SuCallable {

	SuBuiltinBase(CallableType callableType, String name, FunctionSpec params) {
		assert callableType.isBuiltin();
		this.callableType = callableType;
		this.name = name;
		this.params = params;
	}

	//
	// ANCESTOR CLASS: SuCallable
	//

	@Override
	public String sourceCode() {
		StringBuilder sb = new StringBuilder(256);
		return sb.append("/* ").append(callableType.displayString())
				.append(" */\n\"").append(name)
				.append(null != params ? params.params() : "(...)").append('"')
				.toString();
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public abstract Object call(Object... args);
}
