package suneido.language.jsdi.dll;

import java.util.Iterator;

import suneido.SuValue;
import suneido.language.SuCallable;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.StorageType;
import suneido.language.jsdi.type.Type;
import suneido.language.jsdi.type.TypeId;
import suneido.language.jsdi.type.TypeList;

@DllInterface
public class Dll extends SuCallable {

	//
	// DATA
	//

	long       funcPtr;
	TypeList   params;
	Type       returnType;

	String     suTypeName;
	DllFactory dllFactory;
	String     libraryName;
	String     userFunctionName;
	String     actualFunctionName;

	//
	// CONSTRUCTORS
	//

	Dll() {
		// DO NOTHING. The members will be assigned elsewhere.
	}

	//
	// MUTATORS
	//

	// deliberately package-internal
	// todo: docs
	void init(long funcPtr, TypeList params, Type returnType, String suTypeName,
			DllFactory dllFactory, String libraryName, String userFuncName,
			String funcName) {
		assert 0 != funcPtr : "Invalid dll function pointer";
		assert (TypeId.VOID == returnType.getTypeId() ||
			    TypeId.BASIC == returnType.getTypeId()) &&
			   StorageType.VALUE == returnType.getStorageType()
			 : "Invalid dll return type";
		this.funcPtr = funcPtr;
		this.params = params;
		this.returnType = returnType;
		this.suTypeName = suTypeName;
		this.dllFactory = dllFactory;
		this.libraryName = libraryName;
		this.userFunctionName = userFuncName;
		this.actualFunctionName = funcName;
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the Suneido type name.
	 * 
	 * <p>
	 * For example, for a global {@code dll} in a library record called 'X',
	 * the returned value is "X". However, the value returned is not necessarily
	 * a global name. For anonymous types, the Suneido type name is an
	 * arbitrary value assigned by the compiler.
	 * </p>
	 *
	 * <p>
	 * This method is called {@code getSuTypeName()} to differentiate it from
	 * {@link SuValue#typeName()}, which relates to the JSuneido universe rather
	 * than the user's type naming universe. 
	 * </p>
	 *
	 * @return Suneido type name
	 * @see suneido.language.jsdi.type.ComplexType#getSuTypeName()
	 */
	public final String getSuTypeName() {
		return suTypeName;
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(128);
		sb.append("dll ").append(returnType.getDisplayName()).append(' ')
				.append(libraryName).append(':').append(actualFunctionName)
				.append('(');
		final Iterator<TypeList.Entry> i = params.iterator();
		if (i.hasNext()) {
			TypeList.Entry entry = i.next();
			sb.append(entry.getType().getDisplayName()).append(' ')
					.append(entry.getName());
			while (i.hasNext()) {
				entry = i.next();
				sb.append(", ").append(entry.getType().getDisplayName())
						.append(' ').append(entry.getName());
			}
		}
		sb.append(')');
		sb.append(" /* ").append(Long.toHexString(funcPtr)).append(" */");
		return sb.toString();
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			dllFactory.freeDll(this);
		} finally {
			super.finalize();
		}
	}

	//
	// NATIVE METHODS. These functions are available to specific instances of
	//     DllBase which are derived from this class.
	//

	protected static native void callDirectOnlyReturnVoid(long jsdiHandle,
			int sizeDirect, byte[] params);

	protected static native void callDirectOnlyReturn32bit(long jsdiHandle,
			int sizeDirect, byte[] params);

	protected static native void callDirectOnlyReturn64bit(long jsdiHandle,
			int sizeDirect, byte[] params);

	protected static native void callGeneralReturnVoid(long jsdiHandle,
			int sizeDirect, int sizeIndirect, int[] ptrArray, byte[] params);

	protected static native int callGeneralReturn32bit(long jsdiHandle,
			int sizeDirect, int sizeIndirect, int[] ptrArray, byte[] params);

	protected static native long callGeneralReturn64bit(long jsdiHandle,
			int sizeDirect, int sizeIndirect, int[] ptrArray, byte[] params);
}
