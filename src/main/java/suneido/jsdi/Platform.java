package suneido.jsdi;

/**
 * Enumerates platforms (operating systems and CPU architectures) that JSDI
 * supports.
 *
 * @author Victor Schappert
 * @since 20140717
 */
@DllInterface
public enum Platform {

	/**
	 * Enumerator indicating the Windows operating system running on the x86
	 * CPU architecture <em>or</em> a JVM running in 32-bit mode on 64-bit
	 * Windows on the AMD64 architecture.
	 */
	WIN32_X86(4),
	/**
	 * Enumerator indicating the Windows operating system running on the AMD64
	 * CPU architecture where the JVM is running in 64-bit mode.
	 */
	WIN32_AMD64(8),
	/**
	 * Enumerator indicating that JSDI is not supported on the platform.
	 */
	UNSUPPORTED_PLATFORM(-1);

	//
	// DATA/CONSTRUCTORS
	//

	private final int pointerSize;

	private Platform(int pointerSize)
	{
		this.pointerSize = pointerSize;
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the platform pointer size in bytes.
	 * @return Pointer size
	 */
	public int getPointerSize() { return pointerSize; }
	
	//
	// STATICS
	//

	/**
	 * Returns the platform enumerator relevant to the running JVM.
	 * @return Platform this JVM is running on
	 *
	 * The platform need not necessarily correspond to the underlying CPU
	 * architecture. For example, a JVM running within 64-bit Windows on the
	 * AMD64 architecture may still return {@link #WIN32_X86} if the JVM is
	 * running with a 32-bit data model.
	 */
	public static Platform getPlatform()
	{
		if (-1 < System.getProperty("os.name").indexOf("Windows"))
		{
			final String dataModel = System.getProperty("sun.arch.data.model");
			if ("64".equals(dataModel))
				return WIN32_AMD64;
			else if ("32".equals(dataModel))
				return WIN32_X86;
		}
		return UNSUPPORTED_PLATFORM;
	}
}
