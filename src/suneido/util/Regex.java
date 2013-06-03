/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.CharMatcher;

/*
 * regular expression grammar and compiled form:
 *
 *	regex	:	sequence				LEFT 0 ... RIGHT 0 ENDPAT
 *			|	sequence (| sequence)+	Branch sequence (Jump Branch sequence)+
 *
 *	sequence	:	element+
 *
 *	element		:	^					startOfLine
 *				|	$					endOfLine
 *				|	\A					startOfString
 *				|	\Z					endOfString
 *				|	(?i)				IGNORE_CASE
 *				|	(?-i)				CASE_SENSITIVE
 *				|	(?q)				start quoted (literal)
 *				|	(?-q)				end quoted (literal)
 *				|	\<					START_WORD
 *				|	\>					END_WORD
 *				|	\#					PIECE #
 *				|	simple
 *				|	simple ?			Branch simple
 *				|	simple +			simple Branch
 *				|	simple *			Branch simple Branch
 *				|	simple ??			Branch simple
 *				|	simple +?			simple Branch
 *				|	simple *?			Branch simple Branch
 *
 *	simple		:	.					any
 *				|	[...]				CharClass
 *				|	[^...]				CharClass
 *				|	\d					CharClass
 *				|	\D					CharClass
 *				|	\w					CharClass
 *				|	\W					CharClass
 *				|	\s					CharClass
 *				|	\S					CharClass
 *				|	( regex )			LEFT i ... RIGHT i
 *				|	characters			Char(string) // multiple characters
 *
 */
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
		 * @return The position after the match, or -1 if no match
		 */
		public int amatch(String s, int si) {
int begin = si;
System.out.println("amatch { " + this + "} to '" + s.substring(si) + "'");
			final int MAX_BRANCH = 1000;
			int alt_si[] = new int[MAX_BRANCH];
			int alt_pi[] = new int[MAX_BRANCH];
			int na = 0;
			for (int pi = 0; pi < pat.size(); ) {
				Element e = pat.get(pi);
System.out.println(pi + ": " + e + " s: '" + s.substring(si) + "'");
				if (e instanceof Branch) {
					Branch b = (Branch) e;
					alt_pi[na] = pi + b.alt;
					alt_si[na] = si;
					++na;
					pi += b.main;
				} else if (e instanceof Jump) {
					pi += ((Jump) e).offset;
				} else {
int start = si;
				si = e.omatch(s, si, pat, pi);
				if (si >= 0)
{ System.out.println("matched '" + s.substring(start, si) + "'");
					++pi; }
				else if (na > 0) {
					// backtrack
					--na;
					si = alt_si[na];
					pi = alt_pi[na];
System.out.println("failed - taking alternate branch");
				} else
{ System.out.println("FAILED");
					return -1; }
				}
			}
System.out.println("MATCHED '" + s.substring(begin, si) + "'");
			return si;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (Element e : pat)
				sb.append(e.toString()).append(" ");
			return sb.toString();
		}

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
	static class CharClass extends Element {
		private final CharMatcher cm;

		CharClass(CharMatcher cm) {
			this.cm = cm.precomputed();
		}

		@Override
		int omatch(String s, int si) {
			return si < s.length() && cm.matches(s.charAt(si)) ? si + 1 : FAIL;
		}

		@Override
		public String toString() {
			return cm.toString();
		}
	}

	final static Element any = new CharClass(CharMatcher.noneOf("\r\n"));

	/**
	 * Implemented by amatch.
	 * Branch tries to jump to main first
	 * setting up alternate to jump to alt.
	 * main and alt are relative offsets
	 */
	@Immutable
	static class Branch extends Element {
		int main;
		int alt;

		Branch(int main, int alt) {
			this.main = main;
			this.alt = alt;
		}

		@Override
		public String toString() {
			return "Branch(" + main + ", " + alt + ")";
		}
	}

	/**
	 * Implemented by amatch.
	 */
	@Immutable
	static class Jump extends Element {
		int offset;

		Jump(int offset) {
			this.offset = offset;
		}

		@Override
		public String toString() {
			return "Jump(" + offset + ")";
		}
	}

	// end of element classes --------------------------------------------------

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
			int start = pat.size();
			sequence();
			if (match("|")) {
				int len = pat.size() - start;
				insert(start, new Branch(1, len + 2));
				while (true) {
					start = pat.size();
					sequence();
					len = pat.size() - start;
					if (match("|")) {
						insert(start, new Branch(1, len + 2));
						insert(start, new Jump(len + 2));
					} else
						break;
				}
				insert(start, new Jump(len + 1));
			}
		}

		void sequence() {
			while (si < sn && nextNot1of("|)"))
				element();
		}

		void element() {
			if (match("^"))
				emit(startOfLine);
			else if (match("$"))
				emit(endOfLine);
			else {
				int start = pat.size();
				simple();
				int len = pat.size() - start;
				if (match("??"))
					insert(start, new Branch(len + 1, 1));
				else if (match("?"))
					insert(start, new Branch(1, len + 1));
				else if (match("+?"))
					emit(new Branch(1, -len));
				else if (match("+"))
					emit(new Branch(-len, 1));
				else if (match("*?")) {
					emit(new Branch(1, -len));
					insert(start, new Branch(len + 2, 1));
				} else if (match("*")) {
					emit(new Branch(-len, 1));
					insert(start, new Branch(1, len + 2));
				}
			}
		}

		private static final CharMatcher CM_WORD =
				CharMatcher.JAVA_LETTER_OR_DIGIT.or(CharMatcher.is('_'));
		private static final CharMatcher CM_NOTWORD = CM_WORD.negate();

		void simple() {
			if (match("."))
				emit(any);
			else if (match("\\d"))
				emit(new CharClass(CharMatcher.DIGIT));
			else if (match("\\D"))
				emit(new CharClass(CharMatcher.DIGIT.negate()));
			else if (match("\\s"))
				emit(new CharClass(CharMatcher.WHITESPACE));
			else if (match("\\S"))
				emit(new CharClass(CharMatcher.WHITESPACE.negate()));
			else if (match("\\w"))
				emit(new CharClass(CM_WORD));
			else if (match("\\W"))
				emit(new CharClass(CM_NOTWORD));
			else if (match("[")) {
				charClass();
				match("]");
			} else if (match("(")) {
				regex();
				match(")");
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

		void charClass() {
			if (src.charAt(si) != '^' &&
					si + 1 < sn && src.charAt(si + 1) == ']') { // e.g. [.]
				++si;
				emit(new Chars(src.substring(si - 1, si)));
				return;
			}
			boolean negate = match("^");
			CharMatcher cm = CharMatcher.NONE;
			while (si < sn && src.charAt(si) != ']') {
				if (match("\\d"))
					cm = cm.or(CharMatcher.DIGIT);
				else if (match("\\D"))
					cm = cm.or(CharMatcher.DIGIT.negate());
				else if (match("\\s"))
					cm = cm.or(CharMatcher.WHITESPACE);
				else if (match("\\S"))
					cm = cm.or(CharMatcher.WHITESPACE.negate());
				else if (match("\\w"))
					cm = cm.or(CM_WORD);
				else if (match("\\W"))
					cm = cm.or(CM_NOTWORD);
			}
			if (negate)
				cm = cm.negate();
			emit(new CharClass(cm));
		}

		boolean match(String s) {
			if (src.startsWith(s, si)) {
				si += s.length();
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
		Regex.compile("a|b|c").amatch("c");
	}

}
