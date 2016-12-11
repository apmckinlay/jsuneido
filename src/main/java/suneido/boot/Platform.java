/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.boot;

/**
 * Enumerates platforms (operating systems and CPU architectures) that Suneido
 * knows about.
 *
 * @author Victor Schappert
 * @since 20140717
 */
public enum Platform {

	/**
	 * Enumerator indicating the Windows operating system running on the x86 CPU
	 * architecture <em>or</em> a JVM running in 32-bit mode on 64-bit Windows
	 * on the AMD64 architecture.
	 */
	WIN32_X86(".dll"),
	/**
	 * Enumerator indicating the Windows operating system running on the AMD64
	 * CPU architecture where the JVM is running in 64-bit mode.
	 */
	WIN32_AMD64(".dll"),
	/**
	 * Enumerator indicating a Linux distribution running on the AMD64 CPU
	 * architecture.
	 */
	LINUX_AMD64(".so"),
	/**
	 * Enumerator indicating that Suneido does not know what platform it is
	 * running on.
	 */
	UNKNOWN_PLATFORM(null);

	//
	// DATA
	//

	final String libraryFilenameExtension; // package internal

	//
	// CONSTRUCTORS
	//

	private Platform(String libraryFilenameExtention) {
		this.libraryFilenameExtension = libraryFilenameExtention;
	}

	//
	// STATICS
	//

	private static final Platform currentPlatform = determinePlatform();

	private static Platform determinePlatform() {
		final String osName = System.getProperty("os.name");
		if (osName.startsWith("Windows")) {
			if (isCPU_amd64()) {
				return isDataModel_64bit() ? WIN32_AMD64 : WIN32_X86;
			} else if (isCPU_x86()) {
				return WIN32_X86;
			}
		} else if (osName.startsWith("Linux")) {
			if (isCPU_amd64()) {
				assert isDataModel_64bit();
				return LINUX_AMD64;
			}
		}
		return UNKNOWN_PLATFORM;
	}

	private static boolean isDataModel_64bit() {
		final String dataModel = System.getProperty("sun.arch.data.model",
				System.getProperty("com.ibm.vm.bitmode"));
		return "64".equals(dataModel);
	}

	private static boolean isCPU_x86() {
		final String osArch = System.getProperty("os.arch");
		return "i386".equals(osArch) || "x86".equals(osArch)
				|| osArch.startsWith("i686");
	}

	private static boolean isCPU_amd64() {
		final String osArch = System.getProperty("os.arch");
		return "x86_64".equals(osArch) || "ia64".equals(osArch)
				|| "amd64".equals(osArch);
	}

	/**
	 * <p>
	 * Returns the platform enumerator relevant to the running JVM.
	 * </p>
	 *
	 * <p>
	 * The platform need not necessarily correspond to the underlying CPU
	 * architecture. For example, a JVM running within 64-bit Windows on the
	 * AMD64 architecture may still return {@link #WIN32_X86} if the JVM is
	 * running with a 32-bit data model.
	 * </p>
	 *
	 * @return Platform this JVM is running on
	 */
	public static Platform getPlatform() {
		return currentPlatform;
	}

	//
	// TESTING
	//

//	public static void main(String[] args) {
//		System.out.println(getPlatform());
//	}

}
