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
	SourceLanguage sourceLanguage() default SourceLanguage.JAVA;
}
