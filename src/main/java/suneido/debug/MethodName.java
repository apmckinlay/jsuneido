/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import suneido.runtime.SuCallable;

/**
 * <p>
 * Enumerates the names of {@link SuCallable} methods that can possibly
 * represent a Suneido stack frame.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140911
 */
enum MethodName {
	// ---------------------------------------------------------------------
	// WARNING: This code MUST be kept in sync with the equivalent code in
	// the "jsdebug" shared object/dynamic link library as well as
	// the call stack decoding code in CallstackAll.java and
	// CallstackStack.java.
	// ---------------------------------------------------------------------
	UNKNOWN(0x000), EVAL(0x100), EVAL0(EVAL.value | 10), EVAL1(
			EVAL.value | 11), EVAL2(EVAL.value | 12), EVAL3(EVAL.value | 13), EVAL4(
			EVAL.value | 14), CALL(0x200), CALL0(CALL.value | 10), CALL1(
			CALL.value | 11), CALL2(CALL.value | 12), CALL3(CALL.value | 13), CALL4(
			CALL.value | 14);
	public final int value;

	private MethodName(int value) {
		this.value = value;
	}

	public boolean isCall() {
		return CALL.value == (CALL.value & this.value);
	}

	public static MethodName getMethodName(String methodName) {
		if (methodName.startsWith("eval")) {
			if (4 == methodName.length()) {
				return EVAL;
			} else if (5 == methodName.length()) {
				switch (methodName.charAt(4)) {
				case '0':
					return EVAL0;
				case '1':
					return EVAL1;
				case '2':
					return EVAL2;
				case '3':
					return EVAL3;
				case '4':
					return EVAL4;
				}
			}
		} else if (methodName.startsWith("call")) {
			if (4 == methodName.length()) {
				return CALL;
			} else if (5 == methodName.length()) {
				switch (methodName.charAt(4)) {
				case '0':
					return CALL0;
				case '1':
					return CALL1;
				case '2':
					return CALL2;
				case '3':
					return CALL3;
				case '4':
					return CALL4;
				}
			}
		} /* if (name.startsWith(...)) */
		return UNKNOWN;
	} /* static MethodName getMethodName(...) */
} /* enum MethodName */