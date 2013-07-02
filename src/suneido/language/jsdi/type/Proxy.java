package suneido.language.jsdi.type;

import suneido.language.jsdi.Allocates;
import suneido.language.jsdi.StorageType;

@Allocates
public final class Proxy extends Type {

	//
	// DATA
	//

	private final String typeName;
	private final StorageType storageType;
	private final int numElems;

	//
	// CONSTRUCTORS
	//

	public Proxy(String typeName, StorageType storageType, int numElems) {
		super(TypeId.PROXY, storageType);
		this.typeName = typeName;
		this.storageType = storageType;
		this.numElems = numElems;
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		switch (storageType) {
		case VALUE:
			return typeName;
		case POINTER:
			return typeName + '*';
		case ARRAY:
			StringBuilder sb = new StringBuilder(24);
			return sb.append(typeName).append('[').append(numElems)
					.append(']').toString();
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
