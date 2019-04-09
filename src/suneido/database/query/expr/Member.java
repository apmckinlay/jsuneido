/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query.expr;

// used as temporary intermediate
// between ParseExpression/TreeQueryGenerator and FunCall
public class Member {
	public final Object left;
	public final String right;

	public Member(Object left, String right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public String toString() {
		return "Member [left=" + left + ", right=" + right + "]";
	}
}
