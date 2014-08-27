/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * <p>
 * Utility class to help find files.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140731
 */
public final class FileFinder {

	//
	// TYPES
	//

	/**
	 * Stage of the search currently being done.
	 */
	public static enum SearchStage {
		/**
		 * Search by iterating over each explicit search path property name,
		 * splitting it into paths, and iterating over each path. If the file is
		 * found using this method, the result of the search is the actual
		 * filesystem path to the file.
		 */
		EXPLICIT_SEARCH_PATH_PROPERTY,
		/**
		 * Search by iterating over each given relpath and searching in the
		 * directory given by appending the relpath to the process current
		 * working directory. If the file is found using this method, the result
		 * of the search is the actual filesystem path to the file.
		 */
		RELPATH_RELATIVE_TO_WORKING_DIR,
		/**
		 * Search by iterating over each given relpath and attempting to use
		 * this class' {@link ClassLoader} to load the file. If the file is
		 * found using this method, the result of the search is the filesystem
		 * path to a temporary file created with
		 * {@link java.io.File#createTempFile(String, String)
		 * File.createTempFile(...)} into which the actual file has been copied.
		 * The search algorithm marks the file for deletion on JVM exit before
		 * returning, so that caller must copy the file if it needs to persist
		 * beyond the current JVM invocation.
		 */
		RELPATH_RELATIVE_TO_CLASSPATH,
		/**
		 * Search by iterating over each path in the operating system library
		 * path. If the file is found using this search method, the result of
		 * the search is the actual filesystem path to the file.
		 */
		SYSTEM_LIBARY_PATH
	}

	/**
	 * Result of the search algorithm.
	 */
	public static final class SearchResult {
		/**
		 * <p>
		 * Abstract path to the file found. May be {@code null} if the search
		 * was unsuccessful.
		 * </p>
		 * <p>
		 * In particular, this field is non-{@code null} iff {@link #success()}.
		 * </p>
		 */
		public final File file;
		/**
		 * Search stage this result is from. This field is never {@code null}.
		 */
		public final SearchStage stage;
		/**
		 * <p>
		 * Informational field having the following meaning, depending on
		 * {@link #stage}:
		 * </p>
		 * <dl>
		 * <dt>
		 * {@link SearchStage#EXPLICIT_SEARCH_PATH_PROPERTY}</dt>
		 * <dd>
		 * Property name of the property whose search produced this result</dd>
		 * <dt>
		 * {@link SearchStage#RELPATH_RELATIVE_TO_WORKING_DIR}</dt>
		 * <dd>
		 * Relpath whose search produced this result</dd>
		 * <dt>
		 * {@link SearchStage#RELPATH_RELATIVE_TO_CLASSPATH}</dt>
		 * <dd>
		 * Relpath whose search produced this result</dd>
		 * <dt>
		 * {@link SearchStage#SYSTEM_LIBARY_PATH}</dt>
		 * <dd>
		 * System environment variable property name</dd>
		 * </dl>
		 */
		public final String searchInfo;
		private boolean stop; // hidden field: search predicate said to stop

		private SearchResult(File file, SearchStage stage, String searchInfo) {
			assert null != stage && null != searchInfo;
			this.file = file;
			this.stage = stage;
			this.searchInfo = searchInfo;
			this.stop = false;
		}

		private static SearchResult succeeded(File file, SearchStage stage,
				String searchInfo) {
			assert null != file;
			return new SearchResult(file, stage, searchInfo);
		}

		private static SearchResult failed(SearchStage lastStage,
				String searchInfo) {
			return new SearchResult(null, lastStage, searchInfo);
		}

		/**
		 * Indicates whether this is the result of a successful search.
		 *
		 * @return True iff {@link #file} represents a valid search result
		 */
		public boolean success() {
			return null != file;
		}

		@Override
		public String toString() {
			return '[' + (null == file ? "N/A" : '\'' + file.toString() + '\'')
					+ ", " + stage + ", '" + searchInfo + "']";
		}
	}

	//
	// DATA
	//

	private final ArrayList<String> searchPathPropertyNames;
	private final ArrayList<String> relPaths; // rel to process wd or classpath,
												// use '/' as sep
	private boolean searchSystemLibraryPath;

	//
	// CONSTANTS
	//

	private static final String SYSTEM_LIBRARY_PATH_PROPERTY_NAME = "java.library.path";

	//
	// CONSTRUCTORS
	//

	/**
	 * Constructs a file finder.
	 * 
	 * @param searchSystemLibraryPath
	 *            Whether the {@link SearchStage#SYSTEM_LIBARY_PATH} search
	 *            stage should be run
	 */
	public FileFinder(boolean searchSystemLibraryPath) {
		this.searchPathPropertyNames = new ArrayList<String>();
		this.relPaths = new ArrayList<String>();
		setSearchSystemLibraryPath(searchSystemLibraryPath);
	}

	//
	// MUTATORS
	//

	/**
	 * Adds zero or more non-NULL property name to the list of search path
	 * property names.
	 *
	 * @param propertyNames
	 *            List of non-NULL property names
	 * @see #findUsingSearchPathPropertyNames(String, Predicate<SearchResult>)
	 * @see #addRelPaths(String...)
	 * @see #setSearchSystemLibraryPath(boolean)
	 */
	public void addSearchPathPropertyNames(String... propertyNames) {
		addStrings(searchPathPropertyNames, propertyNames, "property name");
	}

	/**
	 * <p>
	 * Adds zero or more non-NULL relative paths to the list of relative paths
	 * to search.
	 * </p>
	 *
	 * <p>
	 * Relative paths may not contain backslash ({@code '\\'} characters. Use
	 * forward slash ({@code '/'}) to separate elements of a path, regardless of
	 * the value of {@link File#separatorChar} on the current operating system.
	 * </p>
	 *
	 * @param relPaths
	 *            List of non-NULL relpaths
	 * @see #findRelToCwd(String, Predicate<SearchResult>)
	 * @see #findRelToClasspath(String, Predicate<SearchResult>)
	 * @see #addSearchPathPropertyNames(String...)
	 */
	public void addRelPaths(String... relPaths) {
		for (final String relpath : relPaths) {
			validateRelpath(relpath);
		}
		addStrings(this.relPaths, relPaths, "relpath");
	}

	/**
	 * <p>
	 * Sets a flag that determines whether {@link #find(String)} will search the
	 * system library path.
	 * </p>
	 *
	 * <p>
	 * The value to set this flag to will depend on whether the file being
	 * searched for is a native dynamic library that might live in the system
	 * library path, or whether it is some other kind of file.
	 * </p>
	 *
	 * @param searchSystemLibraryPath
	 *            Whether the {@link #find(String)} method should search the
	 *            system library path
	 */
	public void setSearchSystemLibraryPath(boolean searchSystemLibraryPath) {
		this.searchSystemLibraryPath = searchSystemLibraryPath;
	}

	//
	// ACCESSORS
	//

	/**
	 * <p>
	 * Executes the full search algorithm, stopping as soon as the first
	 * instance of the named file is found.
	 * </p>
	 *
	 * <p>
	 * The full search algorithm consists of executing each search stage
	 * enumerated in {@link SearchStage} in the order they appear in the
	 * enumeration.
	 * </p>
	 * 
	 * <ul>
	 * <li>
	 * The properties examined during
	 * {@link SearchStage#EXPLICIT_SEARCH_PATH_PROPERTY} are those added to this
	 * finder with {@link #addSearchPathPropertyNames(String...)}.</li>
	 * <li>
	 * The relative paths examined during
	 * {@link SearchStage#RELPATH_RELATIVE_TO_WORKING_DIR} and
	 * {@link SearchStage#RELPATH_RELATIVE_TO_CLASSPATH} are those added to this
	 * finder with {@link #addRelPaths(String...)}.</li>
	 * <li>
	 * Whether or not {@link SearchStage#SYSTEM_LIBARY_PATH} is run is dependent
	 * on this class' system library search flag.</li>
	 * </ul>
	 * 
	 * @param name
	 *            File name to search for
	 * @return Result of the search
	 * @throws IOException
	 * @see {@link #find(String, Predicate<SearchResult>)}
	 */
	public SearchResult find(String name) throws IOException {
		return find(name, (SearchResult) -> {
			return false;
		});
	}

	/**
	 * <p>
	 * Executes the full search algorithm, stopping only when instructed to do
	 * so by a search result predicate or all applicable search stages have been
	 * exhausted without finding anything.
	 * </p>
	 *
	 * @param name
	 *            File name to search for
	 * @param predicate
	 *            Given a successful search result, returns {@code true} if
	 *            the search should continue, or {@code false} if it should stop
	 * @return First successful search result, if any were found, or a non-NULL
	 *         failed search result otherwise
	 * @throws IOException
	 *             If an I/O exception occurs while searching relative to the
	 *             classpath
	 * @see #find(String)
	 * @see #findUsingSearchPathPropertyNames(String, Predicate<SearchResult>)
	 * @see #findRelToCwd(String, Predicate<SearchResult>)
	 * @see #findRelToClasspath(String, Predicate<SearchResult>)
	 * @see #findInSystemLibraryPath(String, Predicate<SearchResult>)
	 */
	public SearchResult find(String name, Predicate<SearchResult> predicate)
			throws IOException {
		SearchResult firstSuccess = null;
		SearchResult lastResult = findUsingSearchPathPropertyNames(name,
				predicate);
		if (lastResult.success()) {
			if (null == firstSuccess) {
				firstSuccess = lastResult;
			}
			if (lastResult.stop) {
				return firstSuccess;
			}
		}
		lastResult = findRelToCwd(name, predicate);
		if (lastResult.success()) {
			if (null == firstSuccess) {
				firstSuccess = lastResult;
			}
			if (lastResult.stop) {
				return firstSuccess;
			}
		}
		lastResult = findRelToClasspath(name, predicate); // may throw
															// IOException
		if (lastResult.success()) {
			if (null == firstSuccess) {
				firstSuccess = lastResult;
			}
			if (lastResult.stop) {
				return firstSuccess;
			}
		}
		if (searchSystemLibraryPath) {
			lastResult = findInSystemLibraryPath(name, predicate);
		}
		assert null != lastResult;
		return null != firstSuccess ? firstSuccess : lastResult;
	}

	/**
	 * Searches in the search path property names only.
	 *
	 * @param name
	 *            File name to search for
	 * @param predicate
	 *            Given a successful search result, returns {@code true} if
	 *            the search should continue, or {@code false} if it should stop
	 * @return First successful search result, if any were found, or a non-NULL
	 *         failed search result otherwise
	 * @see #addSearchPathPropertyNames(String...)
	 * @see #find(String, Predicate<SearchResult>)
	 */
	public SearchResult findUsingSearchPathPropertyNames(String name,
			Predicate<SearchResult> predicate) {
		SearchResult firstSuccess = null;
		SearchResult lastFail = null;
		for (final String propName : searchPathPropertyNames) {
			final SearchResult result = findUsingSearchPathPropertyName(name,
					propName, SearchStage.EXPLICIT_SEARCH_PATH_PROPERTY,
					predicate);
			if (result.success()) {
				if (null == firstSuccess) {
					firstSuccess = result;
				}
				if (result.stop) {
					break;
				}
			} else {
				lastFail = result;
			}
		}
		return null != firstSuccess ? firstSuccess
				: null != lastFail ? lastFail : SearchResult.failed(
						SearchStage.EXPLICIT_SEARCH_PATH_PROPERTY, "");
	}

	/**
	 * Searches relative to the current working directory only.
	 *
	 * @param name
	 *            File name to search for
	 * @param predicate
	 *            Given a successful search result, returns {@code true} if
	 *            the search should continue, or {@code false} if it should stop
	 * @return First successful search result, if any were found, or a non-NULL
	 *         failed search result otherwise
	 * @see #addRelPaths(String...)
	 * @see #findRelToClasspath(String, Predicate<SearchResult>)
	 * @see #find(String, Predicate<SearchResult>)
	 */
	public SearchResult findRelToCwd(String name,
			Predicate<SearchResult> predicate) {
		SearchResult firstSuccess = null;
		String lastRelPathSearched = "";
		for (final String relPath : relPaths) {
			final File file = new File(relPath, name);
			if (file.exists()) {
				final SearchResult result = SearchResult.succeeded(file,
						SearchStage.RELPATH_RELATIVE_TO_WORKING_DIR, relPath);
				if (null == firstSuccess) {
					firstSuccess = result;
				}
				if (!predicate.test(result)) {
					firstSuccess.stop = true;
				}
			}
			lastRelPathSearched = relPath;
		}
		return null != firstSuccess ? firstSuccess : SearchResult.failed(
				SearchStage.RELPATH_RELATIVE_TO_WORKING_DIR,
				lastRelPathSearched);
	}

	/**
	 * Searches relative to the classpath only.
	 *
	 * @param name
	 *            File name to search for
	 * @param predicate
	 *            Given a successful search result, returns {@code true} if
	 *            the search should continue, or {@code false} if it should stop
	 * @return First successful search result, if any were found, or a non-NULL
	 *         failed search result otherwise
	 * @throws IOException
	 *             If reading from the classpath, or writing to the requisite
	 *             temporary file, triggers an I/O exception
	 * @see #addRelPaths(String...)
	 * @see #findRelToCwd(String, Predicate<SearchResult>)
	 * @see #find(String, Predicate<SearchResult>)
	 */
	public SearchResult findRelToClasspath(String name,
			Predicate<SearchResult> predicate) throws IOException {
		SearchResult firstSuccess = null;
		String lastRelPathSearched = "";
		for (final String relPath : relPaths) {
			final SearchResult result = findRelToClasspath(name, relPath,
					predicate);
			if (null != result) {
				assert result.success();
				if (null == firstSuccess) {
					firstSuccess = result;
				}
				if (result.stop) {
					break;
				}
			} else {
				lastRelPathSearched = relPath;
			}
		}
		return null != firstSuccess ? firstSuccess : SearchResult.failed(
				SearchStage.RELPATH_RELATIVE_TO_CLASSPATH, lastRelPathSearched);
	}

	/**
	 * Searches in the system library path only.
	 *
	 * @param name
	 *            File name to search for
	 * @param predicate
	 *            Given a successful search result, returns {@code true} if
	 *            the search should continue, or {@code false} if it should stop
	 * @return First successful search result, if any were found, or a non-NULL
	 *         failed search result otherwise
	 * @see #find(String, Predicate<SearchResult>)
	 * @see #setSearchSystemLibraryPath(boolean)
	 */
	public static SearchResult findInSystemLibraryPath(String name,
			Predicate<SearchResult> predicate) {
		return findUsingSearchPathPropertyName(name,
				SYSTEM_LIBRARY_PATH_PROPERTY_NAME,
				SearchStage.SYSTEM_LIBARY_PATH, predicate);
	}

	//
	// INTERNALS
	//

	private static void addStrings(ArrayList<String> list, String[] strs,
			String info) {
		for (final String str : strs) {
			if (null == str) {
				throw new IllegalArgumentException(info + " cannot be null");
			}
			list.add(str);
		}
	}

	private static SearchResult findUsingSearchPathPropertyName(String name,
			String propName, SearchStage stage,
			Predicate<SearchResult> predicate) {
		SearchResult firstSuccess = null;
		final String propValue = System.getProperty(propName);
		if (null != propValue) {
			for (final String path : propValue.split(Pattern
					.quote(File.pathSeparator))) {
				final File file = new File(path, name);
				if (file.exists()) {
					SearchResult thisResult = SearchResult.succeeded(file,
							stage, propName);
					if (null == firstSuccess) {
						firstSuccess = thisResult;
					}
					if (!predicate.test(thisResult)) {
						firstSuccess.stop = true;
						break;
					}
				}
			}
		}
		return null != firstSuccess ? firstSuccess : SearchResult.failed(stage,
				propName);
	}

	private SearchResult findRelToClasspath(String name, String relPath,
			Predicate<SearchResult> predicate) throws IOException {
		String absResourcePath = null;
		if ("".equals(relPath)) {
			absResourcePath = '/' + name;
		} else {
			absResourcePath = '/'
					+ (relPath.endsWith("/") ? relPath.replace("/+$", "")
							: relPath) + '/' + name;
		}
		File tmpFile = null;
		try (InputStream is = getClass().getResourceAsStream(absResourcePath)) {
			if (null == is) {
				return null;
			}
			tmpFile = makeTmpFile(name);
			try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
				copy(is, fos);
			}
		}
		final SearchResult result = SearchResult.succeeded(tmpFile,
				SearchStage.RELPATH_RELATIVE_TO_CLASSPATH, relPath);
		if (!predicate.test(result)) {
			result.stop = true;
		}
		return result;
	}

	private static String[] makeTmpFilePre_And_Suff_ix(String name) {
		// Split on the last dot...
		final String[] splits = name.split("\\.(?!\\.)", 2);
		assert 1 <= splits.length && splits.length <= 2;
		if (2 == splits.length) {
			splits[1] = '.' + splits[1];
			return splits;
		} else {
			assert -1 == splits[0].indexOf('.');
			return new String[] { splits[0], null };
		}
	}

	private static File makeTmpFile(String name) throws IOException {
		final String[] ps = makeTmpFilePre_And_Suff_ix(name);
		final File tmpFile = File.createTempFile(ps[0], ps[1]);
		tmpFile.deleteOnExit();
		return tmpFile;
	}

	private static void copy(InputStream is, FileOutputStream fos)
			throws IOException {
		final int N = 4096;
		byte[] buffer = new byte[N];
		for (int n = is.read(buffer, 0, N); 0 < n; n = is.read(buffer, 0, N)) {
			fos.write(buffer, 0, n);
		}
	}

	private static void validateRelpath(String relpath) {
		// Relpaths may not contain backslashes. Even on Windows, the forward
		// slash should be used to separate elements of the path.
		if (-1 < relpath.indexOf('\\')) {
			throw new IllegalArgumentException("illegal '\\' in relpath: '"
					+ relpath + "'");
		}
	}

	//
	// TEST
	//

	public static void main(String[] args) throws IOException {
		final int NAME = 0;
		final int PROPERTY = 1;
		final int RELPATH = 2;
		int state = NAME;
		final FileFinder f = new FileFinder(false);
		final ArrayList<String> names = new ArrayList<String>();
		final int N = args.length;
		for (int k = 0; k < N; ++k) {
			final String arg = args[k];
			if ("-s".equals(arg)) {
				f.setSearchSystemLibraryPath(true);
				state = NAME;
			} else if ("-p".equals(arg)) {
				state = PROPERTY;
			} else if ("-r".equals(arg)) {
				state = RELPATH;
			} else if ("-n".equals(arg)) {
				state = NAME;
			} else {
				switch (state) {
				case NAME:
					names.add(arg);
					break;
				case PROPERTY:
					f.addSearchPathPropertyNames(arg);
					break;
				case RELPATH:
					f.addRelPaths(arg);
					break;
				default:
					throw new RuntimeException("bad state");
				}
			}
		}
		if (names.isEmpty()) {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			String name;
			while (null != (name = br.readLine())) {
				names.add(name);
			}
		}
		for (final String name : names) {
			System.out.println("searching for '" + name + "'...");
			final SearchResult r = f.find(name, (SearchResult x) -> {
				if (SearchStage.RELPATH_RELATIVE_TO_CLASSPATH == x.stage) {
					// Clean up the temporary
					x.file.deleteOnExit();
				}
				System.out.println("    => " + x);
				return true;
			});
			System.out.println("    FIRST => " + r);
		}
	}
}
