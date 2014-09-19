/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// used to specify parameter names and default values for builtin methods
@Retention(RetentionPolicy.RUNTIME)
public @interface Params {
	String value() default("");
}
