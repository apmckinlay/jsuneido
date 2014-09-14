package suneido.runtime;

import suneido.jsdi.DllInterface;

/**
 * Enumerates broad classes of callable objects. Mainly used for display
 * purposes.
 *
 * @author Victor Schappert
 * @since 20140912
 */
public enum CallableType {

	/**
	 * Unknown type of callable.
	 */
	UNKNOWN("???", "???", false),
	/**
	 * Callable is a plain function.
	 * 
	 * @see #BUILTIN_FUNCTION
	 * @see #METHOD
	 */
	FUNCTION("Function", "function", "$f", false),
	/**
	 * Callable is a method.
	 * 
	 * @see #BUILTIN_METHOD
	 * @see #FUNCTION
	 */
	METHOD("Method", "method", "$m", false),
	/**
	 * Callable is a built-in function.
	 * 
	 * @see #FUNCTION
	 * @see #BUILTIN_METHOD
	 */
	BUILTIN_FUNCTION("Builtin", "builtin function", false),
	/**
	 * Callable is a method of a built-in class.
	 * 
	 * @see #METHOD
	 * @see #BUILTIN_FUNCTION
	 */
	BUILTIN_METHOD("Method", "builtin method", false),
	/**
	 * Callable is a block.
	 * 
	 * @see #CLOSURE
	 * @see #WRAPPED_BLOCK
	 */
	BLOCK("Block", "block", "$b", false),
	/**
	 * Callable is a block that is wrapped in a closure.
	 *
	 * @see #BLOCK
	 * @see #CLOSURE
	 */
	WRAPPED_BLOCK("Block", "block", "$B", false),
	/**
	 * Callable is a closure wrapping a block.
	 *
	 * @see #WRAPPED_BLOCK
	 * @see #BLOCK
	 */
	CLOSURE("Block", "block", true),
	/**
	 * Callable is a method that has been bound to a "this".
	 *
	 * @see #METHOD
	 * @see #BUILTIN_METHOD
	 */
	BOUND_METHOD("Method", "method", true),
	/**
	 * Callable is a dll function.
	 */
	@DllInterface
	DLL("Dll", "dll", "$d", false);

	//
	// DATA
	//

	private final String typeName;
	private final String displayString;
	private final String compilerNameSuffix;
	private final boolean isBlock;
	private final boolean isWrapper;
	private final boolean isBuiltin;

	//
	// CONSTRUCTORS
	//

	private CallableType(String typeName, String displayString,
			String compilerNameSuffix, boolean isWrapper) {
		this.typeName = typeName;
		this.displayString = displayString;
		this.compilerNameSuffix = compilerNameSuffix;
		this.isBlock = "Block".equals(typeName);
		this.isWrapper = isWrapper;
		this.isBuiltin = displayString.startsWith("builtin");
	}

	private CallableType(String typeName, String displayString,
			boolean isWrapper) {
		this(typeName, displayString, null, isWrapper);
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns typename of the callable as it would be displayed by the
	 * {@code Type()} built-in function.
	 *
	 * @return Type name
	 * @see #displayString()
	 */
	public final String typeName() {
		return typeName;
	}

	/**
	 * Returns the type name component of the display string that would be
	 * rendered by calling the {@code Display()} built-in function.
	 *
	 * @return Type name component of display string
	 * @see #typeName()
	 */
	public final String displayString() {
		return displayString;
	}

	/**
	 * Returns a name suffix for use by the Suneido compiler, or {@code null} if
	 * this callable type is not compiled Suneido code.
	 *
	 * @return String for use by the Suneido compiler
	 */
	public final String compilerNameSuffix() {
		return compilerNameSuffix;
	}

	/**
	 * Returns <b>{@code true}</b> for {@link #BLOCK blocks} and
	 * {@link #CLOSURE closures} and <b>{@code false}</b> otherwise.
	 *
	 * @return True iff this callable type is a block
	 * @see #isWrapper()
	 * @see #isBuiltin()
	 */
	public final boolean isBlock() {
		return isBlock;
	}

	/**
	 * <p>
	 * Indicates whether this callable type wraps another {@link SuCallable}.
	 * </p>
	 *
	 * <p>
	 * This is primarily useful for deciding an {@link SuCallable} <b>
	 * {@code this}</b> in a given Java stack frame should be ignored in favour
	 * of the wrapped callable when extracting Suneido stack frames.
	 * </p>
	 *
	 * @return True iff this callable type wraps another callable
	 * @see #isBlock()
	 * @see #isBuiltin()
	 */
	public final boolean isWrapper() {
		return isWrapper;
	}

	/**
	 * Indicates whether this callable is a built-in.
	 *
	 * @return Truee iff this callable type is built in
	 * @see #isBlock()
	 * @see #isWrapper()
	 */
	public final boolean isBuiltin() {
		return isBuiltin;
	}
}
