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
 *				|	(?q)				(only affects compile)
 *				|	(?-q)				(only affects compile)
 *				|	\<					START_WORD					TODO
 *				|	\>					END_WORD					TODO
 *				|	\#					PIECE #						TODO
 *				|	simple
 *				|	simple ?			Branch simple
 *				|	simple +			simple Branch
 *				|	simple *			Branch simple Branch
 *				|	simple ??			Branch simple
 *				|	simple +?			simple Branch
 *				|	simple *?			Branch simple Branch
 *
 *	simple		:	.					any
 *				|	[ charmatch+ ]		CharClass
 *				|	[^ charmatch+ ]		CharClass
 *				|	shortcut			CharClass
 *				|	( regex )			LEFT i ... RIGHT i
 *				|	chars				Char(string) // multiple characters
 *
 *	charmatch	:	shortcut			CharClass
 *				|	posix				CharClass
 *				|	char - char			CharClass
 *				|	char				CharClass
 *
 *	shortcut	:	\d					CharClass
 *				|	\D					CharClass
 *				|	\w					CharClass
 *				|	\W					CharClass
 *				|	\s					CharClass
 *				|	\S					CharClass
 *
 *	posix		|	[:alnum:]			CharClass
 *				|	[:alpha:]			CharClass
 *				|	[:blank:]			CharClass
 *				|	[:cntrl:]			CharClass
 *				|	[:digit:]			CharClass
 *				|	[:graph:]			CharClass
 *				|	[:lower:]			CharClass
 *				|	[:print:]			CharClass
 *				|	[:punct:]			CharClass
 *				|	[:space:]			CharClass
 *				|	[:upper:]			CharClass
 *				|	[:xdigit:]			CharClass
 *
 * handling ignore case:
 * - compile Chars and CharClass to lower case
 * - match has to convert to lower case
 * NOTE: assumes that ignore case state is in sync between compile and match
 * this won't be the case for e.g. (abc(?i)def)+
 * 
 * Element.nextPossible is used to optimize match
 * if amatch fails at a certain position
 * nextPossible skips ahead
 * so it doesn't just try amatch at every position
 * This makes match almost as fast as indexOf or contains
 */
public class Regex {

	public static Pattern compile(String rx) {
		return new Compiler(rx).compile();
	}

	@Immutable
	static class Pattern {
		private final List<Element> pat;

		private Pattern(List<Element> pat) {
			this.pat = pat;
		}

		public boolean match(String s) {
			return match(s, 0);
		}

		public boolean match(String s, int i) {
			// TODO search backwards
			int sn = s.length();
			Element e = pat.get(0);
			while (i <= sn) {
				if (-1 != amatch(s, i))
					return true;
				i = e.nextPossible(s, sn, i);
			}
			return false;
		}

		public int amatch(String s) {
			return amatch(s, 0);
		}

		/**
		 * Try to match at a specific position.
		 * @return The position after the match, or -1 if no match
		 */
		public int amatch(String s, int si) {
			final int MAX_BRANCH = 1000;
			int alt_si[] = new int[MAX_BRANCH];
			int alt_pi[] = new int[MAX_BRANCH];
			boolean ignoringCase = false;		// BUT how to get this to elements?
			int na = 0;
			for (int pi = 0; pi < pat.size(); ) {
				Element e = pat.get(pi);
				if (e instanceof Branch) {
					Branch b = (Branch) e;
					alt_pi[na] = pi + b.alt;
					alt_si[na] = si;
					++na;
					pi += b.main;
				} else if (e instanceof Jump) {
					pi += ((Jump) e).offset;
				} else if (e == Regex.ignoreCase || e == Regex.caseSensitive) {
					ignoringCase = (e == Regex.ignoreCase);
					++pi;
				} else {
					si = e.omatch(s, si, pat, pi);
					if (si >= 0)
						++pi;
					else if (na > 0) {
						// backtrack
						--na;
						si = alt_si[na];
						pi = alt_pi[na];
					} else
						return -1;
				}
			}
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

	// compile -----------------------------------------------------------------

	private static class Compiler {
		String src;
		int si = 0;
		int sn;
		ArrayList<Element> pat = new ArrayList<>();
		boolean ignoringCase = false;

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
			else if (match("\\A"))
				emit(startOfString);
			else if (match("\\Z"))
				emit(endOfString);
			else if (match("(?i)")) {
				emit(ignoreCase);
				ignoringCase = true;
			} else if (match("(?-i)")) {
				emit(caseSensitive);
				ignoringCase = false;
			} else if (match("(?q)"))
				quoted();
			else if (match("(?-q)"))
				;
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
		
		void quoted() {
			int start = si;
			si = src.indexOf("(?-q)");
			if (si == -1)
				si = sn;
			emit(new Chars(src.substring(start, si), ignoringCase));
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
			else if (match("\\w"))
				emit(new CharClass(CM_WORD));
			else if (match("\\W"))
				emit(new CharClass(CM_NOTWORD));
			else if (match("\\s"))
				emit(new CharClass(CharMatcher.WHITESPACE));
			else if (match("\\S"))
				emit(new CharClass(CharMatcher.WHITESPACE.negate()));
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
				emit(new Chars(src.substring(start, si), ignoringCase));
			}
		}

		void charClass() {
			if (src.charAt(si) != '^' &&
					si + 1 < sn && src.charAt(si + 1) == ']') { // e.g. [.]
				++si;
				emit(new Chars(src.substring(si - 1, si), ignoringCase));
				return;
			}
			boolean negate = match("^");
			CharMatcher cm = CharMatcher.NONE;
			while (si < sn && src.charAt(si) != ']') {
				CharMatcher elem;
				if (matchRange())
					elem = CharMatcher.inRange(
							handleCase(src.charAt(si - 3)), 
							handleCase(src.charAt(si - 1)));
				else if (match("\\d"))
					elem = digit;
				else if (match("\\D"))
					elem = notdigit;
				else if (match("\\w"))
					elem = CM_WORD;
				else if (match("\\W"))
					elem = CM_NOTWORD;
				else if (match("\\s"))
					elem = CharMatcher.WHITESPACE;
				else if (match("\\S"))
					elem = notwhite;
				else if (match("[:"))
					elem = posixClass();
				else
					elem = CharMatcher.is(handleCase(src.charAt(si++)));
				cm = cm.or(elem);

			}
			if (negate)
				cm = cm.negate();
			emit(new CharClass(cm, ignoringCase));
		}

		private char handleCase(char c) {
			return ignoringCase ? Character.toLowerCase(c) : c;
		}

		private boolean matchRange() {
			if (src.charAt(si + 1) == '-' &&
					si+2 < sn && src.charAt(si + 2) != ']') {
				si += 3;
				return true;
			} else
				return false;
		}

		static final CharMatcher blank = CharMatcher.anyOf(" \t");
		static final CharMatcher digit = CharMatcher.anyOf("0123456789");
		static final CharMatcher notdigit = digit.negate();
		static final CharMatcher alnum = digit.or(CharMatcher.JAVA_LETTER);
		static final CharMatcher punct =
				CharMatcher.anyOf("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~");
		static final CharMatcher graph = alnum.or(punct);
		static final CharMatcher print = graph.or(CharMatcher.is(' '));
		static final CharMatcher xdigit =
				CharMatcher.anyOf("0123456789abcdefABCDEF");
		static final CharMatcher notwhite = CharMatcher.WHITESPACE.negate();

		CharMatcher posixClass() {
			if (match("alpha:]"))
				return CharMatcher.JAVA_LETTER;
			else if (match("alnum:]"))
				return alnum;
			else if (match("blank:]"))
				return blank;
			else if (match("cntrl:]"))
				return CharMatcher.JAVA_ISO_CONTROL;
			else if (match("digit:]"))
				return digit;
			else if (match("graph:]"))
				return graph;
			else if (match("lower:]"))
				return CharMatcher.JAVA_LOWER_CASE;
			else if (match("print:]"))
				return print;
			else if (match("punct:]"))
				return punct;
			else if (match("space:]"))
				return CharMatcher.WHITESPACE;
			else if (match("upper:]"))
				return CharMatcher.JAVA_UPPER_CASE;
			else if (match("xdigit:]"))
				return xdigit;
			else
				throw new RuntimeException("bad posix class");
		}

		// helpers

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
			return "^$.?+*|()[\\".indexOf(c) != -1;
		}

	}

	// elements of compiled regex ----------------------------------------------

	private final static int FAIL = Integer.MIN_VALUE;

	abstract static class Element {
		/** @return FAIL or the position after the match */
		int omatch(String s, int si, List<Element> pat, int pi) {
			return omatch(s, si);
		}
		int omatch(String s, int si) {
			throw new RuntimeException("must be overridden");
		}
		public int nextPossible(String s, int sn, int i) {
			return i + 1;
		}
	}

	@Immutable
	static class StartOfLine extends Element {

		@Override
		public int omatch(String s, int si) {
			return (si == 0 || s.charAt(si - 1) == '\n') ? si : FAIL;
		}
		
		@Override
		public int nextPossible(String s, int sn, int i) {
			if (i == sn)
				return i + 1;
			int j = s.indexOf('\n', i + 1);
			return j == -1 ? sn : j + 1;
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
	static class StartOfString extends Element {

		@Override
		public int omatch(String s, int si) {
			return (si == 0) ? si : FAIL;
		}
		
		@Override
		public int nextPossible(String s, int sn, int i) {
			return sn + 1; // only the initial position is possible
		}

		@Override
		public String toString() {
			return "\\A";
		}
	}
	final static Element startOfString = new StartOfString();

	@Immutable
	static class EndOfString extends Element {

		@Override
		public int omatch(String s, int si) {
			return (si >= s.length()) ? si : FAIL;
		}

		@Override
		public String toString() {
			return "\\Z";
		}
	}
	final static Element endOfString = new EndOfString();

	@Immutable
	static class Chars extends Element {
		private final String chars;
		private final boolean ignoringCase;

		Chars(String chars, boolean ignoringCase) {
			this.chars = ignoringCase ? chars.toLowerCase() : chars;
			this.ignoringCase = ignoringCase;
		}

		@Override
		public int omatch(String s, int si) {
			if (ignoringCase) {
				for (int i = 0; i < chars.length(); ++i)
					if (si + i >= s.length() ||
						Character.toLowerCase(s.charAt(si + i)) != chars.charAt(i))
						return FAIL;
			} else if (! s.startsWith(chars, si))
				return FAIL;
			return si + chars.length();
		}
		
		@Override
		public int nextPossible(String s, int sn, int i) {
			int j = s.indexOf(chars, i + 1);
			return j == -1 ? sn + 1 : j;
		}

		@Override
		public String toString() {
			return "'" + chars + "'";
		}
	}

	@Immutable
	static class CharClass extends Element {
		private final CharMatcher cm;
		private final boolean ignoringCase;

		CharClass(CharMatcher cm) {
			this(cm, false);
		}

		CharClass(CharMatcher cm, boolean ignoringCase) {
			this.cm = cm.precomputed();
			this.ignoringCase = ignoringCase;
		}

		@Override
		int omatch(String s, int si) {
			if (si >= s.length())
				return FAIL;
			char c = s.charAt(si);
			if (ignoringCase)
				c = Character.toLowerCase(c);
			return cm.matches(c) ? si + 1 : FAIL;
		}
		
		@Override
		public int nextPossible(String s, int sn, int i) {
			int j = cm.indexIn(s, i + 1);
			return j == -1 ? sn + 1 : j;
		}

		@Override
		public String toString() {
			return cm.toString();
		}
	}

	final static Element any = new CharClass(CharMatcher.noneOf("\r\n"));

	/**
	 * Implemented by amatch.
	 * Tries to jump to main first
	 * after setting up fallback alternative to jump to alt.
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

	/** Implemented by amatch. */
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

	/** Implemented by amatch. */
	@Immutable
	static class Flag extends Element {
		String name;
		Flag(String name) {
			this.name = name;
		}
		@Override
		public String toString() {
			return name;
		}
	}
	final static Flag ignoreCase = new Flag("(?i)");
	final static Flag caseSensitive = new Flag("(?-i)");

	public static void main(String[] args) {
		Regex.compile("a|b|c").amatch("c");
	}

}
