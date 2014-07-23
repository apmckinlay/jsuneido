/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import suneido.jsdi.DllInterface;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
public enum TypeId {

	VOID,
	BASIC,
	STRING_DIRECT,
	STRING_INDIRECT,
	PROXY,
	STRUCT,
	CALLBACK;

}