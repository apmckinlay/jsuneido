package suneido.language;

import java.util.ArrayDeque;
import java.util.Deque;

import suneido.SuRecord;

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

	public static class Rule {
		public SuRecord rec;
		public Object member;

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
