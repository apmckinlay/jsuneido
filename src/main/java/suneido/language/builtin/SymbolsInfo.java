/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

/**
 * Implements the {@code SymbolsInfo()} built-in.
 *
 * @author Victor Schappert
 * @since 20130819
 * @see GlobalsInfo
 * @see ThreadCount
 */
public final class SymbolsInfo {

	private static final String DUMMY_STRING = "Symbols: Count 0 (max 0), Size 0 (max 0)";

	public static String SymbolsInfo() {
		// TODO: Implement me
		return DUMMY_STRING;
	}

}
