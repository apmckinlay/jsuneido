/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import suneido.SuException;
import suneido.TheDbms;
import suneido.database.server.Dbms.LibGet;

/**
 * Old style context with a stack of layered libraries "in use"
 * <p>
 * Overloading (_Name) has two forms:
 * - class base e.g. Name = _Name { ... } - "previous" value is given unique name
 * - reference in code - "previous" value becomes constant
 */
public class ContextLayered extends Context {
	private static int overload = 0;

	public ContextLayered(Contexts contexts) {
		super(contexts);
	}

	/** caller (i.e. Context) must synchronize */
	@Override
	protected Object fetch(String name) {
		Object x = Builtins.get(name);
		if (x == null)
			x = libget(name);
		return x;
	}

	private Object libget(String name) {
		if (! TheDbms.isAvailable())
			return null;
		// System.out.println("LOAD " + name);
		Object result = null;
		for (LibGet libget : TheDbms.dbms().libget(name)) {
			String src = (String) Pack.unpack(libget.text);
			try {
				result = Compiler.compile(name, src, this);
				// needed inside loop for overloading references
				set(name, result);
			} catch (Exception e) {
				throw new SuException("error loading " + name, e);
			}
		}
		return result;
	}

	/** Called by AstCompile for classes that inherit from _Name */
	synchronized String overload(String base) {
		assert base.startsWith("_");
		String name = base.substring(1); // remove leading underscore
		String nameForPreviousValue = overload++ + base;
		set(nameForPreviousValue, get(name));
		return nameForPreviousValue;
	}

}
