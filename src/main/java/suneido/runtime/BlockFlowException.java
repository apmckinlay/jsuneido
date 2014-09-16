/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import suneido.SuException;

/**
 * <p>
 * Class for {@code "block:break"} and {@code "block:continue"} exceptions which
 * are thrown when a Suneido block uses a <b>{@code break}</b> or <b>
 * {@code continue}</b> statement, respectively.
 * </p>
 *
 * <p>
 * Unlike {@link BlockReturnException}, this exception is caught by Suneido
 * {@code try/catch} blocks. This is because {@link BlockReturnException} simply
 * uses Java exception handling to implement part of the runtime system behind
 * the scenes while {@code "block:break"} and {@code "block:continue"}
 * exceptions are part of the language available to the Suneido programmer.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140911
 * @see BlockReturnException
 */
@SuppressWarnings("serial")
public final class BlockFlowException extends SuException {

	//
	// CONSTANTS
	//

	/**
	 * Exception for {@code "block:break"}
	 */
	public static final BlockFlowException BREAK_EXCEPTION = new BlockFlowException(
			"block:break");
	/**
	 * Exception for {@code "block:break"}
	 */
	public static final BlockFlowException CONTINUE_EXCEPTION = new BlockFlowException(
			"block:continue");

	//
	// CONSTRUCTORS
	//

	private BlockFlowException(String blockFlowEvent) {
		super(blockFlowEvent, null, false, false /* no stack trace */);
	}
}
