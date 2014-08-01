/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import suneido.jsdi.DllInterface;

/**
 * <p>
 * Enumerates categories of types in the JSDI type hierarchy.
 * </p>
 *
 * @author Victor Schappert
 * @since 20130625
 * @see Type#getTypeId()
 */
@DllInterface
public enum TypeId {

	/**
	 * The type behaves like an instance of {@link VoidType}. This corresponds
	 * to a <code><b>void</b></code> return "type" on a {@code dll}. 
	 */
	VOID,
	/**
	 * The type behaves like an instance of {@link BasicValue} or
	 * {@link BasicArray}. It represents either a single primitive value, such
	 * as <code><b>int16</b> x</code> or an array of primitive values, such as
	 * <code><b>int8</b>[16] y</code>.
	 */
	BASIC,
	/**
	 * The type behaves like an instance of {@link StringDirect} implementing a
	 * string of 8-bit characters in direct storage. It represents a value such
	 * as <code><b>string</b>[m] str</code> or <code><b>buffer</b>[n]
	 * buf</code>.
	 */
	STRING_DIRECT,
	/**
	 * The type behaves like a subclass of {@link StringIndirect} implementing a
	 * string of 8-bit characters in indirect storage (<em>ie</em> it represents
	 * a pointer to a string. It represents a value such as
	 * <code><b>string</b> str</code> or <code>[<b>in</b>] <b>string</b>
	 * instr</code>.
	 */
	STRING_INDIRECT,
	/**
	 * The type behaves like a late-binding type that can be bound to a concrete
	 * instance of a user-defined {@link ComplexType}, such as a
	 * {@code callback} or {@code struct}.
	 */
	LATE_BINDING,
	/**
	 * The type behaves like a concrete instance of {@link Structure}, a
	 * user-defined {@code struct}.
	 */
	STRUCT,
	/**
	 * The type behaves like a concrete instance of {@link Callback}, a
	 * user-defined {@code callback}.
	 */
	CALLBACK;
}