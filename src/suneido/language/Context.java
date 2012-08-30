/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;

import com.google.common.collect.Lists;

/**
 * The "global" context for a function or method.
 * Names are assigned unique integer slots.
 * A context knows how to lookup the value for a name.
 * Values are cached.<p>
 * Derived classes must define fetch(name)
 *
 * @see ContextLayered
 * @see ContextModules
 */
@ThreadSafe
public abstract class Context {
	private final Contexts contexts;
	private final ConcurrentHashMap<String,Integer> nameToSlot =
			new ConcurrentHashMap<String,Integer>();
	// these list should be threadsafe
	private final ArrayList<String> names = Lists.newArrayList();
	private final ArrayList<Object> values = Lists.newArrayList();

	Context(Contexts contexts) {
		this.contexts = contexts;
		contexts.addContext(this);
		// don't use slot 0
		names.add(null);
		values.add(null);
	}

	/** @return The slot for a name, assigning a new slot for a new name */
	int slotForName(String name) {
		Integer slot = nameToSlot.get(name);
		return (slot == null) ? newSlot(name) : slot;
	}

	synchronized private int newSlot(String name) {
		int slot = names.size();
		names.add(name);
		values.add(null);
		nameToSlot.put(name, slot);
		// WARNING: concurrency bug if nameToSlot change becomes visible before
		// names and values adds
		return slot;
	}

	public Object get(String name) {
		return get(slotForName(name));
	}

	public Object tryget(String name) {
		return tryget(slotForName(name));
	}

	/** Get the value for a slot. If no cached value, then do lookup */
	Object get(int slot) {
		Object value = tryget(slot);
		if (value == null)
			throw new SuException("can't find " + nameForSlot(slot));
		return value;
	}

	private Object tryget(int slot) {
		Object value = values.get(slot);
		if (value == null) {
			String name = nameForSlot(slot);
			value = name.contains("@") ? contexts.fetchExplicit(name) : fetch(name);
		}
		if (value != null)
			values.set(slot, value);
		return value;
	}

	String nameForSlot(int slot) {
		return names.get(slot);
	}

	/** Lookup the value for a name in this context */
	abstract protected Object fetch(String name);

	/** Remove the cached value for a slot. */
	public void clear(String name) {
		clear(slotForName(name));
	}

	/** Remove the cached value for a slot. */
	void clear(int slot) {
		values.set(slot, null);
	}

	/** Remove the cached values for all slots. */
	public void clearAll() {
		for (int i = 0; i < values.size(); ++i)
			values.set(i, null);
	}

	/** used by overloading and tests */
	public void set(String name, Object value) {
		values.set(slotForName(name), value);
	}

}
