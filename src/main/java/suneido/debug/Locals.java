/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates methods whose local variables should be available as Suneido debug
 * information.
 *
 * @author Victor Schappert
 * @since 20140902
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Locals {

	/**
	 * Enumerates the possible source languages a method may be compiled from.
	 */
	public enum SourceLanguage {
		JAVA, SUNEIDO;
	}

	/**
	 * Indicates the source language of a method whose local variables can be
	 * inspected at runtime.
	 * 
	 * @return Method source language
	 */
	public SourceLanguage sourceLanguage() default SourceLanguage.JAVA;

	/**
	 * Flags a Suneido method having a <b>{@code this}</b> local variable.
	 * 
	 * @return Whether the annotated method is a "self call"
	 */
	public boolean isSelfCall() default false;

	/**
	 * Indicates the name of the "args array" local variable. If the method does
	 * not use an "args array", this attribute is <b>{@code null}</b>.
	 *
	 * @return Whether the annotated method uses an args array
	 */
	public String argsArray() default "";

	/**
	 * Flags a Suneido method whose non-parameter local variables are opaque to
	 * the Suneido programmer.
	 *
	 * @return True iff non-parameter local variables should be hidden from the
	 *         debug system
	 */
	public boolean ignoreNonParams();
}
