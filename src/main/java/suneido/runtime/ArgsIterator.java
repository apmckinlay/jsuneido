/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static suneido.SuContainer.IterResult.ENTRY;
import static suneido.SuContainer.IterWhich.ALL;
import static suneido.runtime.Args.Special.EACH;
import static suneido.runtime.Args.Special.EACH1;
import static suneido.runtime.Args.Special.NAMED;

import java.util.AbstractMap;
import java.util.Iterator;

import suneido.runtime.builtin.ContainerMethods;
import suneido.util.NullIterator;

import com.google.common.collect.Iterators;

/**
 * Iterates through the arguments passed to a Suneido function.
 * Handles EACH, EACH1, NAMED.
 * Returns Map.Entry for named arguments.
 *
 * Used by {@link ContainerMethods.evaluate} and {@link Ops.callString}
 *
 * @see Args
 */
public class ArgsIterator implements Iterator<Object>, Iterable<Object> {
	private final Object[] args;
	private boolean named = true;
	private int argi = 0;
	Iterator<Object> each = new NullIterator<>();

	public ArgsIterator(Object[] args) {
		this.args = args;
	}

	@Override
	public boolean hasNext() {
		if (each.hasNext())
			return true;
		if (argi >= args.length)
			return false;
		Object x = args[argi];
		if (x != EACH && x != EACH1)
			return true;
		return Ops.toContainer(args[argi + 1]).size() > (x == EACH1 ? 1 : 0);
	}

	@Override
	public Object next() {
		if (each.hasNext())
			return each.next();
		Object x = args[argi++];
		if (x == EACH || x == EACH1) {
			each = Ops.toContainer(args[argi++]).iterator(ALL, ENTRY);
			if (x == EACH1 && each.hasNext())
				each.next();
			return next();
		} else if (x == NAMED && named) {
			x = args[argi++];
			return new AbstractMap.SimpleEntry<>(x, args[argi++]);
		}
		return x;
	}

	/** @return the remaining arguments as Object[] */
	public Object[] rest() {
		named = false;
		return Iterators.toArray(this, Object.class);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 * to allow: for (x : ArgsIterator(args))
	 */
	@Override
	public Iterator<Object> iterator() {
		return this;
	}

}
