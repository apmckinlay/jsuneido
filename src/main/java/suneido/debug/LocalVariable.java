/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

/**
 * Local variable name/value pair in a {@link Frame}. 
 *
 * @author Victor Schappert
 * @since 20140903
 */
public final class LocalVariable {

	//
	// DATA
	//

	private final String name;
	private final Object value;

	//
	// CONSTRUCTORS
	//

	LocalVariable(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	//
	// ACCESSORS
	//

	/**
	 * Return name of the local variable (never {@code null}).
	 *
	 * @return Local variable name
	 * @see #getValue()
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return value of the local variable (never {@code null}).
	 *
	 * @return Local variable value
	 * @see #getName()
	 */
	public Object getValue() {
		return value;
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		return name + " => " + value;
	}
}
