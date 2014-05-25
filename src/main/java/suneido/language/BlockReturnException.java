/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

/**
 * return from within block is implemented as throw BlockReturnException
 * <p>
 * This exception is <u>not</u> caught by the Suneido language try-catch
 * <p>
 * See also: {@link suneido.language.Ops} blockReturnException and catchMatch
 */
@SuppressWarnings("serial")
public class BlockReturnException extends RuntimeException {
	public final Object returnValue;
	public final int parent; // used by Ops.blockReturnHandler

	public BlockReturnException(Object returnValue, int parent) {
		this.returnValue = returnValue;
		this.parent = parent;
	}

	@Override
	public String toString() {
		return "block-return(" + Ops.display(returnValue) + ")";
	}
}
