/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.util.regex.Pattern;

import suneido.SuException;
import suneido.TheDbms;
import suneido.compiler.Compiler;
import suneido.database.server.Dbms.LibGet;
import suneido.util.ByteBuffers;

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

	static final Pattern windows =
			Pattern.compile("jSuneido does not implement (dll|struct|callback)");

	private Object libget(String name) {
		if (! TheDbms.isAvailable())
			return null;
		// System.out.println("LOAD " + name);
		Object result = null;
		SuException error = null;
		for (LibGet libget : TheDbms.dbms().libget(name)) {
			String src = getOverride(libget.library, name);
			if (src == null)
				src = ByteBuffers.bufferToString(libget.text);
			try {
				try {
					result = Compiler.compile(libget.library, name, src, this);
				} catch (SuException e) {
					if (windows.matcher(e.toString()).find())
						error = e;
					else
						throw e;
				}
				// needed inside loop for overloading references
				set(name, result);
			} catch (Exception e) {
				set(name, null);
				throw new SuException("error loading " + name, e);
			}
		}
		if (result == null && error != null)
			throw new SuException("error loading " + name, error);
		return result;
	}

	/** Called by AstCompile for classes that inherit from _Name */
	public synchronized String overload(String base) {
		assert base.startsWith("_");
		String name = base.substring(1); // remove leading underscore
		String nameForPreviousValue = overload++ + base;
		set(nameForPreviousValue, get(name));
		return nameForPreviousValue;
	}

}
