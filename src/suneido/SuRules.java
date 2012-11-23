/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import suneido.util.Util;

import com.google.common.collect.*;

/** Implements rules */
public class SuRules extends SuContainer {
	/** usedBy is cumulative (never cleared) */
	private final SetMultimap<Object, Object> usedBy = HashMultimap.create();
	/** dependencies is accurate for the last time the rule ran */
	private final ListMultimap<Object, Dependency> dependencies = ArrayListMultimap.create();
	/** invalid tracks which fields are potentially invalid */
	private final Set<Object> invalid = Sets.newHashSet();
	/** activeRules is used to track which rule is currently active */
	private final Deque<Object> activeRules = new ArrayDeque<Object>();

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
		for (Object user : usedBy.get(field))
			invalidate(user, field);
	}

	private void invalidate(Object field, Object source) {
		List<Dependency> deps = dependencies.get(field);
		boolean invalidated = false; //deps.isEmpty();
		for (Dependency d : deps)
			if (d.field.equals(source) && ! d.invalidated)
				invalidated = d.invalidated = true;
		if (invalidated) {
			invalidate(field);
		}
	}

	private void invalidate(Object field) {
		if (! invalid.add(field)) // before recursing
			return; // already invalid
		invalidateUsers(field); // recurse
		invalidated(field); // for observers
	}

	/** hook for observers */
	protected void invalidated(Object field) {
	}

	public void forceInvalidate(Object field) {
		dependencies.removeAll(field); // unconditionally invalid
		invalidate(field);
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
		for (Dependency d : dependencies.get(field)) {
			if (d.field.equals(field2)) {
				if (! value.equals(d.value))
					throw new RuntimeException(
							"dependency has inconsistent value " +
							field + " => " + field2);
				return; // already has dependency
			}
		}
		dependencies.put(field, new Dependency(field2, value));
		usedBy.put(field2, field);
	}

	private void makeValid(Object field) {
		dependencies.removeAll(field);
		invalid.remove(field);
	}

	private boolean isValid(Object field) {
		if (! invalid.contains(field))
			return true;
		List<Dependency> deps = dependencies.get(field);
		if (deps.isEmpty()) // invalidated by user
			return false;
		for (Dependency d : deps) {
			if (d.invalidated && ! get(d.field).equals(d.value))
				return false;
			d.invalidated = false;
		}
		// all dependencies had same values
		invalid.remove(field);
		return true;
	}

	/** @return Result of rule if there is one, otherwise null */
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

	/** used to auto-register dependencies */
	@ThreadSafe
	private static class RuleContext {
		private static final ThreadLocal<Deque<Rule>> activeRules =
				new ThreadLocal<Deque<Rule>>() {
					@Override
					public Deque<Rule> initialValue() {
						return new ArrayDeque<Rule>();
					}
				};

		static void push(SuRules rec, Object field) {
			activeRules.get().push(new Rule(rec, field));
		}

		static Rule top() {
			return activeRules.get().peek();
		}

		static void pop(SuRules rec, Object field) {
			Rule ar = activeRules.get().pop();
			assert rec.equals(ar.rec) && field.equals(ar.field);
		}

		@Immutable
		static class Rule {
			public final SuRules rec;
			public final Object field;

			public Rule(SuRules rec, Object field) {
				this.rec = rec;
				this.field = field;
			}
		}
	}

	//--------------------------------------------------------------------------

	private final Object dummyValue = new Object();

	public void setdeps(String field, String s) {
		List<Dependency> deps = dependencies.get(field);
		deps.clear();
		for (String d : Util.commaSplitter(s)) {
			// not checking for duplicates
			deps.add(new Dependency(d, dummyValue));
			usedBy.put(d, field);
		}
	}

	public List<String> getdeps(String field) {
		ImmutableList.Builder<String> builder = ImmutableList.builder();
		for (Dependency d : dependencies.get(field))
			builder.add(d.field.toString());
		return builder.build();
	}

}
