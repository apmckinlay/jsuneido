/* Copyright 2018 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

/** Used by tests. Similar to Display.
 * Implemented by SuObject to sort named members
 * Implemented by SuClass to show contents
 * Implemented by SuCallable to show params
 * For containers, shows members in sorted order
 */
public interface Showable {

	String show();

	static String show(Object x) {
		return x instanceof Showable ? ((Showable) x).show() : Ops.display(x);
	}

}
