/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query.expr;

import static suneido.SuException.unreachable;
import static suneido.language.Ops.*;
import static suneido.language.Token.GT;
import static suneido.language.Token.GTE;
import static suneido.language.Token.LT;
import static suneido.language.Token.LTE;
import static suneido.util.ByteBuffers.bufferUcompare;
import static suneido.util.Util.union;

import java.nio.ByteBuffer;
import java.util.List;

import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.language.Token;

public class BinOp extends Expr {
	public Token op;
	public Expr left;
	public Expr right;
	private boolean isTerm = false;
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
		switch (op) {
		case LT : op = GT; break ;
		case LTE : op = GTE; break ;
		case GT : op = LT; break ;
		case GTE : op = LTE; break ;
		case IS : case ISNT : break ;
		default : throw unreachable();
		}
	}

	@Override
	public String toString() {
		return "(" + left + " " + op.string + " " + right + ")";
	}

	@Override
	public List<String> fields() {
		return union(left.fields(), right.fields());
	}

	@Override
	public Expr fold() {
		left = left.fold();
		right = right.fold();
		if (left instanceof Constant && right instanceof Constant) {
			Constant kx = (Constant) left;
			Constant ky = (Constant) right;
			return Constant.valueOf(eval2(kx.value, ky.value));
		}
		return this;
	}

	private Object eval2(Object x, Object y) {
		switch (op) {
		case IS :	return is(x, y);
		case ISNT :	return isnt(x, y);
		case LT :	return lt(x, y);
		case LTE :	return lte(x, y);
		case GT :	return gt(x, y);
		case GTE :	return gte(x, y);
		case ADD :	return add(x, y);
		case SUB :	return sub(x, y);
		case CAT: 	return cat(x, y);
		case MUL :	return mul(x, y);
		case DIV :	return div(x, y);
		case MOD :	return mod(x, y);
		case LSHIFT :	return lshift(x, y);
		case RSHIFT :	return rshift(x, y);
		case BITAND :	return bitand(x, y);
		case BITOR :	return bitor(x, y);
		case BITXOR:	return bitxor(x, y);
		case MATCH :	return match(x, y);
		case MATCHNOT : return matchnot(x, y);
		default : 	throw unreachable();
		}
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
		if (left.isField(fields) && right instanceof Constant)
			return true;
		return false;
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
			boolean result;
			switch (op) {
			case IS :	result = field.equals(value); break;
			case ISNT :	result = ! field.equals(value); break;
			case LT :	result = bufferUcompare(field, value) < 0; break;
			case LTE :	result = bufferUcompare(field, value) <= 0; break;
			case GT :	result = bufferUcompare(field, value) > 0; break;
			case GTE :	result = bufferUcompare(field, value) >= 0; break;
			default :	throw unreachable();
			}
			return result ? Boolean.TRUE : Boolean.FALSE;
		}
		else
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

}
