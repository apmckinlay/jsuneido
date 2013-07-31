package suneido.language.jsdi;

import static suneido.util.Util.array;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import suneido.SuValue;
import suneido.language.*;

/**
 * <p>
 * Implements the Suneido {@code Buffer} type, an abitrary sequence of bytes.
 * </p>
 * <p>
 * This class is semi-threadsafe. It is possible for multiple threads to modify
 * the contents of the buffer in such a way that they are incoherent according
 * to the standards of the client Suneido code. However, because the underlying
 * data array never changes and {@link #size()} is always less than or equal to
 * the size of the underlying data array, a buffer is always in a reasonable
 * state from the point of view of the run-time system.
 * </p>
 *
 * @author Victor Schappert
 * @since 20130718
 */
@DllInterface
public final class Buffer extends SuValue {

	//
	// DATA
	//

	private final byte[] data;
	private int          size;

	//
	// CONSTRUCTORS
	//

	public Buffer(int size, CharSequence str) {
		this.data = new byte[size];
		this.size = size;
		final int N = Math.min(size, str.length());
		copyStr(str.toString(), data, 0, N);
	}

	public Buffer(byte[] src, int start, int end) {
		this.data = new byte[end - start];
		setAndSetSizeInternal(src, start, end);
	}

	//
	// ACCESSORS
	//

	public int size() {
		return size;
	}

	public int capacity() {
		return data.length;
	}

	byte[] getInternalData() {
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
	// ANCESTOR CLASS: SuValue
	//

	private static final Map<String, SuCallable> builtins = BuiltinMethods
			.methods(Buffer.class);

	@Override
	public SuValue lookup(String method) {
		SuValue result = builtins.get(method);
		return null != result ? result : new SuValue.NotFound(method);
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		try {
			return new String(data, 0, size, "ISO-8859-1");
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

	//
	// BUILT-IN METHODS
	//

	/**
	 * Built-in size method. <em>eg</em>: {@code Buffer(10, "abc").Size()}. The
	 * requirements for built-in methods are documented in
	 * {@link suneido.language.BuiltinMethods}.
	 * @param self The structure.
	 * @return Integer size of the structure in bytes.
	 * @see suneido.language.BuiltinMethods
	 */
	public static Object Size(Object self) {
		Buffer buffer = (Buffer)self;
		return buffer.size();
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

		FunctionSpec newFS = new FunctionSpec(array("size", "string"), "");
		@Override
		protected Object newInstance(Object... args) {
			args = Args.massage(newFS, args);
			int size = Ops.toInt(args[0]);
			if (size < 0) {
				throw new JSDIException("invalid buffer size: " + size);
			}
			return new Buffer(size, Ops.toStr(args[1]));
		}
	};
}
