package suneido.runtime.builtin;

import static suneido.runtime.builtin.StringMethods.Windows1252;

import java.nio.charset.Charset;

import suneido.runtime.Ops;
import suneido.runtime.Params;
import suneido.util.Util;

public class WideChar {

	static final Charset Utf8 = Charset.forName("UTF-8");

	@Params("string, cp = 0")
	public static Object WideCharToMultiByte(Object a, Object b) {
		String s = Ops.toStr(a); // pairs of chars
		if (s.length() == 0)
			return s;
		assert s.length() % 2 == 0;
		StringBuilder sb = new StringBuilder(s.length() / 2);
		for (int i = 0; i < s.length(); i += 2)
			sb.append((char) (s.charAt(i) + (s.charAt(i+1) << 8)));
		if (sb.charAt(sb.length() - 1) == 0)
			sb.setLength(sb.length() - 1);
		String t = sb.toString();
		byte[] bytes = t.getBytes(cp(b));
		return Util.bytesToString(bytes);
	}

	@Params("string, cp = 0")
	public static Object MultiByteToWideChar(Object a, Object b) {
		String s = Ops.toStr(a);
		byte[] bytes = Util.stringToBytes(s);
		String t = new String(bytes, cp(b));
		StringBuilder sb = new StringBuilder(t.length() * 2 + 2);
		for (int i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			sb.append((char) (c & 255)).append((char) (c >>> 8));
		}
		sb.append((char) 0).append((char) 0); // nul terminated
		return sb.toString();
	}

	private static Charset cp(Object b) {
		Charset cs;
		int cp = Ops.toInt(b);
		if (cp == 0 /*CP_ACP*/ || cp == 3 /*CP_THREAD_ACP*/ || cp == 1252)
			cs = Windows1252;
		else if (cp == 65001)
			cs = Utf8;
		else
			throw new RuntimeException("invalid code page");
		return cs;
	}

}
