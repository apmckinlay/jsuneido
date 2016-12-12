/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

/**
 * Implements the {@code GlobalsInfo()} built-in.
 *
 * @author Victor Schappert
 * @since 20130819
 * @see SymbolsInfo
 * @see ThreadCount
 */
public final class GlobalsInfo {

	private static final String DUMMY_STRING = "Globals: Count 0 (max 0), Size 0 (max 0)";

	public static String GlobalsInfo() {
		// TODO: Implement me
		return DUMMY_STRING;
	}

}
