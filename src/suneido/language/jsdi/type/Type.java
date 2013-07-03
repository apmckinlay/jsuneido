package suneido.language.jsdi.type;

import suneido.SuValue;
import suneido.language.jsdi.StorageType;

public abstract class Type extends SuValue {

	//
	// DATA
	//

	private final TypeId typeId;
	private final StorageType storageType;
	private long jsdiHandle; // Handle to JSDI C++ object, may be 0

	//
	// CONSTRUCTORS
	//

	protected Type(TypeId typeId, StorageType storageType, long jsdiHandle) {
		if (null == typeId)
			throw new IllegalArgumentException("typeId may not be null");
		if (null == storageType)
			throw new IllegalArgumentException("storageType may not be null");
		this.typeId = typeId;
		this.storageType = storageType;
		this.jsdiHandle = jsdiHandle;
	}

	protected Type(TypeId typeId, StorageType storageType) {
		this(typeId, storageType, 0);
	}

	//
	// INTERNALS
	//

	protected void releaseHandle() {
		assert 0 != jsdiHandle : "No handle to release";
		TypeFactory.releaseHandle(jsdiHandle);
		jsdiHandle = 0;
	}

	//
	// ACCESSORS
	//

	public final TypeId getTypeId() {
		return typeId;
	}

	public final StorageType getStorageType() {
		return storageType;
	}

	/**
	 * Return a human-readable name for the type. This is the value which will
	 * be printed for a user of the built-in Suneido function {@code Display()}.
	 * The {@link #toString()} method is deliberately not overridden because it
	 * can be useful in debugging to have more information.
	 * 
	 * @return Human readable display name.
	 * @author Victor Schappert
	 * @since 20130628
	 */
	public abstract String getDisplayName();

	/**
	 * Return, in bytes, of this type on the native platform.
	 * 
	 * @return Size of this type in bytes.
	 * @author Victor Schappert
	 * @since 20130701
	 */
	public int sizeOf() {
		assert jsdiHandle != 0 : "Can't call sizeOf without a JSDI handle";
		return sizeOf(jsdiHandle);
	}

	//
	// INTERNALS
	//

	private static native String toStringNative(long jsdiHandle);

	private static native int sizeOf(long jsdiHandle);

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName()).append('[')
				.append(Long.toHexString(jsdiHandle));
		if (jsdiHandle != 0) {
			result.append(" [").append(toStringNative(jsdiHandle)).append(']');
		}
		result.append(']');
		return result.toString();
	}
}
