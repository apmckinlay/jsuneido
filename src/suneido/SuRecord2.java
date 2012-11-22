/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.util.*;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class SuRecord2 extends SuContainer {
	private final Map<Object, FieldInfo> info = Maps.newHashMap();
	private final Deque<Object> activeRules = new ArrayDeque<Object>();

	private static class FieldInfo {
		boolean invalidated; // but not necessarily invalid
		List<Dependency> dependencies = Lists.newArrayList();
		Set<Object> usedBy = Sets.newHashSet();
	}

	private static class Dependency {
		Object field;
		Object value;
		boolean invalidated = false;
		public Dependency(Object field, Object value) {
			this.field = field;
			this.value = value;
		}
	}

	// put ---------------------------------------------------------------------

	@Override
	public void put(Object field, Object value) {
		makeValid(field); // before get
		if (alreadyHas(field, value))
			return;
		super.put(field, value);
		invalidateUsers(field);
	}

	private boolean alreadyHas(Object field, Object value) {
		if (containsKey(field)) {
			Object old = super.get(field);
			if (old != null && old.equals(value))
				return true;
		}
		return false;
	}

	// recursive
	private void invalidateUsers(Object field) {
		FieldInfo fi = info.get(field);
		if (fi == null)
			return;
		invalidateUsers(field, fi);
	}

	private void invalidateUsers(Object field, FieldInfo fi) {
		Iterator<Object> iter = fi.usedBy.iterator();
		while (iter.hasNext()) {
			Object user = iter.next();
			if (! invalidate(user, field))
				iter.remove();
		}
	}

	/**
	 * @return Whether or not source is currently a dependency.
	 */
	private boolean invalidate(Object field, Object source) {
		FieldInfo fi = info.get(field);
		if (fi == null)
			return false;
		boolean invalidated = false;
		boolean isDependency = false;
		for (Dependency d : fi.dependencies)
			if (d.field.equals(source)) {
				isDependency = true;
				if (! d.invalidated)
					invalidated = d.invalidated = true;
			}
		if (invalidated && ! fi.invalidated) {
			fi.invalidated = true; // before recursing
			invalidateUsers(field, fi); // recurse
		}
		return isDependency;
	}

	// get ---------------------------------------------------------------------

	@Override
	public Object get(Object field) {
		Object value = containsKey(field) ? super.get(field) : null;

		RuleContext.Rule ar = RuleContext.top();
		if (ar != null && ar.rec == this)
			addDependency(ar.field, field, value);

		if (value != null && isValid(field))
			return value;

		makeValid(field);
		Object x = callRule(field);
		if (x == null)
			return "";
		else {
			putMap(field, x);
			return x;
		}
	}

	private void addDependency(Object field, Object field2, Object value) {
		// add field2 to field dependencies (if not already there)
		FieldInfo fi = getOrCreateFieldInfo(field);
		for (Dependency d : fi.dependencies) {
			if (d.field.equals(field2)) {
				if (! value.equals(d.value))
					throw new RuntimeException(
							"dependency has inconsistent value " +
							field + " => " + field2);
				return; // already has dependency
			}
		}
		fi.dependencies.add(new Dependency(field2, value));

		// add field to field2 usedBy
		FieldInfo fi2 = getOrCreateFieldInfo(field2);
		fi2.usedBy.add(field);
	}

	private FieldInfo getOrCreateFieldInfo(Object field) {
		FieldInfo fi = info.get(field);
		if (fi == null)
			info.put(field, fi = new FieldInfo());
		return fi;
	}

	private void makeValid(Object field) {
		// can't just delete info because we need to keep usedBy
		FieldInfo fi = info.get(field);
		if (fi == null)
			return;
		fi.invalidated = false;
		fi.dependencies.clear();
	}

	private boolean isValid(Object field) {
		FieldInfo fi = info.get(field);
		if (fi == null || ! fi.invalidated)
			return true;
		for (Dependency d : fi.dependencies) {
			if (d.invalidated && ! get(d.field).equals(d.value))
				return false;
			d.invalidated = false;
		}
		// all dependencies had same values
		fi.invalidated = false;
		return true;
	}

	/**
	 * @return Result of rule if there is one, otherwise null.
	 */
	private Object callRule(Object field) {
		Object rule = getRule(field);
		if (rule == null)
			return null;
		// prevent cycles
		if (activeRules.contains(field))
			return null;
		activeRules.push(field);
		try {
			RuleContext.push(this, field);
			try {
				if (rule instanceof SuValue) {
					Object x = executeRule(rule);
					return x;
				} else
					throw new SuException("invalid Rule_" + field);
			} finally {
				RuleContext.pop(this, field);
			}
		} finally {
			assert activeRules.peek() == field;
			activeRules.pop();
		}
	}

	// overridden by tests
	Object getRule(Object field) {
		return Suneido.context.tryget("Rule_" + field);
	}

	// overridden by tests
	Object executeRule(Object rule) {
		return ((SuValue) rule).eval(this);
	}

	/**
	 * used to auto-register dependencies
	 */
	@ThreadSafe
	private static class RuleContext {
		private static final ThreadLocal<Deque<Rule>> activeRules =
				new ThreadLocal<Deque<Rule>>() {
					@Override
					public Deque<Rule> initialValue() {
						return new ArrayDeque<Rule>();
					}
				};

		static void push(SuRecord2 rec, Object field) {
			activeRules.get().push(new Rule(rec, field));
		}

		static Rule top() {
			return activeRules.get().peek();
		}

		static void pop(SuRecord2 rec, Object field) {
			Rule ar = activeRules.get().pop();
			assert rec.equals(ar.rec) && field.equals(ar.field);
		}

		@Immutable
		static class Rule {
			public final SuRecord2 rec;
			public final Object field;

			public Rule(SuRecord2 rec, Object field) {
				this.rec = rec;
				this.field = field;
			}
		}

	}

}
