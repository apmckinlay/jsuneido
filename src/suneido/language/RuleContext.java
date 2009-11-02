package suneido.language;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import suneido.SuRecord;

/**
 * used to auto-register dependencies
 *
 * @author Andrew McKinlay
 */
@ThreadSafe
public class RuleContext {

	public static void push(SuRecord rec, Object member) {
		activeRules.get().push(new Rule(rec, member));
	}

	public static Rule top() {
		return activeRules.get().peek();
	}

	public static void pop(SuRecord rec, Object member) {
		Rule ar = activeRules.get().pop();
		assert rec.equals(ar.rec) && member.equals(ar.member);
	}

	@Immutable
	public static class Rule {
		public final SuRecord rec;
		public final Object member;

		public Rule(SuRecord rec, Object member) {
			this.rec = rec;
			this.member = member;
		}
	}

	private static final ThreadLocal<Deque<Rule>> activeRules =
			new ThreadLocal<Deque<Rule>>() {
				@Override
				public Deque<Rule> initialValue() {
					return new ArrayDeque<Rule>();
				}
			};

}
