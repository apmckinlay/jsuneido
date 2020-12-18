/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query.expr;

import static suneido.SuInternalError.unreachable;
import static suneido.compiler.Token.GT;
import static suneido.compiler.Token.GTE;
import static suneido.compiler.Token.LT;
import static suneido.compiler.Token.LTE;
import static suneido.runtime.Ops.*;
import static suneido.util.ByteBuffers.bufferUcompare;
import static suneido.util.Util.union;

import java.nio.ByteBuffer;
import java.util.List;

import suneido.compiler.Token;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.runtime.Ops;

public class BinOp extends Expr {
	public Token op;
	public Expr left;
	public Expr right;
	private boolean isTerm = false; // valid for isTermFields
	private List<String> isTermFields = null;

	public BinOp(Token op, Expr left, Expr right) {
		this.op = op;
		this.left = left;
		this.right = right;
		if (left instanceof Constant && op.termop())
			reverse();
	}

	private void reverse() {
		Expr tmp = left; left = right; right = tmp;
		op = switch (op) {
			case LT -> GT;
			case LTE -> GTE;
			case GT -> LT;
			case GTE -> LTE;
			case IS, ISNT -> op;
			default -> throw unreachable();
		};
	}

	@Override
	public String toString() {
		return (op == Token.SUBSCRIPT)
			? left + "[" + right + "]"
			: "(" + left + " " + op.string + " " + right + ")";
	}

	@Override
	public List<String> fields() {
		return union(left.fields(), right.fields());
	}

	@Override
	public Expr fold() {
		left = left.fold();
		right = right.fold();
		Object x = left.constant();
		Object y = right.constant();
		if (x != null && y != null)
			return Constant.valueOf(eval2(x, y));
		return this;
	}

	private Object eval2(Object x, Object y) {
		return switch (op) {
			case IS -> is(x, y);
			case ISNT -> isnt(x, y);
			case LT -> cmp(x, y) < 0;
			case LTE -> cmp(x, y) <= 0;
			case GT -> cmp(x, y) > 0;
			case GTE -> cmp(x, y) >= 0;
			case ADD -> add(x, y);
			case SUB -> sub(x, y);
			case CAT -> cat(x, y);
			case MUL -> mul(x, y);
			case DIV -> div(x, y);
			case MOD -> mod(x, y);
			case LSHIFT -> lshift(x, y);
			case RSHIFT -> rshift(x, y);
			case BITAND -> bitand(x, y);
			case BITOR -> bitor(x, y);
			case BITXOR-> bitxor(x, y);
			case MATCH -> match(x, y);
			case MATCHNOT -> matchnot(x, y);
			case SUBSCRIPT -> get(x, y);
			default -> throw unreachable();
		};
	}

	// override Ops.cmp to make "" < all other values
	// to match packed comparison
	private static int cmp(Object x, Object y) {
		if (x == y)
			return 0;
		if ("".equals(x))
			return -1;
		if ("".equals(y))
			return +1;
		return Ops.cmp(x, y);
	}

	// see also In
	@Override
	public boolean isTerm(List<String> fields) {
		if (! fields.equals(isTermFields)) {
			isTerm = isTerm2(fields); // cache
			isTermFields = fields;
		}
		return isTerm;
	}

	private boolean isTerm2(List<String> fields) {
		if (! op.termop())
			return false;
		return left.isField(fields) && right instanceof Constant;
	}

	@Override
	public Object eval(Header hdr, Row row) {
		// only use raw comparison if isTerm has been used (by Select)
		// NOTE: do NOT want to use raw for Extend because of rule issues
		if (isTerm && hdr.fields().equals(isTermFields)) {
			Identifier id = (Identifier) left;
			ByteBuffer field = row.getraw(hdr, id.ident);
			Constant c = (Constant) right;
			ByteBuffer value = c.packed;
			return switch (op) {
				case IS -> field.equals(value);
				case ISNT -> ! field.equals(value);
				case LT -> bufferUcompare(field, value) < 0;
				case LTE -> bufferUcompare(field, value) <= 0;
				case GT -> bufferUcompare(field, value) > 0;
				case GTE -> bufferUcompare(field, value) >= 0;
				default -> throw unreachable();
			};
		} else
			return eval2(left.eval(hdr, row), right.eval(hdr, row));
	}

	@Override
	public Expr rename(List<String> from, List<String> to) {
		Expr new_left = left.rename(from, to);
		Expr new_right = right.rename(from, to);
		return new_left == left && new_right == right ? this :
			new BinOp(op, new_left, new_right);
	}

	@Override
	public Expr replace(List<String> from, List<Expr> to) {
		Expr new_left = left.replace(from, to);
		Expr new_right = right.replace(from, to);
		return new_left == left && new_right == right ? this :
			new BinOp(op, new_left, new_right);
	}

	@Override
	public boolean cantBeNil(List<String> fields) {
		if (! isTerm(fields))
			return false;
		Constant c = (Constant) right;
		return switch (op) {
			case IS -> c != Constant.EMPTY;
			case ISNT -> c == Constant.EMPTY;
			case LT -> Ops.lte(c.value, "");
			case LTE -> Ops.lt(c.value, "");
			case GT -> Ops.gte(c.value, "");
			case GTE -> Ops.gt(c.value, "");
			default -> false;
		};
	}

}
