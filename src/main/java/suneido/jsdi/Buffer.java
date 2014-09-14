/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi;

import static suneido.util.Util.array;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Map;

import suneido.SuInternalError;
import suneido.SuValue;
import suneido.runtime.Args;
import suneido.runtime.BuiltinClass;
import suneido.runtime.BuiltinMethods;
import suneido.runtime.Concats;
import suneido.runtime.FunctionSpec;
import suneido.runtime.Ops;
import suneido.runtime.Pack;
import suneido.runtime.Range;
import suneido.runtime.SuCallable;
import suneido.runtime.builtin.StringMethods;

/**
 * <p>
 * Implements the Suneido {@code Buffer} type, an abitrary sequence of bytes.
 * </p>
 * <p>
 * This class is semi-threadsafe. It is possible for multiple threads to modify
 * the contents of the buffer in such a way that they are incoherent according
 * to the standards of the client Suneido code. However, because the underlying
 * data array never changes and {@link #length()} is always less than or equal
 * to the size of the underlying data array, a buffer is always in a reasonable
 * state from the point of view of the run-time system.
 * </p>
 *
 * @author Victor Schappert
 * @since 20130718
 */
@DllInterface
public final class Buffer extends JSDIValue implements CharSequence {

	//
	// DATA
	//

	protected final byte[] data;
	protected int          size;

	//
	// STATICS
	//

	private static final String BUFFER_ENCODING = "ISO-8859-1";

	//
	// CONSTRUCTORS
	//

	/**
	 * <p>
	 * Constructs a buffer and initializes it using a string.
	 * </p>
	 *
	 * <p>
	 * Since Java strings are 16-bit UTF-16 characters and buffers contain
	 * bytes, the characters from {@code str} used to initialize the buffer must
	 * be reduced to 8-bit characters. This is done using the same algorithm as
	 * is used by {@link #copyStr(String, byte[], int, int)}.
	 * </p>
	 * 
	 * @param size
	 *            Size of the buffer
	 * @param str
	 *            Must have {@link String#length() length()} at least
	 *            {@code size}; used to initialize up to {@link String#length()
	 *            str.length()} bytes of the buffer
	 * @see #Buffer(int, Buffer)
	 * @see #Buffer(byte[], int, int)
	 * @see #Buffer(int)
	 */
	public Buffer(int size, String str) {
		this(size);
		final int N = str.length();
		sizeCheck(N);
		copyStr(str, data, 0, N);
	}

	/**
	 * <p>
	 * Constructs a buffer and initializes it using bytes from another buffer.
	 * </p>
	 * 
	 * @param size
	 *            Size of the buffer
	 * @param buf
	 *            Must have {@link Buffer#length() length()} at least
	 *            {@code size}; used to initialize up to {@link Buffer#length()
	 *            buf.length()} bytes of the buffer
	 * @see #Buffer(int, Buffer)
	 * @see #Buffer(byte[], int, int)
	 * @see #Buffer(int)
	 */
	public Buffer(int size, Buffer buf) {
		this(size);
		sizeCheck(buf.size);
		System.arraycopy(buf.data, 0, this.data, 0, buf.size);
	}

	/**
	 * <p>
	 * Constructs a buffer by copying a range from a byte array.
	 * </p>
	 *
	 * <p>
	 * The {@link Buffer#length() length()} of the new buffer is
	 * {@code end - start} and it contains the range {@code data[start..end-1]}.
	 * </p>
	 *
	 * @param src Data array from which to copy initial bytes of new buffer
	 * @param start Start index of copy operation
	 * @param end Index one-past-the-end of copy operation
	 * @see #Buffer(int, String)
	 * @see #Buffer(int, Buffer)
	 * @see #Buffer(int)
	 */
	public Buffer(byte[] src, int start, int end) {
		this.data = new byte[end - start];
		setAndSetSize(src, start, end);
	}

	/**
	 * <p>
	 * Constructs a buffer full of zeroes.
	 * </p>
	 *
	 * @param size Size of the buffer
	 * @see #Buffer(int, String)
	 * @see #Buffer(int, Buffer)
	 * @see #Buffer(byte[], int, int)
	 */
	public Buffer(int size) {
		this.data = new byte[size];
		this.size = size;
	}

	//
	// INTERNALS
	//

	private void sizeCheck(int minRequired) throws JSDIException {
		if (size < minRequired) {
			throw new JSDIException(
					"Buffer must be large enough for initial string");
		}
	}

	private static boolean arraysEqualN(byte[] b1, byte[] b2, int N) {
		for (int k = 0; k < N; ++k) {
			if (b1[k] != b2[k]) return false;
		}
		return true;
	}

	void setAndSetSize(byte[] src, int start, int end) {
		int k = 0;
		for (; start < end; ++k, ++start) {
			data[k] = src[start];
		}
		size = k;
	} // Deliberately package-internal

	//
	// ACCESSORS
	//

	/**
	 * <p>
	 * Returns the actual capacity of the buffer. This may be greater than the
	 * value returned by {@link #length()} if the buffer has been
	 * {@link #truncate() truncated}.
	 * </p>
	 *
	 * @return Non-negative number indicating buffer capacity
	 * @see #getInternalData()
	 */
	public int capacity() {
		// Used by marshallers to determine if they can unmarshall a string of
		// a given length into a Buffer...
		return data.length;
	}

	/**
	 * Returns {@code true} iff the buffer contains one or more zero bytes.
	 *
	 * @return Whether this buffer contains a zero
	 * @since 20130808
	 */
	public boolean hasZero() {
		for (byte b : data) {
			if (0 == b) return true;
		}
		return false;
	}

	/**
	 * <p>
	 * Returns a reference to the buffer's internal data array. Only the indices
	 * {@code 0..}{@link #length()} contain valid data.
	 * </p>
	 *
	 * @return Reference to buffer's internal data array
	 * @see #capacity()
	 */
	public byte[] getInternalData() {
		return data;
	}

	/**
	 * <p>
	 * Returns {@code true} iff another buffer is equal to this buffer.
	 * </p>
	 *
	 * @param other Reference to another buffer, may be {@code null}
	 * @return Whether {@code other} is value-wise equal to {@code this}
	 * @see #equals(String)
	 * @see #equals(Object)
	 */
	public boolean equals(Buffer other) {
		return other != null && size == other.size
				&& arraysEqualN(data, other.data, size);
		// Don't use Arrays.equals() because 'this' and 'other' may possibly
		// have different *capacities* in their backing arrays even though their
		// size is the same. This is because when a non-zero terminated string
		// is marshalled out of direct storage, the marshaller will resize the
		// existing given Buffer if it can...
	}

	/**
	 * <p>
	 * Returns {@code true} iff the contents of this buffer are equal to a given
	 * string.
	 * </p>
	 *
	 * @param other Reference to a string, may be {@code null}
	 * @return Whether {@code other} is value-wise equal to {@code this}
	 * @see #equals(Buffer)
	 * @see #equals(String)
	 */
	public boolean equals(String other) {
		if (null == other) {
			return false;
		} else {
			// cache for thread safety b/c our size could change to > other.length()
			final int N = other.length();
			if (N != size) {
				return false;
			} else {
				for (int k = 0; k < N; ++k) {
					if (data[k] != (byte)other.charAt(k))
						return false;
				}
				return true;
			}
		}
	}

	//
	// MUTATORS
	//

	/**
	 * <p>
	 * Changes the size of this buffer to a value between 0 and
	 * {@link #capacity()}.
	 * </p>
	 * 
	 * @param size
	 *            New buffer size
	 */
	public void setSize(int size) {
		if (size < 0 || capacity() < size) {
			throw new SuInternalError("invalid buffer size: " + size);
		}
		this.size = size;
	}

	/**
	 * <p>
	 * Truncates the buffer just before the first zero by shrinking the buffer
	 * size to be one less than the minimum size that would include the zero.
	 * </p> 
	 *
	 * <p>
	 * This method has no effect if the buffer does not contain a zero.
	 * </p>
	 *
	 * @return This buffer
	 * @see #setSize(int)
	 */
	public Buffer truncate() {
		for (int k = 0; k < size; ++k) {
			if (0 == data[k]) {
				size = k;
				return this;
			}
		}
		return this;
	} // Used by marshalling code to emulate cSuneido

	//
	// STATICS
	//

	/**
	 * <p>
	 * Copies the first {@code length} characters from a Java string into a
	 * range of a byte array, converting from 16-bit UTF-16 characters to 8-bit
	 * characters.
	 * </p>
	 * 
	 * @param in
	 *            String to copy; must contain at least {@code length}
	 *            characters
	 * @param out
	 *            Array to receive copied string; the range
	 *            {@code [start..start+length-1]} must be valid in {@code out}
	 * @param start
	 *            Non-negative index to first destination position in
	 *            {@code out}
	 * @param length
	 *            Number of characters to copy
	 * @return Reference to {@code out}
	 */
	public static byte[] copyStr(String in, byte[] out, int start, int length) {
		// This method is the only point in which we convert from String to
		// byte[], so if we want to introduce a more sophisticated
		// transformation to fixed-size 8-bit characters than simply truncating
		// the high-order 8 bits, it can be done here.
		int j = start;
		for (int i = 0; i < length; ++i) {
			char ch = in.charAt(i);
			out[j++] = (byte) ch;
		}
		return out;
	}

	//
	// INTERFACE: CharSequence
	//

	@Override
	public int length() {
		return size;
	}

	@Override
	public char charAt(int index) {
		if (! (0 <= index && index < size))
			throw new IndexOutOfBoundsException("invalid index: " + index);
		return (char)data[index];
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		try {
			return new String(data, start, end - start, BUFFER_ENCODING);
		} catch (UnsupportedEncodingException e ) {
			throw new JSDIException("can't convert buffer to string", e);
		}
	}

	//
	// INTERFACE: Packable
	//

	/**
	 * At the moment, {@code Buffer} is packed as if it were a string, and
	 * strings are packed as if they are arrays of 8-bit characters.
	 *
	 * @since 20130828
	 * @see #pack(ByteBuffer)
	 * @see Pack#packSize(String)
	 * @see Concats#packSize(int)
	 */
	@Override
	public int packSize(int nest) {
		return 0 == size ? 0 : 1 + size;
	}

	/**
	 * At the moment, {@code Buffer} is packed as if it were a string, and
	 * strings are packed as if they are arrays of 8-bit characters.
	 *
	 * @since 20130828
	 * @see #packSize(int)
	 * @see Concats#pack(ByteBuffer)
	 */
	@Override
	public void pack(ByteBuffer buf) {
		if (0 < size) {
			buf.put(Pack.Tag.STRING);
			buf.put(data, 0, size);
		}
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	private static final Map<String, SuCallable> builtins = BuiltinMethods
			.methods("buffer", Buffer.class);

	@Override
	public Object get(Object member) {
		if (member instanceof Range)
			return ((Range) member).substr(this);
		int i = Ops.toInt(member);
		int len = length();
		if (i < 0)
			i += len;
		return 0 <= i && i < len ? subSequence(i, i + 1) : "";
	}

	@Override
	public SuValue lookup(String method) {
		// Try using Buffer's custom built-ins. If that doesn't work, try
		// leapfrogging off of String's built-ins (note that String's built-ins
		// will convert this Buffer to a String and operate on the String).
		SuValue result = builtins.get(method);
		return null != result ? result : StringMethods.singleton.lookup(method);
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		try {
			return new String(data, 0, size, BUFFER_ENCODING);
		} catch (UnsupportedEncodingException e ) {
			throw new JSDIException("can't convert buffer to string", e);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (null == other) {
			return false;
		} else if (other instanceof Buffer) {
			return equals((Buffer)other); 
		} else {
			return equals(other.toString());
		}
	}

	@Override
	public int hashCode() {
		int h = 0;
		for (int k = 0; k < size; ++k) {
			h = 31 * h + data[k];
		}
		return h;
	}

	//
	// BUILT-IN METHODS
	//

	/**
	 * Built-in size method. <em>eg</em>: {@code Buffer(10, "abc").Size()}. The
	 * requirements for built-in methods are documented in
	 * {@link suneido.runtime.BuiltinMethods}.
	 * @param self The buffer
	 * @return Integer size of the buffer in bytes
	 * @see suneido.runtime.BuiltinMethods
	 */
	public static Integer Size(Object self) {
		Buffer buffer = (Buffer)self;
		return buffer.length();
	}

	/**
	 * <p>
	 * Built-in multi-byte character to wide-character method.
	 * </p>
	 * <p>
	 * <strong>NOTE</strong> that this method exists for compatibility with
	 * {@code CSuneido}. It currently has a trivial implementation which assumes
	 * the input is 8-bit Windows-1252 characters. Unlike in CSuneido, this
	 * method is only available instances of {@code Buffer}, rather than for all
	 * strings. The output is always 16-bit little-endian Unicode characters.
	 * </p>
	 * @param self A buffer containing multi-byte characters
	 * @return A new buffer containing the wide-character equivalent of the
	 * buffer's contents
	 * @see #Size(Object)
	 * @see suneido.runtime.BuiltinMethods
	 */
	public static Buffer Mbstowcs(Object self) {
		Buffer buffer = (Buffer)self;
		byte[] x = buffer.data;
		byte[] y = new byte[2 * x.length];
		int i = 0, j = 0;
		for (; i < x.length; ++i, j += 2) {
			y[j] = x[i];
		}
		return new Buffer(y, 0, y.length);
	}

	//
	// BUILT-IN CLASS
	//

	/**
	 * Reference to a {@link BuiltinClass} that describes how to expose this
	 * class to the Suneido programmer.
	 * 
	 * @see suneido.runtime.Builtins
	 */
	public static final BuiltinClass clazz = new BuiltinClass() {

		private final FunctionSpec newFS = new FunctionSpec(array("size",
				"string"), "");

		@Override
		protected Object newInstance(Object... args) {
			args = Args.massage(newFS, args);
			int size = Ops.toInt(args[0]);
			if (size <= 0) {
				// Per discussion with APM 20130828, Suneido programmer is not
				// permitted to request a zero-size Buffer explicitly. However,
				// a Buffer may still be truncated to zero by the marshaller if
				// passed to a dll parameter taking 'string' type, etc.
				throw new JSDIException("Buffer size must be greater than zero");
			}
			Object o = args[1];
			return o instanceof Buffer
					? new Buffer(size, (Buffer)o)
					: new Buffer(size, Ops.toStr(o));
		}
	};
}
