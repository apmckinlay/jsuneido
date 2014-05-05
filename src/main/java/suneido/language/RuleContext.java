/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import suneido.SuContainer;

/**
 * used to auto-register dependencies
 */
@ThreadSafe
public class RuleContext {
	private static final ThreadLocal<Deque<Rule>> activeRules =
			new ThreadLocal<Deque<Rule>>() {
				@Override
				public Deque<Rule> initialValue() {
					return new ArrayDeque<>();
				}
			};

	public static void push(SuContainer rec, Object member) {
		activeRules.get().push(new Rule(rec, member));
	}

	public static Rule top() {
		return activeRules.get().peek();
	}

	public static void pop(SuContainer rec, Object member) {
		Rule ar = activeRules.get().pop();
		assert rec == ar.rec && member == ar.member;
	}

	@Immutable
	public static class Rule {
		public final SuContainer rec; // TODO should be SuRecord
		public final Object member;

		public Rule(SuContainer rec, Object member) {
			this.rec = rec;
			this.member = member;
		}
	}

}
