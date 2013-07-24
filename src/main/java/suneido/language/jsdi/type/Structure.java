package suneido.language.jsdi.type;

import java.util.Map;

import suneido.SuValue;
import suneido.language.BuiltinMethods;
import suneido.language.SuCallable;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.Marshaller;

/**
 * TODO: docs
 * <p>
 * This class is <em>not</em> immutable because {@link #getMarshallPlan()} may
 * change depending on the value of any members that are proxies.
 * </p>
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
public final class Structure extends ComplexType {

	//
	// CONSTRUCTORS
	//

	Structure(String suTypeName, TypeList members) {
		super(TypeId.STRUCT, suTypeName, members);
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public boolean isClosed() {
		return typeList.isClosed();
	}

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

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		typeList.marshallInMembers(marshaller, value);
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		return typeList.marshallOutMembers(marshaller, oldValue);
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
		Structure struct = (Structure)self;
		struct.resolve(0);
		return struct.getMarshallPlan().getSizeDirectIntrinsic();
	}
}
