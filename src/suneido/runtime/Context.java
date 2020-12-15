/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suneido.util.ThreadSafe;

import suneido.SuException;

/**
 * The "global" context for a function or method.
 * Names are assigned unique integer slots.
 * A context knows how to lookup the value for a name.
 * Values are cached.<p>
 * Derived classes must define fetch(name)
 *
 * @see ContextLayered - the current system
 * @see ContextModules - a possible future system, not currently used
 */
@ThreadSafe
public abstract class Context {
	private final Contexts contexts;
	private final Map<String, Integer> nameToSlot = new HashMap<>();
	private final List<String> names = new ArrayList<>(1000);
	private final List<Object> values = new ArrayList<>(1000);
	private final Map<String, String> override = new HashMap<>();
	private static final Object nonExistent = new Object();

	// protected so we can derive a trivial context for testing purposes
	protected Context(Contexts contexts) {
		this.contexts = contexts;
		contexts.addContext(this);
		// don't use slot 0
		names.add(null);
		values.add(null);
	}

	/**
	 * Called by compile.
	 * @return The slot for a name, assigning a new slot for a new name.
	 */
	public final synchronized int slotForName(String name) {
		return nameToSlot.computeIfAbsent(name, key -> {
			assert names.size() == values.size();
			int slot = names.size();
			names.add(name);
			values.add(null);
			return slot;
			});
	}

	public final synchronized Object get(String name) {
		return get(slotForName(name));
	}

	/** Called by compiled code to get the value of a global */
	public final synchronized Object get(int slot) {
		Object value = tryget(slot);
		if (value == null)
			throw new SuException("can't find " + nameForSlot(slot));
		return value;
	}

	/** Called for rules and triggers and UserDefined */
	public synchronized final Object tryget(String name) {
		return tryget(slotForName(name));
	}

	private synchronized final Object tryget(int slot) {
		Object value = values.get(slot);
		if (value == null) {
			values.set(slot, nonExistent); // in case fetch fails
			String name = nameForSlot(slot);
			value = name.contains("@") ? contexts.fetchExplicit(name) : fetch(name);
			values.set(slot, value == null ? nonExistent : value);
			// nonExistent is used to avoid repeating failing fetches
		}
		return value == nonExistent ? null : value;
	}

	public synchronized final String nameForSlot(int slot) {
		return names.get(slot);
	}

	/** Lookup the value for a name in this context */
	abstract protected Object fetch(String name);

	/** Remove the cached value for a slot. Called by Unload */
	public synchronized final void clear(String name) {
		values.set(slotForName(name), null);
	}

	/** Remove the cached values for all slots. Called by Use & Unuse */
	public synchronized final void clearAll() {
		for (int i = 0; i < values.size(); ++i)
			values.set(i, null);
	}

	/** Add or remove an override of specific record. Called by LibraryOverride */
	public synchronized final void override(String lib, String name, String text) {
		String key = lib + ':' + name;
		if (text.isEmpty()) {
			if (!override.containsKey(key))
				return;
			override.remove(key);
		} else
			override.put(key, text);
		clear(name);
	}
	/** Remove all overrides. Called by LibraryOverrideClear */
	public synchronized final void overrideClear() {
		for (Map.Entry<String, String> e : override.entrySet()) {
			String name = e.getKey().substring(e.getKey().indexOf(':') + 1);
			clear(name);
		}
		override.clear();
	}

	protected String getOverride(String lib, String name) {
		return override.get(lib + ':' + name);
	}

	/**
	 * Used by ContextLayered overloading.
	 * Also used by tests which is why it is public.
	 */
	public synchronized final void set(String name, Object value) {
		values.set(slotForName(name), value);
	}

}
