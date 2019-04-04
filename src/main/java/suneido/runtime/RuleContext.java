/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.util.ArrayDeque;
import java.util.Deque;

import suneido.util.Immutable;
import suneido.util.ThreadSafe;

import suneido.SuObject;

/**
 * used to auto-register dependencies
 */
@ThreadSafe
public class RuleContext {
	private static final ThreadLocal<Deque<Rule>> activeRules =
			ThreadLocal.withInitial(ArrayDeque::new);

	public static void push(SuObject rec, Object member) {
		activeRules.get().push(new Rule(rec, member));
	}

	public static Rule top() {
		return activeRules.get().peek();
	}

	public static void pop(SuObject rec, Object member) {
		Rule ar = activeRules.get().pop();
		assert rec == ar.rec && member == ar.member;
	}

	@Immutable
	public static class Rule {
		public final SuObject rec; // TODO should be SuRecord
		public final Object member;

		public Rule(SuObject rec, Object member) {
			this.rec = rec;
			this.member = member;
		}
	}

}
