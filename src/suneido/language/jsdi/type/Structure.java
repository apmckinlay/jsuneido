package suneido.language.jsdi.type;

import java.util.Map;

import suneido.SuValue;
import suneido.language.BuiltinMethods;
import suneido.language.SuCallable;

public final class Structure extends ComplexType {

	//
	// CONSTRUCTORS
	//

	public Structure(TypeList members) {
		super(TypeId.STRUCT, members);
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		StringBuilder sb = new StringBuilder(128);
		sb.append("struct { ");
		for (TypeList.Entry entry : typeList) {
			sb.append(entry.getType().getDisplayName());
			sb.append(' ');
			sb.append(entry.getName().toString());
			sb.append("; ");
		}
		sb.append('}');
		return sb.toString();
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	private static final Map<String, SuCallable> builtins = BuiltinMethods
			.methods(Structure.class);

	@Override
	public SuValue lookup(String method) {
		SuValue result = builtins.get(method);
		return null != result ? result : new SuValue.NotFound(method);
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		return getDisplayName(); // Can be result of Suneido 'Display' built-in.
	}

	//
	// BUILT-IN METHODS
	//

	/**
	 * Built-in size method. <em>eg</em>: {@code (struct { }).Size()}. The
	 * requirements for built-in methods are documented in
	 * {@link suneido.language.BuiltinMethods}.
	 * @param self The structure.
	 * @return Integer size of the structure in bytes.
	 * @see suneido.language.BuiltinMethods
	 */
	public static Object Size(Object self) {
		return 0;
	}
}
