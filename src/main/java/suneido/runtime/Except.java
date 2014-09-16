/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import suneido.SuContainer;
import suneido.SuValue;
import suneido.debug.Callstack;
import suneido.debug.CallstackProvider;
import suneido.debug.DebugManager;
import suneido.debug.Frame;

/**
 * <p>
 * Type of a variable declared in a Suneido {@code catch} clause. Can be treated
 * as a string for backward compatibility.
 * </p>
 *
 * <p>
 * Consider the following Suneido-language code:
 * <pre>    try
 *        f()
 *    catch (e /* internally, e is an instance of Except *&#47;)
 *    {
 *        Print(Display(e.Callstack());
 *    }
 * </p>
 * 
 * @author Andrew McKinlay, Victor Schappert
 */
public final class Except extends String2 {

	//
	// DATA
	//

	private final String message;
	private final Throwable throwable;

	//
	// CONSTRUCTORS
	//

	public Except(Throwable throwable) {
		this(translateMsgToSuneido(throwable), throwable);
	}

	public Except(String message, Throwable throwable) {
		this.message = message;
		this.throwable = throwable;
	}

	//
	// ACCESSORS
	//

	/**
	 * Called by {@link Ops} to get the internal throwable.
	 *
	 * @return Throwable object
	 */
	Throwable getThrowable() {
		return throwable;
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public String typeName() {
		return "Except";
	}

	@Override
	public SuValue lookup(String method) {
		SuValue f = methods.getMethod(method);
		return f != null ? f : super.lookup(method);
	}

	//
	// INTERFACE: CharSequence
	//

	@Override
	public char charAt(int index) {
		return message.charAt(index);
	}

	@Override
	public int length() {
		return message.length();
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return message.subSequence(start, end);
	}

	@Override
	public String toString() {
		return message;
	}

	//
	// BUILT-IN METHODS
	//

	private static final BuiltinMethods methods = new BuiltinMethods(
			Except.class);

	@Params("string")
	public static Object As(Object self, Object a) {
		return new Except(Ops.toStr(a), ((Except) self).throwable);
	}

	public static Object Callstack(Object self) {
		Throwable t = ((Except) self).throwable;
		Callstack s = t instanceof CallstackProvider ? ((CallstackProvider) t)
		        .getCallstack() : DebugManager.getInstance()
		        .makeCallstackFromThrowable(t);
		SuContainer c = new SuContainer(s.size());
		for (Frame f : s) {
			c.add(callob(f));
		}
		return c;
	}

	private static SuContainer callob(Frame x) {
		SuContainer call = new SuContainer();
		call.put("locals", x.getLocalsContainer());
		call.put("fn", x.getFrame());
		int lineNumber = x.getLineNumber();
		call.put("line", 0 < lineNumber ? lineNumber : Boolean.FALSE);
		return call;
	}

	private static String translateMsgToSuneido(Throwable throwable) {
		// Some Java exceptions have special names in Suneido. This method
		// converts the Java-style message to a Suneido-style message where
		// appropriate.
		if (throwable instanceof StackOverflowError) {
			return "function call overflow";
		} else if (throwable instanceof NullPointerException ||
					throwable instanceof AssertionError) {
			return throwable.getClass().getName();
		} else {
			return throwable.getMessage();
		}
	}
}