/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import suneido.SuContainer;
import suneido.SuValue;

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
 *    catch (e) // internally, e is an instance of Except
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
		assert message != null;
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
		Throwable e = ((Except) self).throwable;
		ArrayList<StackTraceElement> stack = Lists.newArrayList();
		getStack(e, stack);
		SuContainer calls = new SuContainer();
		for (int i = stack.size() - 1; i >= 0; --i)
			if (!stack.get(i).toString().contains(".java:"))
				calls.add(callob(stack.get(i)));
		return calls;
	}

	private static void getStack(Throwable e, ArrayList<StackTraceElement> dest) {
		if (e.getCause() != null)
			getStack(e.getCause(), dest); // bottom up
		StackTraceElement[] stackTrace = e.getStackTrace();
		// skip duplication with cause
		int i = stackTrace.length - 1;
		for (int idest = 0; i >= 0 && idest < dest.size()
				&& stackTrace[i].equals(dest.get(idest)); --i, ++idest) {
		}
		for (; i >= 0; --i)
			dest.add(stackTrace[i]); // reverse order
	}

	private static SuContainer callob(StackTraceElement x) {
		SuContainer call = new SuContainer();
		call.put("fn", x.toString());
		call.put("locals", new SuContainer());
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
