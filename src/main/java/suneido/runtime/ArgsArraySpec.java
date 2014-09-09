package suneido.runtime;

import javax.annotation.concurrent.Immutable;

import suneido.SuInternalError;

/**
 * Function information for functions whose parameters and local variables are
 * stored in an array ({@code Object[]}) rather than in Java local variables.
 * 
 * @author Victor Schappert
 * @since 20140908
 */
@Immutable
public class ArgsArraySpec extends FunctionSpec {

	//
	// DATA
	//

	private final String[] localNames;

	//
	// CONSTRUCTORS
	//

	public ArgsArraySpec(String[] paramNames) {
		this(null, paramNames, NO_DEFAULTS, false, null, NO_VARS);
	}

	public ArgsArraySpec(String name, String[] paramNames, Object[] defaults,
			boolean atParam, String[] dynParams, String[] localNames) {
		super(name, paramNames, defaults, atParam, dynParams);
		this.localNames = localNames;
	}

	//
	// ACCESSORS
	//

	public Object getParamValueFromArgsArray(Object[] args, int index) {
		return args[index];
	}

	public int getLocalCount() {
		return localNames.length;
	}

	public String getLocalName(int index) {
		return localNames[index];
	}

	public Object getLocalValueFromArgsArray(Object[] args, int index) {
		return args[paramNames.length + index];
	}

	public int getUpvalueCount() {
		return 0;
	}

	public String getUpvalueName(int index) {
		throw new SuInternalError("bare ArgsArrayFunctionSpec has no upvalues");
	}

	public Object getUpvalueFromArgsArray(Object[] args, int index) {
		throw new SuInternalError("bare ArgsArrayFunctionSpec has no upvalues");
	}

	//
	// STATIC METHODS
	//

	public static ArgsArraySpec from(String spec) {
		Object[][] namesAndDefs = paramsNamesAndDefaults(spec);
		return new ArgsArraySpec(null, (String[]) namesAndDefs[0],
				namesAndDefs[1], false, null, NO_VARS);
	}

	//
	// ANCESTOR CLASS: FunctionSpec
	//

	@Override
	public int getAllLocalsCount() {
		return getParamCount() + localNames.length;
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		return getClass().getSimpleName() + " extends " + super.toString();
	}
}
