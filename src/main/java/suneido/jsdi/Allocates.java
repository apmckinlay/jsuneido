/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi;

/**
 * This annotation flags the code as allocating memory in the DLL (or being
 * attached to allocated memory in the JSDI DLL). All classes tagged with this
 * annotation must have finalizers to ensure that any garbage collection done
 * on the Java side propagates to the native side.
 * @author Victor Schappert
 * @since 20130621
 */
@DllInterface
public @interface Allocates {

}
