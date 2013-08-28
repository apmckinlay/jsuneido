package suneido.language.jsdi;

import static suneido.util.Util.array;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;

import suneido.SuValue;
import suneido.language.*;
import suneido.language.builtin.StringMethods;

/**
 * <p>
 * Implements the Suneido {@code Buffer} type, an abitrary sequence of bytes.
 * </p>
 * <p>
 * This class is semi-threadsafe. It is possible for multiple threads to modify
 * the contents of the buffer in such a way that they are incoherent according
 * to the standards of the client Suneido code. However, because the underlying
 * data array never changes and {@link #length()} is always less than or equal to
 * the size of the underlying data array, a buffer is always in a reasonable
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

	private final byte[] data;
	private int          size;

	//
	// STATICS
	//

	private static final String BUFFER_ENCODING = "ISO-8859-1";

	//
	// CONSTRUCTORS
	//

	public Buffer(int size, String str) {
		this.data = new byte[size];
		this.size = size;
		final int N = str.length();
		sizeCheck(N);
		copyStr(str, data, 0, N);
	}

	public Buffer(int size, Buffer buf) {
		this.data = new byte[size];
		this.size = size;
		sizeCheck(buf.size);
		System.arraycopy(buf.data, 0, this.data, 0, buf.size);
	}

	public Buffer(byte[] src, int start, int end) {
		this.data = new byte[end - start];
		setAndSetSizeInternal(src, start, end);
	}

	// TODO -- docs -- since 20130814
	Buffer(Buffer other) { // Copy constructor for testing purposes
		this.data = Arrays.copyOf(other.data, other.data.length);
		this.size = other.size;
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

	//
	// ACCESSORS
	//

	// TODO: docs -- and see #length()
	public int capacity() {
		return data.length;
	}

	// TODO: docs -- 20130809 -- added for testing purposes
	public String toStringNoZeroes() {
		StringBuilder builder = new StringBuilder(size);
		for (int k = 0; k < size; ++k) {
			byte b = data[k];
			if (0 == b) break;
			builder.append((char)b);
		}
		return builder.toString();
	}

	// TODO: docs -- since 20130808
	// TODO: add test
	public boolean hasZero() {
		for (byte b : data) {
			if (0 == b) return true;
		}
		return false;
	}

	// TODO: docs
	// TODO: make this public to support Structure(Buffer)
	public byte[] getInternalData() {
		return data;
	}

	void copyInternalData(byte[] dest, int start, int maxChars) {
		maxChars = Math.min(size, maxChars);
		System.arraycopy(data, 0, dest, start, maxChars);
	}

	public boolean equals(Buffer other) {
		return other != null && size == other.size
				&& arraysEqualN(data, other.data, size);
		// Don't use Arrays.equals() because 'this' and 'other' may possibly
		// have different *capacities* in their backing arrays even though their
		// size is the same. This is because when a non-zero terminated string
		// is marshalled out of direct storage, the marshaller will resize the
		// existing given Buffer if it can...
	}

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

	void setAndSetSize(byte[] src, int start, int end) {
		assert end - start <= data.length;
		setAndSetSizeInternal(src, start, end);
		if (size < data.length) data[size] = 0; 
	}

	// TODO: docs -- since 20130813 -- truncate size to first 0
	Buffer truncate() {
		for (int k = 0; k < size; ++k) {
			if (0 == data[k]) {
				size = k;
				return this;
			}
		}
		return this;
	}

	//
	// INTERNALS
	//

	private void setAndSetSizeInternal(byte[] src, int start, int end) {
		int k = 0;
		for (; start < end; ++k, ++start) {
			data[k] = src[start];
		}
		size = k;
	}

	private static boolean arraysEqualN(byte[] b1, byte[] b2, int N) {
		for (int k = 0; k < N; ++k) {
			if (b1[k] != b2[k]) return false;
		}
		return true;
	}

	//
	// STATICS
	//

	static int copyStr(String in, byte[] out, int start, int length) {
		int j = start;
		for (int i = 0; i < length; ++i) {
			char ch = in.charAt(i);
			out[j++] = (byte) ch;
		}
		return j;
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
	// ANCESTOR CLASS: SuValue
	//

	private static final Map<String, SuCallable> builtins = BuiltinMethods
			.methods(Buffer.class);

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
	 * {@link suneido.language.BuiltinMethods}.
	 * @param self The buffer
	 * @return Integer size of the buffer in bytes
	 * @see suneido.language.BuiltinMethods
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
	 * @see suneido.language.BuiltinMethods
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
	 * @see suneido.language.Builtins
	 */
	public static final SuValue clazz = new BuiltinClass() {

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
