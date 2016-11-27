/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.CharMatcher;
import com.google.common.primitives.Ints;

import gnu.trove.list.array.TIntArrayList;

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
 * - case is ASCII only i.e. a-z, A-Z, not unicode
 * - compile Chars to lower case, match has to convert to lower case
 * - CharClass tries matching both toupper and tolower (to handle ranges)
 * - Backref converts both to lower
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
		private static final int MAX_RESULTS = 10;
		private final int[] tmp = new int[MAX_RESULTS];
		public final int[] pos = new int[MAX_RESULTS];
		public final int[] end = new int[MAX_RESULTS];

		public int groupCount() {
			int n = Ints.indexOf(end, -1);
			return n == -1 ? 9 : n - 1;
		}

		public String group(String s, int i) {
			assert 0 <= i && i < MAX_RESULTS;
			return end[i] == -1 ? "" : s.substring(pos[i], end[i]);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < MAX_RESULTS; i++)
				sb.append("(").append(pos[i]).append(", ").append(end[i]).append(") ");
			return sb.toString();
		}
	}

	public interface ForEach {
		/** @return The index to continue searching at. */
		public int each(Result result);
	}

	@Immutable
	public static class Pattern {
		private static final int ALT_INIT_CAPACITY = 100;
		private final List<Element> pat;

		private Pattern(List<Element> pat) {
			this.pat = pat;
		}

		/**
		 * Find the first match in the string at or after pos.
		 * @return Result if a match is found, else null
		 */
		public Result firstMatch(String s, int pos) {
			// allocate these once per match instead of once per amatch
			Result result = new Result();
			TIntArrayList alt_si = new TIntArrayList(ALT_INIT_CAPACITY);
			TIntArrayList alt_pi = new TIntArrayList(ALT_INIT_CAPACITY);
			int sn = s.length();
			assert 0 <= pos && pos <= sn;
			Element e = pat.get(1); // skip LEFT0
			for (int si = pos; si <= sn; si = e.nextPossible(s, si, sn))
				if (null != amatch(s, si, result, alt_si, alt_pi))
					return result;
			return null;
		}

		/**
		 * Find the last match in the string before pos.
		 * Does not use the nextPossible optimization so may be slower;
		 * @return Result if a match is found, else null
		 */
		public Result lastMatch(String s, int pos) {
			Result result = new Result();
			TIntArrayList alt_si = new TIntArrayList(ALT_INIT_CAPACITY);
			TIntArrayList alt_pi = new TIntArrayList(ALT_INIT_CAPACITY);
			int sn = s.length();
			assert 0 <= pos && pos <= sn;
			for (int si = pos; si >= 0; si--)
				if (null != amatch(s, si, result, alt_si, alt_pi))
					return result;
			return null;
		}

		/**
		 * Calls action for each match in the string.
		 */
		public void forEachMatch(String s, ForEach action) {
			Result result = new Result();
			TIntArrayList alt_si = new TIntArrayList(ALT_INIT_CAPACITY);
			TIntArrayList alt_pi = new TIntArrayList(ALT_INIT_CAPACITY);
			int sn = s.length();
			Element e = pat.get(1); // skip LEFT0
			for (int si = 0; si <= sn; si = e.nextPossible(s, si, sn))
				if (null != amatch(s, si, result, alt_si, alt_pi)) {
					int si2 = action.each(result);
					assert si2 > si;
					si = si2 - 1;
					// -1 since nextPossible will at least increment
				}
		}

		/**
		 * Try to match at a specific position.
		 * @return Result[] if it matches, else null
		 */
		public Result amatch(String s, int si) {
			return amatch(s, si, new Result(),
					new TIntArrayList(ALT_INIT_CAPACITY),
					new TIntArrayList(ALT_INIT_CAPACITY));
		}

		private Result amatch(String s, int si, Result result,
				TIntArrayList alt_si, TIntArrayList alt_pi) {
			Arrays.fill(result.end, -1);
			alt_si.resetQuick(); // need to reset since these are reused
			alt_pi.resetQuick();
			for (int pi = 0; pi < pat.size(); ) {
				Element e = pat.get(pi);
				if (e instanceof Branch) {
					Branch b = (Branch) e;
					alt_pi.add(pi + b.alt);
					alt_si.add(si);
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
					else {
						int na = alt_si.size();
						if (na > 0) {
							// backtrack
							si = alt_si.removeAt(na - 1);
							pi = alt_pi.removeAt(na - 1);
						} else
							return null;
					}
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

	static final CharMatcher blank = CharMatcher.anyOf(" \t");
	static final CharMatcher digit = CharMatcher.anyOf("0123456789");
	static final CharMatcher notDigit = digit.negate();
	static final CharMatcher lower = CharMatcher.inRange('a', 'z');
	static final CharMatcher upper = CharMatcher.inRange('A', 'Z');
	static final CharMatcher alpha = lower.or(upper);
	static final CharMatcher alnum = digit.or(alpha);
	static final CharMatcher punct =
			CharMatcher.anyOf("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~");
	static final CharMatcher graph = alnum.or(punct);
	static final CharMatcher print = graph.or(CharMatcher.is(' '));
	static final CharMatcher xdigit =
			CharMatcher.anyOf("0123456789abcdefABCDEF");
	static final CharMatcher space = CharMatcher.anyOf(" \t\r\n");
	static final CharMatcher notSpace = space.negate();
	static final CharMatcher cntrl = CharMatcher.inRange('\u0000', '\u001f')
				.or(CharMatcher.inRange('\u007f', '\u009f'));
	static final CharMatcher word = alnum.or(CharMatcher.is('_'));
	static final CharMatcher notWord = word.negate();

	private static class Compiler {
		String src;
		int si = 0;
		int sn;
		ArrayList<Element> pat = new ArrayList<>();
		boolean ignoringCase = false;
		int leftCount = 0;
		boolean inChars = false;
		boolean inCharsIgnoringCase = false;

		Compiler(String src) {
			this.src = src;
			sn = src.length();
		}

		Pattern compile() {
			emit(LEFT0);
			regex();
			emit(RIGHT0);
			if (si < sn)
				throw new RuntimeException("regex: closing ) without opening (");
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
				if (match("??")) {
					insert(start, new Branch(len + 1, 1));
				} else if (match("?")) {
					insert(start, new Branch(1, len + 1));
				} else if (match("+?"))
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
			si = src.indexOf("(?-q)", si);
			if (si == -1)
				si = sn;
			emitChars(src.substring(start, si));
		}

		void simple() {
			if (match("."))
				emit(any);
			else if (match("\\d"))
				emit(new CharClass(digit));
			else if (match("\\D"))
				emit(new CharClass(notDigit));
			else if (match("\\w"))
				emit(new CharClass(word));
			else if (match("\\W"))
				emit(new CharClass(notWord));
			else if (match("\\s"))
				emit(new CharClass(space));
			else if (match("\\S"))
				emit(new CharClass(notSpace));
			else if (matchBackref()) {
				int i = src.charAt(si - 1) - '0';
				emit(new Backref(i, ignoringCase));
			} else if (match("[")) {
				charClass();
				mustMatch("]");
			} else if (match("(")) {
				int i = ++leftCount;
				emit(new Left(i));
				regex();					// recurse
				emit(new Right(i));
				mustMatch(")");
			} else {
				if (si + 1 < sn)
					match("\\");
				++si;
				emitChars(src.substring(si - 1, si));
			}
		}

		private void emitChars(String s) {
			if (inChars && inCharsIgnoringCase == ignoringCase &&
					! next1of("?*+")) {
				((Chars) pat.get(pat.size() - 1)).add(s);
			} else {
				emit(ignoringCase ? new CharsIgnoreCase(s) : new Chars(s));
				inChars = true;
				inCharsIgnoringCase = ignoringCase;
			}
		}

		boolean next1of(String set) {
			return si < sn && set.indexOf(src.charAt(si)) != -1;
		}

		void charClass() {
			boolean negate = match("^");
			StringBuilder chars = new StringBuilder();
			if (match("]"))
				chars.append(']');
			CharMatcher cm = CharMatcher.none();
			while (si < sn && src.charAt(si) != ']') {
				CharMatcher elem;
				if (matchRange()) {
					char from = src.charAt(si - 3);
					char to = src.charAt(si - 1);
					elem = (from <= to)
							? CharMatcher.inRange(from, to)
							: CharMatcher.none();
				} else if (match("\\d"))
					elem = digit;
				else if (match("\\D"))
					elem = notDigit;
				else if (match("\\w"))
					elem = word;
				else if (match("\\W"))
					elem = notWord;
				else if (match("\\s"))
					elem = space;
				else if (match("\\S"))
					elem = notSpace;
				else if (match("[:"))
					elem = posixClass();
				else {
					if (si + 1 < sn)
						match("\\");
					chars.append(src.charAt(si++));
					continue;
				}
				cm = cm.or(elem);
			}
			if (! negate && cm == CharMatcher.none() && chars.length() == 1) {
				// optimization for class with only one character
				emitChars(chars.toString());
				return;
			}
			if (chars.length() > 0)
				cm = cm.or(CharMatcher.anyOf(chars.toString()));
			if (negate)
				cm = cm.negate();
			cm = cm.precomputed(); // ???
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

		CharMatcher posixClass() {
			if (match("alpha:]"))
				return alpha;
			else if (match("alnum:]"))
				return alnum;
			else if (match("blank:]"))
				return blank;
			else if (match("cntrl:]"))
				return cntrl;
			else if (match("digit:]"))
				return digit;
			else if (match("graph:]"))
				return graph;
			else if (match("lower:]"))
				return lower;
			else if (match("print:]"))
				return print;
			else if (match("punct:]"))
				return punct;
			else if (match("space:]"))
				return space;
			else if (match("upper:]"))
				return upper;
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

		void mustMatch(String s) {
			if (! match(s))
				throw new RuntimeException("regex: missing '" + s + "'");
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
			inChars = false;
		}

		void insert(int i, Element e) {
			pat.add(i, e);
			inChars = false;
		}

	}

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
			int j = s.indexOf('\n', si);
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
			return (si == 0 || ! word.matches(s.charAt(si - 1))) ? si : FAIL;
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
			return (si >= s.length() || ! word.matches(s.charAt(si)))
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

	// immutable after construction
	static class Chars extends Element {
		protected String chars;

		Chars(String chars) {
			this.chars = chars;
		}

		public void add(String s) {
			chars += s;
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
	// extend Chars so compile (simple) can treat them the same
	static class CharsIgnoreCase extends Chars {

		CharsIgnoreCase(String chars) {
			super(chars.toLowerCase());
		}

		@Override
		public void add(String s) {
			chars += s.toLowerCase();
		}

		@Override
		public int omatch(String s, int si) {
			int len = chars.length();
			if (si + len > s.length())
				return FAIL;
			for (int i = 0; i < len; ++i)
				if (Character.toLowerCase(s.charAt(si + i)) != chars.charAt(i))
					return FAIL;
			return si + chars.length();
		}

		@Override
		public int nextPossible(String s, int si, int sn) {
			int len = chars.length();
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
		public int nextPossible(String s, int si, int sn) {
			if (si >= sn)
				return si + 1;
			int j = cm.indexIn(s, si + 1);
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
			return matches(s.charAt(si)) ? si + 1 : FAIL;
		}

		@Override
		public int nextPossible(String s, int si, int sn) {
			for (++si; si < sn; ++si)
				if (matches(s.charAt(si)))
					return si;
			return sn + 1; // no possible match
		}

		private boolean matches(char c) {
			return cm.matches(Character.toLowerCase(c)) ||
					cm.matches(Character.toUpperCase(c));
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
	final static Left LEFT0 = new Left(0);

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
	final static Right RIGHT0 = new Right(0);
}

