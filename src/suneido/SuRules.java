/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.util.*;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import suneido.util.CommaStringBuilder;
import suneido.util.Util;

import com.google.common.collect.*;

/**
 * Implements rules layered on top of {@link SuContainer}.
 * {@link SuObservers} is layered on top of this,
 * and then {@link SuRecord} on top of that.
 * <p>
 * usedBy is for invalidation.
 * It is cumulative and may contain extra fields if the dependencies change.
 * But invalidation also checks dependencies
 * <p>
 * dependencies is accurate for the last time the rule ran.
 * It tracks both fields and their values.
 * Although it's a ListMultimap, the lists are sets as far as Dependency.field.
 * Rules should be pure functional so the value should be consistent.
 * <p>
 * invalid tracks which rules are potentially invalid.
 * get on an invalid field checks the values of its dependencies.
 * If none have changed, then the field is actually still valid
 * and we avoid running the rule.
 */
public class SuRules extends SuContainer {
	/** usedBy is cumulative (never cleared) */
	private final SetMultimap<Object, Object> usedBy;
	/** dependencies is accurate for the last time the rule ran */
	private final ListMultimap<Object, Dependency> dependencies;
	/** invalid tracks which fields are potentially invalid */
	private final Set<Object> invalid;
	private final Map<Object, Object> attachedRules;
	/** activeRules is used to track which rule is currently active */
	private final Deque<Object> activeRules = new ArrayDeque<Object>();

	private static class Dependency {
		Object field;
		/** value is set to null if inconsistent */
		Object value;
		boolean invalidated = false;
		public Dependency(Object field, Object value) {
			this.field = field;
			this.value = value;
		}
	}

	public SuRules() {
		usedBy = HashMultimap.create();
		dependencies = ArrayListMultimap.create();
		invalid = Sets.newHashSet();
		attachedRules = Maps.newHashMap();
	}

	public SuRules(SuRules r) {
		super(r);
		usedBy = HashMultimap.create(r.usedBy);
		dependencies = ArrayListMultimap.create(r.dependencies);
		invalid = Sets.newHashSet(r.invalid);
		attachedRules = Maps.newHashMap(r.attachedRules);
	}

	// put ---------------------------------------------------------------------

	@Override
	public void put(Object field, Object value) {
		// dependencies.removeAll(field);
		for (Dependency d : dependencies.get(field)) {
			d.value = INCONSISTENT;
			d.invalidated = false;
		}
		invalid.remove(field); // before get
		if (alreadyHas(field, value))
			return;
		super.put(field, value);
		invalidateUsers(field);
	}

	private boolean alreadyHas(Object field, Object value) {
		return Objects.equals(value, getIfPresent(field));
	}

	// recursive
	private void invalidateUsers(Object field) {
		for (Object user : usedBy.get(field))
			invalidate(user, field);
	}

	private void invalidate(Object field, Object source) {
		boolean invalidated = false;
		for (Dependency d : dependencies.get(field))
			if (d.field.equals(source) && ! d.invalidated)
				invalidated = d.invalidated = true;
//		if (invalidated)
			invalidate1(field);
	}

	private void invalidate1(Object field) {
		if (! invalid.add(field)) // before recursing
			return; // already invalid
		invalidateUsers(field); // recurse
		invalidated(field); // for observers
	}

	/** hook for observers */
	protected void invalidated(Object field) {
	}

	/** called by RecordMethods Invalidate */
	public void invalidate(Object field) {
		dependencies.removeAll(field); // unconditionally invalid
		invalidate1(field);
	}

	// get ---------------------------------------------------------------------

	@Override
	public Object get(Object field) {
		Object result = getIfPresent(field);

		RuleContext.Rule ar = RuleContext.top();
		if (ar != null && ar.rec == this)
			addDependency(ar.field, field, result);

		if (result == null || ! isValid(field)) {
			invalid.remove(field);
//			dependencies.removeAll(field);
			Object x = callRule(field);
			if (x != null) {
				result = x;
				if (! getReadonly())
					putMap(field, x);
//System.out.println("callRule " + field + " => " + x);
			} else if (result == null)
				result = defval;
		}
		return result;
	}

	private static Object INCONSISTENT = new Object();

	private void addDependency(Object field, Object field2, Object value) {
		usedBy.put(field2, field);
		if (value instanceof SuContainer &&
				! ((SuContainer) value).getReadonly())
			value = INCONSISTENT;
		// add field2 to field dependencies (if not already there)
		for (Dependency d : dependencies.get(field)) {
			if (d.field.equals(field2)) {
				if (! Objects.equals(value, d.value))
					d.value = INCONSISTENT;
				return; // already has dependency
			}
		}
		dependencies.put(field, new Dependency(field2, value));
	}

	private boolean isValid(Object field) {
return ! invalid.contains(field);
//		if (! invalid.contains(field))
//			return true;
//		List<Dependency> deps = dependencies.get(field);
//		if (deps.isEmpty()) // invalidated by user
//			return false;
//		for (Dependency d : deps) {
//			if (d.invalidated &&
//					(d.value == INCONSISTENT || ! get(d.field).equals(d.value)))
//				return false;
//			d.invalidated = false;
//		}
//		// all dependencies had same values
//		invalid.remove(field);
//		return true;
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
					Object x = ((SuValue) rule).eval(this);
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

	private Object getRule(Object field) {
		Object rule = attachedRules.get(field);
		if (rule == null && defval != null)
			rule = Suneido.context.tryget("Rule_" + field);
		return rule;
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

	public void setdeps(String field, String s) {
		List<Dependency> deps = dependencies.get(field);
//		deps.clear();
		for (String d : Util.commaSplitter(s)) {
			// not checking for duplicates
			deps.add(new Dependency(d, INCONSISTENT));
			usedBy.put(d, field);
		}
	}

	public String getdeps(String field) {
		CommaStringBuilder deps = new CommaStringBuilder();
//		for (Dependency d : dependencies.get(field))
//			deps.add(d.field);
for (Object key : usedBy.keySet()) {
	Set<Object> values = usedBy.get(key);
	if (values.contains(field))
		deps.add(key.toString());
}
		return deps.toString();
	}

	public void attachRule(String field, Object rule) {
		attachedRules.put(field, rule);
	}

}
