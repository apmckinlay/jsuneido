/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi;

/**
 * This annotation flags the code as representing a 64-bit conversion issue.
 * Code tagged with this annotation will need to be refactored or rewritten to
 * work with x86_64 binaries.
 * 
 * @author Victor Schappert
 * @since 20140520
 */
@DllInterface
public @interface _64BitIssue {

}
