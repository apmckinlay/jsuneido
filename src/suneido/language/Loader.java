/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

interface Loader {

	/** @return The definition of name in module */
	Object load(String module, String name);

}
