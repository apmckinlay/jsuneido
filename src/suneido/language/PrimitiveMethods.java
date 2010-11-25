/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.language.UserDefined.userDefined;
import suneido.language.builtin.*;

/**
 * The base class for classes that define methods for
 * Java types such as strings, integers, dates, etc.
 * @see DateMethods
 * @see NumberMethods
 * @see StringMethods
 */
public abstract class PrimitiveMethods extends SuClass {
	private final String name;

	protected PrimitiveMethods(String name, Object members) {
		super(name, null, members);
		this.name = name + "s";
	}

	@Override
	protected void linkMethods() {
	}

	@Override
	protected Object notFound(Object self, String method, Object... args) {
		return userDefined(name, self, method, args);
	}

}
