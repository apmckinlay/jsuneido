/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

/**
 * The interface to a module.
 * Initially module = library, but could have sub-modules later.
 *
 * @see ModuleBuiltins
 * @see ModuleLoader
 */
interface Module {

	Object get(String name);

}
