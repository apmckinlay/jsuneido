package suneido.language.jsdi.type;

import suneido.language.jsdi.Allocates;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDI;
import suneido.language.jsdi.StorageType;

@DllInterface
@Allocates
public final class BasicArray extends Type {

	//
	// DATA
	//

	//
	// CONSTRUCTORS
	//

	BasicArray(long jsdiHandle) {
		super(TypeId.BASIC, StorageType.ARRAY, jsdiHandle);
		assert jsdiHandle != 0 : "BasicArray may not have a null handle";
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		// TODO
		return "ARRAY -- todo";
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	protected void finalize() throws Throwable {
		try {
			releaseHandle();
		} finally {
			super.finalize();
		}
	}
}
