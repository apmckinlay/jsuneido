/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.Immutable;

public class Regex {

	public static Pattern compile(String rx) {
		return new Compiler(rx).compile();
	}

	static class Pattern {
		private final List<Element> pat;

		private Pattern(List<Element> pat) {
			this.pat = pat;
		}

		public int amatch(String s) {
			return amatch(s, 0);
		}

		/**
		 * Try to match at a specific position.
		 * @return The position after the match, or <0 if no match
		 */
		public int amatch(String s, int si) {
			return Regex.amatch(pat, 0, pat.size(), s, si);
		}

		@Override
		public String toString() {
			return toStr(pat, 0, pat.size());
		}

	}

	private static String toStr(List<Element> pat, int pi, int plim) {
		StringBuilder sb = new StringBuilder();
		for (; pi < plim; ++pi) {
			Element e = pat.get(pi);
			sb.append(e.toString()).append(" ");
		}
		return sb.toString();
	}

	/**
	 * Try matching a slice of pat to a specify position (si) in s
	 * @return The position after the match, or <0 if no match
	 */
	private static int amatch(List<Element> pat, int pi, int plim, String s, int si) {
System.out.println("amatch { " + toStr(pat, pi, plim) + "} '" + s.substring(si) + "'");
		final int MAX_BRANCH = 1000;
		int alt_si[] = new int[MAX_BRANCH];
		int alt_pi[] = new int[MAX_BRANCH];
		int na = 0;
		for (; pi < plim; ) {
			Element e = pat.get(pi);
			if (e instanceof Branch) {
				int offset = e.advance();
				if (offset < 0) { // branch backward e.g. '+' or '*'
					alt_pi[na] = pi + 1;
					pi += offset; // default is to take branch (greedy)
				} else { // branch forward e.g. '?' or '+'
					++pi; // default is to NOT take branch, i.e. try to match
					alt_pi[na] = pi + offset;
				}
				alt_si[na] = si;
				++na;
			} else {
int start = si;
System.out.println("omatch " + e + " to '" + s.substring(si) + "'");
			si = e.omatch(s, si, pat, pi);
			if (si >= 0)
{ System.out.println("matched " + e + " to '" + s.substring(start, si) + "'");
				++pi; }
			else if (na > 0) {
				// backtrack
				--na;
				si = alt_si[na];
				pi = alt_pi[na];
System.out.println("backtrack");
			} else
{ System.out.println("amatch failed on " + e);
				return FAIL; }
			}

		}
System.out.println("amatch succeeded leaving '" + s.substring(si) + "'");
		return si;
	}

	// implementation ----------------------------------------------------------

	private final static int FAIL = Integer.MIN_VALUE;

	abstract static class Element {
		/** @return FAIL or the position after the match */
		int omatch(String s, int si, List<Element> pat, int pi) {
			return omatch(s, si);
		}
		int omatch(String s, int si) {
			throw new RuntimeException("must be overridden");
		}
		int advance() {
			return 1; // overridden by repetition
		}
	}

	@Immutable
	static class StartOfLine extends Element {

		@Override
		public int omatch(String s, int si) {
			return (si == 0 || s.charAt(si - 1) == '\n') ? si : FAIL;
		}

		@Override
		public String toString() {
			return "^";
		}
	}
	final static Element startOfLine = new StartOfLine();

	@Immutable
	static class EndOfLine extends Element {

		@Override
		public int omatch(String s, int si) {
			return (si >= s.length() || s.charAt(si) == '\n') ? si : FAIL;
		}

		@Override
		public String toString() {
			return "$";
		}
	}
	final static Element endOfLine = new EndOfLine();

	@Immutable
	static class Chars extends Element {
		String chars;

		Chars(String chars) {
			this.chars = chars;
		}

		@Override
		public int omatch(String s, int si) {
			return s.startsWith(chars, si) ? si + chars.length() : FAIL;
		}

		@Override
		public String toString() {
			return "'" + chars + "'";
		}
	}

	@Immutable
	static class Any extends Element {

		@Override
		public int omatch(String s, int si) {
			return (si < s.length() && s.charAt(si) != '\r' && s.charAt(si) != '\n')
					? si + 1 : FAIL;
		}

		@Override
		public String toString() {
			return "ANY";
		}
	}
	final static Element any = new Any();

	@Immutable
	static class Branch extends Element {
		int offset;

		Branch(int offset) {
			this.offset = offset;
		}

		@Override
		int omatch(String s, int si) {
			return si;
		}

		@Override
		int advance() {
			return offset;
		}

		@Override
		public String toString() {
			return "Branch(" + offset + ")";
		}
	}

	private static class Compiler {
		String src;
		int si = 0;
		int sn;
		ArrayList<Element> pat = new ArrayList<>();

		Compiler(String src) {
			this.src = src;
			sn = src.length();
		}

		Pattern compile() {
			regex();
			return new Pattern(pat);
		}

		private void regex() {
			sequence(); // TODO '|'
		}

		void sequence() {
			while (si < sn && nextNot1of("|)"))
				element();
		}

		void element() {
			if (match1('^'))
				emit(startOfLine);
			else if (match1('$'))
				emit(endOfLine);
			else {
				int start = pat.size();
				simple();
				int len = pat.size() - start;
				if (match1('?'))
					insert(start, new Branch(len));
				else if (match1('+'))
					emit(new Branch(-len));
				else if (match1('*')) {
					emit(new Branch(-len));
					insert(start, new Branch(len + 1));
				}
			}
		}

		void simple() {
			if (match1('.'))
				emit(any);
			else if (match1('(')) {
				regex();
				match1(')');
			} else {
				int start = si;
				do
					++si;
					while (si < sn && ! special(src.charAt(si)));
				if (next1of("?*+") && si - 1 > start)
					--si;
				emit(new Chars(src.substring(start, si)));
			}
		}

		boolean match1(char c) {
			if (si < sn && src.charAt(si) == c) {
				++si;
				return true;
			} else
				return false;
		}

		boolean next1of(String set) {
			return si < sn && set.indexOf(src.charAt(si)) != -1;
		}

		boolean nextNot1of(String set) {
			return si < sn && set.indexOf(src.charAt(si)) == -1;
		}

		void emit(Element e) {
			pat.add(e);
		}

		void insert(int i, Element e) {
			pat.add(i, e);
		}

		static boolean special(char c) {
			return "^$.?+*|()[".indexOf(c) != -1;
		}
	}

	public static void main(String[] args) {
		String s = "ac";
		int i = Regex.compile("ab?c").amatch(s);
		if (i >= 0)
			System.out.println("MATCHED '" + s.substring(0, i));
		else
			System.out.println("FAILED");
	}

}
