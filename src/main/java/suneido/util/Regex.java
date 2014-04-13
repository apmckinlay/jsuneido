/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.CharMatcher;

/*
 * regular expression grammar and compiled form:
 *
 *	regex	:	sequence				LEFT0 ... RIGHT0
 *			|	sequence (| sequence)+	Branch sequence (Jump Branch sequence)+
 *
 *	sequence	:	element+
 *
 *	element		:	^					startOfLine
 *				|	$					endOfLine
 *				|	\A					startOfString
 *				|	\Z					endOfString
 *				|	(?i)				(only affects compile)
 *				|	(?-i)				(only affects compile)
 *				|	(?q)				(only affects compile)
 *				|	(?-q)				(only affects compile)
 *				|	\<					startOfWord
 *				|	\>					endOfWord
 *				|	\#					Backref(#)
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
 *				|	( regex )			Left(i) ... Right(i)
 *				|	chars				Chars(string) // multiple characters
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
 * - also handled by Backref
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

	public static class Result {
		private final int[] tmp;
		public int[] pos;
		public int[] end;

		Result() {
			tmp = new int[10];
			pos = new int[10];
			end = new int[10];
		}
	}

	@Immutable
	static class Pattern {
		final int MAX_BRANCH = 1000;
		private final List<Element> pat;

		private Pattern(List<Element> pat) {
			this.pat = pat;
		}

		/**
		 * look for a match at any position
		 * @return Result[] if a match is found, else null
		 */
		public Result match(String s) {
			// allocate these once per match instead of once per amatch
			Result result = new Result();
			int alt_si[] = new int[MAX_BRANCH];
			int alt_pi[] = new int[MAX_BRANCH];
			int si = 0;
			int sn = s.length();
			Element e = pat.get(1); // skip LEFT0
			while (si <= sn) {
				if (null != amatch(s, si, result, alt_si, alt_pi))
					return result;
				si = e.nextPossible(s, si, sn);
			}
			return null;
		}

		/**
		 * Try to match at a specific position.
		 * @return Result[] if it matches, else null
		 */
		public Result amatch(String s, int si) {
			return amatch(s, si, new Result(),
					new int[MAX_BRANCH], new int[MAX_BRANCH]);
		}

		private Result amatch(String s, int si, Result result,
				int[] alt_si, int[] alt_pi) {
			Arrays.fill(result.end, -1);
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
				} else if (e instanceof Left) {
					int i = ((Left) e).idx;
					if (i < result.tmp.length)
						result.tmp[i] = si;
					++pi;
				} else if (e instanceof Right) {
					int i = ((Right) e).idx;
					if (i < result.pos.length) {
						result.pos[i] = result.tmp[i];
						result.end[i] = si;
					}
					++pi;
				} else {
					si = e.omatch(s, si, result);
					if (si >= 0)
						++pi;
					else if (na > 0) {
						// backtrack
						--na;
						si = alt_si[na];
						pi = alt_pi[na];
					} else
						return null;
				}
			}
			return result;
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
		int left_count = 0;

		Compiler(String src) {
			this.src = src;
			sn = src.length();
		}

		Pattern compile() {
			emit(LEFT0);
			regex();
			emit(RIGHT0);
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
			while (si < sn && -1 == "|)".indexOf(src.charAt(si)))
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
			else if (match("\\<"))
				emit(startOfWord);
			else if (match("\\>"))
				emit(endOfWord);
			else if (match("(?i)")) {
				ignoringCase = true;
			} else if (match("(?-i)")) {
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
			String s = src.substring(start, si);
			emit(ignoringCase ? new CharsIgnoreCase(s) : new Chars(s));
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
			else if (matchBackref()) {
				int i = src.charAt(si - 1) - '0';
				emit(new Backref(i, ignoringCase));
			} else if (match("[")) {
				charClass();
				match("]");
			} else if (match("(")) {
				int i = ++left_count;
				emit(new Left(i));
				regex();
				emit(new Right(i));
				match(")");
			} else {
				int prevStart = -1;
				StringBuilder sb = new StringBuilder();
				while (si < sn) {
					int start = si;
					char c = src.charAt(si);
					if (c == '\\') {
						if (-1 != "AZdDwWsS123456789<>".indexOf(src.charAt(si + 1)))
							break;
						else
							c = src.charAt(++si); // skip backslash
					} else if (-1 != "^$.|()[".indexOf(c))
						break;
					else if (-1 != "?+*".indexOf(c) && sb.length() > 0) {
						if (sb.length() > 1) {
							sb.deleteCharAt(sb.length() - 1);
							si = prevStart;
						}
						break;
					}
					sb.append(c);
					++si;
					prevStart = start;
				}
				if (sb.length() > 0)
					emitChars(sb.toString());
			}
		}

		private void emitChars(String s) {
			emit(ignoringCase ? new CharsIgnoreCase(s) : new Chars(s));
		}

		// if ignoring case character classes are built to match lower case

		void charClass() {
			if (src.charAt(si) != '^' &&
					si + 1 < sn && src.charAt(si + 1) == ']') { // e.g. [.]
				++si;
				String s = src.substring(si - 1, si);
				emit(ignoringCase ? new CharsIgnoreCase(s) : new Chars(s));
				return;
			}
			boolean negate = match("^");
			CharMatcher cm = CharMatcher.NONE;
			while (si < sn && src.charAt(si) != ']') {
				CharMatcher elem;
				if (matchRange()) {
					elem = range(src.charAt(si - 3), src.charAt(si - 1));
				} else if (match("\\d"))
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
					elem = CharMatcher.is(src.charAt(si++));
				cm = cm.or(elem);

			}
			if (negate)
				cm = cm.negate();
			emit(ignoringCase ? new CharClassIgnoreCase(cm) : new CharClass(cm));
		}

		private boolean matchRange() {
			if (src.charAt(si + 1) == '-' &&
					si+2 < sn && src.charAt(si + 2) != ']') {
				si += 3;
				return true;
			} else
				return false;
		}

		private CharMatcher range(char from, char to) {
			if (ignoringCase) {
				// WARNING: not guaranteed to work for non-ascii
				char lofrom = Character.toLowerCase(from);
				char loto = Character.toLowerCase(to);
				if ((from == lofrom) && (to != loto))
					// e.g. 0-m split to 0-(a-1), a-z
					return CharMatcher.inRange(from, (char) ('A' - 1)).or(
							CharMatcher.inRange('a', loto));
				else if ((from != lofrom) && (to == loto))
					// e.g. m-~ split to m-z, (z+1)-~
					return CharMatcher.inRange(lofrom, 'z').or(
							CharMatcher.inRange((char) ('Z' + 1), to));
				else // both letters
					return CharMatcher.inRange(lofrom, loto);
			} else
				return CharMatcher.inRange(from, to);
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
				return ignoringCase
						? CharMatcher.JAVA_LOWER_CASE : CharMatcher.JAVA_UPPER_CASE;
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

		boolean matchBackref() {
			if (si + 2 > sn || src.charAt(si) != '\\')
				return false;
			char c = src.charAt(si + 1);
			if (c < '1' || '9' < c)
				return false;
			si += 2;
			return true;
		}

		void emit(Element e) {
			pat.add(e);
		}

		void insert(int i, Element e) {
			pat.add(i, e);
		}

	}

	private static final CharMatcher CM_WORD =
			CharMatcher.JAVA_LETTER_OR_DIGIT.or(CharMatcher.is('_'));

	// elements of compiled regex ----------------------------------------------

	private final static int FAIL = Integer.MIN_VALUE;

	abstract static class Element {
		/** @return FAIL or the position after the match */
		int omatch(String s, int si, Result res) {
			return omatch(s, si);
		}
		int omatch(String s, int si) {
			throw new RuntimeException("must be overridden");
		}
		/** nextPossible is an optional optimization */
		public int nextPossible(String s, int si, int sn) {
			return si + 1;
		}
	}

	@Immutable
	static class StartOfLine extends Element {

		@Override
		public int omatch(String s, int si) {
			return (si == 0 || s.charAt(si - 1) == '\r' ||
					s.charAt(si - 1) == '\n') ? si : FAIL;
		}

		@Override
		public int nextPossible(String s, int si, int sn) {
			if (si == sn)
				return si + 1;
			int j = s.indexOf('\n', si + 1);
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
			return (si >= s.length() || s.charAt(si) == '\r' ||
					s.charAt(si) == '\n') ? si : FAIL;
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
		public int nextPossible(String s, int si, int sn) {
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
	static class StartOfWord extends Element {
		@Override
		public int omatch(String s, int si) {
			return (si == 0 || ! CM_WORD.matches(s.charAt(si - 1))) ? si : FAIL;
		}
		@Override
		public String toString() {
			return "\\<";
		}
	}
	final static Element startOfWord = new StartOfWord();

	@Immutable
	static class EndOfWord extends Element {
		@Override
		public int omatch(String s, int si) {
			return (si >= s.length() || ! CM_WORD.matches(s.charAt(si)))
					? si : FAIL;
		}
		@Override
		public String toString() {
			return "\\>";
		}
	}
	final static Element endOfWord = new EndOfWord();

	@Immutable
	static class Backref extends Element {
		private final int idx;
		private final boolean ignoringCase;

		public Backref(int idx, boolean ignoringCase) {
			this.idx = idx;
			assert 1 <= idx && idx <= 9;
			this.ignoringCase = ignoringCase;
		}
		@Override
		public int omatch(String s, int si, Result res) {
			if (res.end[idx] == -1)
				return FAIL;
			String b = s.substring(res.pos[idx], res.end[idx]);
			if (ignoringCase) {
				int len = b.length();
				if (si + len > s.length())
					return FAIL;
				for (int i = 0; i < len; ++i)
					if (Character.toLowerCase(s.charAt(si + i)) !=
								Character.toLowerCase(b.charAt(i)))
						return FAIL;
			} else if (! s.startsWith(b, si))
				return FAIL;
			return si + b.length();
		}
		@Override
		public String toString() {
			return (ignoringCase ? "i" : "") +
					"\\" + Character.toString((char) ('0' + idx));
		}
	}

	@Immutable
	static class Chars extends Element {
		private final String chars;

		Chars(String chars) {
			this.chars = chars;
		}

		@Override
		public int omatch(String s, int si) {
			if (! s.startsWith(chars, si))
				return FAIL;
			return si + chars.length();
		}

		@Override
		public int nextPossible(String s, int si, int sn) {
			int j = s.indexOf(chars, si + 1);
			return j == -1 ? sn + 1 : j;
		}

		@Override
		public String toString() {
			return "'" + chars + "'";
		}
	}

	@Immutable
	static class CharsIgnoreCase extends Element {
		private final String chars;
		private final int len;

		CharsIgnoreCase(String chars) {
			this.chars = chars.toLowerCase();
			len = chars.length();
		}

		@Override
		public int omatch(String s, int si) {
			if (si + len > s.length())
				return FAIL;
			for (int i = 0; i < len; ++i)
				if (Character.toLowerCase(s.charAt(si + i)) != chars.charAt(i))
					return FAIL;
			return si + chars.length();
		}

		@Override
		public int nextPossible(String s, int si, int sn) {
			for (++si; si <= sn - len; ++si)
				for (int i = 0; ; ++i)
					if (i == len)
						return si;
					else if (Character.toLowerCase(s.charAt(si + i)) != chars.charAt(i))
						break;
			return sn + 1; // no possible match
		}

		@Override
		public String toString() {
			return "i'" + chars + "'";
		}
	}

	@Immutable
	static class CharClass extends Element {
		private final CharMatcher cm;

		CharClass(CharMatcher cm) {
			this.cm = cm;
		}

		@Override
		int omatch(String s, int si) {
			if (si >= s.length())
				return FAIL;
			return cm.matches(s.charAt(si)) ? si + 1 : FAIL;
		}

		@Override
		public int nextPossible(String s, int i, int sn) {
			int j = cm.indexIn(s, i + 1);
			return j == -1 ? sn + 1 : j;
		}

		@Override
		public String toString() {
			return cm.toString();
		}
	}

	@Immutable
	static class CharClassIgnoreCase extends Element {
		private final CharMatcher cm;

		CharClassIgnoreCase(CharMatcher cm) {
			this.cm = cm;
		}

		@Override
		int omatch(String s, int si) {
			if (si >= s.length())
				return FAIL;
			char c = s.charAt(si);
			return cm.matches(Character.toLowerCase(c)) ? si + 1 : FAIL;
		}

		@Override
		public int nextPossible(String s, int si, int sn) {
			for (++si; si < sn; ++si)
				if (cm.matches(Character.toLowerCase(s.charAt(si))))
					return si;
			return sn + 1; // no possible match
		}

		@Override
		public String toString() {
			return "i" + cm.toString();
		}
	}

	final static Element any = new CharClass(CharMatcher.noneOf("\r\n")) {
		@Override
		public String toString() { return "."; }
	};

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
	static class Left extends Element {
		int idx;

		Left(int idx) {
			this.idx = idx;
		}

		@Override
		public String toString() {
			return idx == 0 ? "" : "Left" + idx;
		}
	}

	/** Implemented by amatch. */
	@Immutable
	static class Right extends Element {
		int idx;

		Right(int idx) {
			this.idx = idx;
		}

		@Override
		public String toString() {
			return idx == 0 ? "" : "Right" + idx;
		}
	}

	final static Left LEFT0 = new Left(0);
	final static Right RIGHT0 = new Right(0);

}
