/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.boot;

import java.io.File;
import java.io.IOException;

import suneido.SuInternalError;
import suneido.util.*;
import suneido.util.FileFinder.SearchResult;
import suneido.util.FileFinder.SearchStage;

/**
 * Facilities for detecting native shared libraries used by Suneido.
 *
 * @author Victor Schappert
 * @since 20140813
 */
public final class NativeLibrary {

	//
	// STATIC METHODS
	//

	/**
	 * <p>
	 * Uses the {@link FileFinder} search algorithm to find the native library
	 * having the given name. The search path property given by (
	 * {@value #PATH_PROPERTY_NAME}) is searched before any other location.
	 * </p>
	 *
	 * <p>
	 * <strong>NOTE</strong>: The return value is {@code null} (<em>ie</em> no
	 * exception is thrown) if the platform is not supported for the purposes of
	 * native libraries. If the platform is theoretically supported but the
	 * library was not found, the return value is a valid reference to a
	 * {@link SearchResult} for which the {@link SearchResult#success()
	 * success()} method returns {@code false}.
	 * </p>
	 *
	 * @param libraryName
	 *            File name of the library to search for <em>minus any platform-
	 *            specific filename extension&mdash;this is added
	 *            automatically</em>
	 * @return Either {@code null} if native libraries aren't supported on the
	 *         current {@link Platform}, or a non-{@code null} reference to a
	 *         {@link SearchResult} indicating the (possibly unsuccessful)
	 *         result of the search
	 * @throws IOException
	 *             If an IO error is encountered by the search algorithm
	 * @see #persistLibrary(String)
	 */
	public static SearchResult findLibrary(String libraryName)
			throws IOException {
		final String relPath = platformRelPath();
		if (null == relPath) {
			return null; // Indicates not a supported platform
		}
		final FileFinder finder = new FileFinder(true);
		finder.addSearchPathPropertyNames(PATH_PROPERTY_NAME);
		finder.addRelPaths(relPath);
		libraryName += Platform.getPlatform().libraryFilenameExtension;
		return finder.find(libraryName);
	}

	/**
	 * <p>
	 * Uses {@link #findLibrary(String)} to find the filesystem path to the
	 * given {@code libraryName}. If the library is found and is not in a
	 * temporary file, the path to the library is returned. If the library is
	 * found but is in a temporary file, this method attempts to copy the
	 * temporary file to a permanent location in the {@link Platform platform}-
	 * appropriate native library subdirectory under the JVM's current working
	 * directory. Finally, if the library is not found, or any error occurs in
	 * the copying process, the return value is {@code null}.
	 * </p>
	 *
	 * @param libraryName
	 *            Name of the platform-appropriate native library to find
	 * @return Path to a permanent library with the name {@code libraryName}, on
	 *         success, or {@code null} on failure
	 */
	public static File persistLibrary(String libraryName) {
		SearchResult result = null;
		try {
			result = findLibrary(libraryName);
		} catch (IOException e) {
			Errlog.error("error while searching for " + libraryName, e);
		}
		// Failed to find it for any reason? Return null.
		if (null == result) {
			return null;
		}
		// If it's not a temporary file, just return it.
		if (SearchStage.RELPATH_RELATIVE_TO_CLASSPATH != result.stage) {
			return result.file;
		}
		// If we get here, we have a temporary file. The search algorithm has
		// already marked it for deletion on JVM exit, but the caller wants a
		// path to a persistent version of the file. So we try to copy it into
		// the appropriate platform library directory under the current working
		// directory.
		File libraryDir = new File(platformRelPath());
		if (!libraryDir.exists()) {
			final String errMsg = "can't make platform library directory";
			try {
				libraryDir.mkdirs();
			} catch (SecurityException e) {
				Errlog.error(errMsg, e);
				return null;
			}
			if (!libraryDir.isDirectory()) {
				Errlog.error(errMsg + ": " + libraryDir);
				return null;
			}
		}
		// At this point, we have a valid directory we can try to copy into.
		libraryName += Platform.getPlatform().libraryFilenameExtension;
		File libraryFile = new File(libraryDir, libraryName);
		try {
			FileUtils.copy(result.file, libraryFile);
		} catch (IOException e) {
			Errlog.error(
					"can't persist " + libraryFile + " to " + libraryDir, e);
			return null;
		}
		return libraryFile;
	}

	//
	// CONSTANTS
	//

	/**
	 * Name of the Java system property indicating where to search first for
	 * native libraries.
	 */
	public static final String PATH_PROPERTY_NAME = "suneido.library.path";

	//
	// INTERNALS
	//

	private static final String LIB_RELPATH = "lib/";

	private static String platformRelPath() {
		switch (Platform.getPlatform()) {
		case UNKNOWN_PLATFORM:
			return null; // NOTICE: Return value is null if not supported
		case WIN32_AMD64:
			return LIB_RELPATH + "win32-amd64";
		case WIN32_X86:
			return LIB_RELPATH + "win32-x86";
		case LINUX_AMD64:
			return LIB_RELPATH + "linux-amd64";
		default:
			throw SuInternalError.unhandledEnum(Platform.getPlatform());
		}
	}
}
