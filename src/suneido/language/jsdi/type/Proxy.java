package suneido.language.jsdi.type;

import suneido.language.Context;
import suneido.language.jsdi.Allocates;
import suneido.language.jsdi.StorageType;

@Allocates
public final class Proxy extends Type {

	//
	// DATA
	//

	/**
	 * <p>
	 * Naming context for looking up the Suneido language object which this
	 * proxy refers to (hopefully it is a {@link Structure} or {@link Callback},
	 * or else the user will get a runtime error).
	 * </p>
	 * <p>
	 * Conceptually, the context variable <em>could</em> be carried around
	 * either by this class or, in the alternative, by {@link TypeList}.
	 * However, because the vast majority of existing Suneido structs (as of
	 * 20130702) are relatively flat (<em>ie</em>} they don't contain any
	 * proxies at all), the system will be more lightweight in general if the
	 * context belongs to the proxy. 
	 * </p>
	 */
	private final Context context;
	private final int typeNameSlot;
	private final StorageType storageType;
	private final int numElems;
	private ComplexType lastResolvedType;

	//
	// CONSTRUCTORS
	//

	public Proxy(Context context, int typeNameSlot, StorageType storageType,
			int numElems) {
		super(TypeId.PROXY, storageType);
		this.context = context;
		this.typeNameSlot = typeNameSlot;
		this.storageType = storageType;
		this.numElems = numElems;
		this.lastResolvedType = null;
	}

	//
	// ACCESSORS
	//

	ComplexType resolve() throws ProxyResolveException {
		Object maybeType = context.get(typeNameSlot);
		if (maybeType == this.lastResolvedType) {
			this.lastResolvedType.resolve();
			return this.lastResolvedType;
		} else if (maybeType instanceof ComplexType) {
			this.lastResolvedType = (ComplexType) maybeType;
			this.lastResolvedType.resolve();
			return this.lastResolvedType;
		} else {
			throw new ProxyResolveException(this, maybeType.getClass());
		}
	}

	String getUnderlyingTypeName() {
		return context.nameForSlot(typeNameSlot);
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		final String typeName = getUnderlyingTypeName();
		switch (storageType) {
		case VALUE:
			return typeName;
		case POINTER:
			return typeName + '*';
		case ARRAY:
			StringBuilder sb = new StringBuilder(24);
			return sb.append(typeName).append('[').append(numElems).append(']')
					.toString();
		default:
			assert false : "Missing switch case in Proxy.getDisplayName()";
			return null;
		}
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	protected void finalize() throws Throwable {
		try {
			// TODO: IMPLEMENT THIS!!
			// XXX: This is big 'ol memory leak until implemented!
		} finally {
			super.finalize();
		}
	}
}
