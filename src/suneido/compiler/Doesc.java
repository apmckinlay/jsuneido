/* Copyright 2015 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

// The rather odd design is so that it can be used by Lexer
public class Doesc {
	public interface Src {
		default char charAt() {
			return at(0);
		}
		char at(int d);
		void move(int d);
	}

	private static class StrSrc implements Src {
		String s;
		int i = 0;

		StrSrc(String s) {
			this.s = s;
		}

		@Override
		public char at(int d) {
			return s.charAt(i + d);
		}

		@Override
		public void move(int d) {
			i += d;
		}

		String doesc() {
			StringBuilder sb = new StringBuilder(s.length());
			for (; i < s.length(); ++i) {
				char c = s.charAt(i);
				if (c == '\\')
					sb.append(Doesc.doesc(this));
				else
					sb.append(c);
			}
			return sb.toString();
		}
	}

	public static String doesc(String s) {
		return new StrSrc(s).doesc();
	}

	public static char doesc(Src src) {
		assert src.at(0) == '\\';
		src.move(1); // backslash
		int dig1, dig2;
		switch (src.charAt()) {
		case '0' :
			return 0;
		case 'n' :
			return '\n';
		case 't' :
			return '\t';
		case 'r' :
			return '\r';
		case 'x' :
			if (-1 != (dig1 = Character.digit(src.at(1), 16)) &&
					-1 != (dig2 = Character.digit(src.at(2), 16))) {
				src.move(2);
				return (char) (16 * dig1 + dig2);
			} else {
				src.move(-1);
				return src.charAt();
			}
		case '\\' :
		case '"' :
		case '\'' :
			return src.charAt();
		default :
			src.move(-1);
			return src.charAt();
		}
	}
}
