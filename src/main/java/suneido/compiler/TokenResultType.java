/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

public enum TokenResultType {
	I("Ljava/lang/Integer;"),
	N("Ljava/lang/Number;"),
	S("Ljava/lang/String;"),
	B("Ljava/lang/Boolean;"),
	B_("Z"),
	O("Ljava/lang/Object;");

	String type;

	TokenResultType(String s) {
		type = s;
	}
}
