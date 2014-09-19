/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.marshall;

import suneido.jsdi.DllInterface;

/**
 * Enumerates the possible actions that the C++-side marshaller/unmarshaller can
 * take with respect to variable indirect (<em>ie</em> string) storage.
 * 
 * @author Victor Schappert
 * @since 20130801
 */
@DllInterface
public enum VariableIndirectInstruction {

	/**
	 * Don't do anything with the corresponding element of variable indirect
	 * storage. Just leave whatever contents it has alone.
	 */
	NO_ACTION,
	/**
	 * If the pointer corresponding to the variable indirect storage is not
	 * NULL, treat it as a zero-terminated ASCII string and set the
	 * corresponding element of variable indirect storage to a reference to a
	 * new Java {@link String} representing the ASCII string.
	 * 
	 * @see suneido.jsdi.type.InOutString
	 */
	RETURN_JAVA_STRING,
	/**
	 * <p>
	 * If the pointer corresponding to the variable indirect storage is not
	 * NULL, treat it as a Win32 API resource/Suneido {@code resource} type.
	 * </p>
	 * <p>
	 * In other words,
	 * <ul>
	 * <li>
	 * if the pointer field's high-order bits are all zero, leave the variable
	 * indirect storage alone and allow the low-order 16-bits to propagate back
	 * into Java; and
	 * </li>
	 * <li>
	 * if the pointer field's high-order bits contain a non-zero value, treat it
	 * as a pointer to a zero-terminated ASCII string and behave like
	 * {@link #RETURN_JAVA_STRING}.
	 * </li>
	 * </ul>
	 * </p>
	 * 
	 * @see suneido.jsdi.type.ResourceType
	 */
	RETURN_RESOURCE;
}
