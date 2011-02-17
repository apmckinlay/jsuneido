package suneido.language;

import static suneido.language.Ops.typeName;
import static suneido.util.Util.bufferToString;
import static suneido.util.Util.putStringToByteBuffer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;

import javax.annotation.concurrent.ThreadSafe;

import suneido.*;
import suneido.util.ByteBuf;

@ThreadSafe // all static methods
public class Pack {
	// sequence must match Order
	public static final class Tag {
		public static final byte FALSE = 0;
		public static final byte TRUE = 1;
		public static final byte MINUS = 2;
		public static final byte PLUS = 3;
		public static final byte STRING = 4;
		public static final byte DATE = 5;
		public static final byte OBJECT = 6;
		public static final byte RECORD = 7;
		public static final byte FUNCTION = 8;
		public static final byte CLASS = 9;
	}

	public static int packSize(Object x) {
		Class<?> xType = x.getClass();
		if (xType == Boolean.class)
			return 1;
		if (xType == Integer.class)
			return packSizeNum((Integer) x, 0);
		if (xType == Long.class)
			return packSizeNum((Long) x, 0);
		if (xType == BigDecimal.class)
			return packSizeBD((BigDecimal) x);
		if (xType == String.class)
			return packSizeString((String) x);
		if (xType == Date.class)
			return packSizeDate((Date) x);

		if (x instanceof Packable)
			return ((Packable) x).packSize(0);
		else
			throw new SuException("can't pack " + typeName(x));
	}

	private static int packSizeDate(Date x) {
		return 9;
	}

	static int packSizeString(String s) {
		int n = s.length();
		return n == 0 ? 0 : 1 + n;
	}

	private static int packshorts(long n) {
		int i = 0;
		for (; n != 0; ++i)
			n /= 10000;
		return i;
	}

	public static void pack(Object x, ByteBuffer buf) {
		assert buf.order() == ByteOrder.BIG_ENDIAN;
		Class<?> xType = x.getClass();
		if (xType == Boolean.class)
			buf.put(x == Boolean.TRUE ? Tag.TRUE : Tag.FALSE);
		else if (xType == Integer.class)
			packNum((Integer) x, 0, buf);
		else if (xType == Long.class)
			packNum((Long) x, 0, buf);
		else if (xType == BigDecimal.class)
			packBD((BigDecimal) x, buf);
		else if (xType == String.class)
			packString((String) x, buf);
		else if (xType == Date.class)
			packDate((Date) x, buf);
		else if (x instanceof Packable)
			((Packable) x).pack(buf);
		else
			throw new SuException("can't pack " + typeName(x));
	}

	private static void packDate(Date x, ByteBuffer buf) {
		buf.put(Tag.DATE);
		Calendar cal = Calendar.getInstance();
		cal.setTime(x);
		int date =
				(cal.get(Calendar.YEAR) << 9)
						| ((cal.get(Calendar.MONTH) + 1) << 5)
						| cal.get(Calendar.DAY_OF_MONTH);
		buf.putInt(date);
		int time =
				(cal.get(Calendar.HOUR_OF_DAY) << 22)
						| (cal.get(Calendar.MINUTE) << 16)
						| (cal.get(Calendar.SECOND) << 10)
						| cal.get(Calendar.MILLISECOND);
		buf.putInt(time);
	}

	private static Date unpackDate(ByteBuffer buf) {
		int date = buf.getInt();
		int time = buf.getInt();

		int year = date >> 9;
		int month = (date >> 5) & 0xf;
		int day = date & 0x1f;

		int hour = time >> 22;
		int minute = (time >> 16) & 0x3f;
		int second = (time >> 10) & 0x3f;
		int millisecond = time & 0x3ff;

		Calendar cal = Calendar.getInstance();
		cal.set(year, month - 1, day, hour, minute, second);
		cal.set(Calendar.MILLISECOND, millisecond);
		return cal.getTime();
	}

	static void packString(String s, ByteBuffer buf) {
		if (s.length() == 0)
			return;
		buf.put(Tag.STRING);
		putStringToByteBuffer(s, buf);
	}

	private static int packSizeBD(BigDecimal n) {
		n = n.stripTrailingZeros();
		return packSizeNum(n.unscaledValue().longValue(), -n.scale());
	}

	public static int packSize(int n) {
		return packSizeNum(n, 0);
	}

	private static int packSizeNum(long n, int e) {
		if (n == 0)
			return 1;
		if (n < 0)
			n = -n;
		for (; (e % 4) != 0; --e)
			n *= 10;
		while (n % 10000 == 0)
			n /= 10000;
		return 2 + 2 * packshorts(n);
	}

	private static void packBD(BigDecimal n, ByteBuffer buf) {
		assert n.precision() <= 15;
		n = n.stripTrailingZeros();
		packNum(n.unscaledValue().longValue(), -n.scale(), buf);
	}

	public static void pack(int n, ByteBuffer buf) {
		packNum(n, 0, buf);
	}

	public static void pack(long n, ByteBuffer buf) {
		packNum(n, 0, buf);
	}

	private static void packNum(long n, int e, ByteBuffer buf) {
		boolean minus = n < 0;
		buf.put(minus ? Tag.MINUS : Tag.PLUS);
		if (n == 0)
			return;
		if (n < 0)
			n = -n;
		for (; (e % 4) != 0; --e)
			n *= 10;
		while (n % 10000 == 0) {
			n /= 10000;
			e += 4;
		}
		e = e / 4 + packshorts(n);
		buf.put(scale(e, minus));
		packLongPart(buf, n, minus);
	}

	private static byte scale(int e, boolean minus) {
		byte eb = (byte) ((e ^ 0x80) & 0xff);
		if (minus)
			eb = (byte) ((~eb) & 0xff);
		return eb;
	}

	private static void packLongPart(ByteBuffer buf, long n, boolean minus) {
		short sh[] = new short[5];
		int i;
		for (i = 0; n != 0; ++i) {
			sh[i] = digit(n % 10000, minus);
			n /= 10000;
		}
		while (--i >= 0)
			buf.putShort(sh[i]);
	}

	private static short digit(long n, boolean minus) {
		return (short) (minus ? ~(n & 0xffff) : n);
	}

	public static int packSize(Object x, int nest) {
		return x instanceof SuContainer ? ((SuContainer) x).packSize(nest)
				: packSize(x);
	}

	public static ByteBuffer pack(Object x) {
		ByteBuffer buf = ByteBuffer.allocate(packSize(x));
		pack(x, buf);
		buf.rewind();
		return buf;
	}

	public static Object unpack(ByteBuf buf) {
		return unpack(buf.getByteBuffer());
	}

	public static Object unpack(ByteBuffer buf) {
		if (buf.remaining() == 0)
			return "";
		assert buf.order() == ByteOrder.BIG_ENDIAN;
		switch (buf.get()) {
		case Tag.FALSE:
			return Boolean.FALSE;
		case Tag.TRUE:
			return Boolean.TRUE;
		case Tag.MINUS:
		case Tag.PLUS:
			return unpackNum(buf);
		case Tag.STRING:
			return unpackString(buf);
		case Tag.OBJECT:
			return SuContainer.unpack(buf);
		case Tag.RECORD:
			return SuRecord.unpack(buf);
		case Tag.DATE:
			return unpackDate(buf);
		case Tag.FUNCTION:
		case Tag.CLASS:
			throw new SuException("jSuneido cannot unpack functions or classes");
		default:
			throw new SuException("invalid unpack type: "
					+ buf.get(buf.position() - 1));
		}
	}

	private static String unpackString(ByteBuffer buf) {
		if (buf.remaining() == 0)
			return "";
		return bufferToString(buf);
	}

	private static Object unpackNum(ByteBuffer buf) {
		if (buf.remaining() == 0)
			return 0;
		boolean minus = buf.get(buf.position() - 1) == Tag.MINUS;
		int s = buf.get() & 0xff;
		if (minus)
			s = ((~s) & 0xff);
		s = (byte) (s ^ 0x80);
		s = -(s - buf.remaining() / 2) * 4;
		long n = unpackLongPart(buf, minus);
		if (n == 0)
			return 0;
		if (-10 <= s && s < 0)
			for (; s < 0; ++s)
				n *= 10;
		if (s == 0 && Integer.MIN_VALUE <= n && n <= Integer.MAX_VALUE)
			return (int) n;
		else if (s == 0)
			return n;
		else {
			BigDecimal x = new BigDecimal(BigInteger.valueOf(n), s);
			if (x.precision() > 15)
				x = x.round(Ops.mc);
			return x;
		}
	}

	private static long unpackLongPart(ByteBuffer buf, boolean minus) {
		long n = 0;
		while (buf.remaining() > 0) {
			short x = buf.getShort();
			if (minus)
				x = (short) ((~x) & 0xffff);
			n = n * 10000 + x;
		}
		return minus ? -n : n;
	}

}
