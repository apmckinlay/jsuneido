/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query.expr;

import suneido.compiler.Token;

// used as temporary intermediate
// between ParseExpression/TreeQueryGenerator and FunCall
public class Member extends BinOp {
	public final String right;

	public Member(Object left, String right) {
		super(Token.SUBSCRIPT, (Expr) left, Constant.valueOf(right));
		this.right = right;
	}

	@Override
	public String toString() {
		return left + "." + right;
	}
}
