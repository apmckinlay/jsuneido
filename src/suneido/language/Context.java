/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;

import suneido.SuException;

import com.google.common.collect.Lists;

/**
 * The "global" context for a function or method.
 * Names are assigned unique integer slots.
 * A context knows how to lookup the value for a name.
 * Values are cached.
 *
 * @see ContextLibraries
 * @see ContextModules
 */
abstract class Context {
	private final TObjectIntHashMap<String> nameToSlot = new TObjectIntHashMap<String>();
	private final ArrayList<String> names = Lists.newArrayList();
	private final ArrayList<Object> values = Lists.newArrayList();

	Context() {
		// don't use slot 0
		names.add(null);
		values.add(null);
	}

	/** @return The slot for a name, assigning a new slot for a new name */
	int slotForName(String name) {
		int slot = nameToSlot.get(name);
		if (slot != 0)
			return slot;
		slot = names.size();
		nameToSlot.put(name, slot);
		names.add(name);
		values.add(null);
		return slot;
	}

	/** Get the value for a slot. If no cached value, then do lookup */
	Object get(int slot) {
		Object value = values.get(slot);
		if (value != null)
			return value;
		value = fetch(nameForSlot(slot));
		if (value == null)
			throw new SuException("can't find " + nameForSlot(slot));
		values.set(slot, value);
		return value;
	}

	String nameForSlot(int slot) {
		return names.get(slot);
	}

	/** Lookup the value for a name in this context */
	abstract protected Object fetch(String name);

	/** Remove the cached value for a slot. */
	void clear(String name) {
		clear(slotForName(name));
	}

	/** Remove the cached value for a slot. */
	void clear(int slot) {
		values.set(slot, null);
	}

	/** Remove the cached values for all slots. */
	void clearAll() {
		for (int i = 0; i < values.size(); ++i)
			values.set(i, null);
	}

}
