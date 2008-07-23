package suneido.database.query.expr;

import static suneido.SuException.unreachable;
import static suneido.Util.union;

import java.nio.ByteBuffer;
import java.util.List;

import suneido.*;
import suneido.database.query.Header;
import suneido.database.query.Row;

public class BinOp extends Expr {
	public Op op;
	public Expr left;
	public Expr right;
	public enum Op {
		IS("="), ISNT("!="), LT("<"), LTE("<="), GT(">"), GTE(">="),
		MATCH("=~"), MATCHNOT("!~"),
		ADD("+"), SUB("-"), MUL("*"), DIV("/"), MOD("%"), CAT("$"),
		LSHIFT("<<"), RSHIFT(">>"), BITAND("&"), BITOR("|"), BITXOR("^");
		public String name;
		Op(String name) {
			this.name = name;
		}
	}

	public BinOp(Op op, Expr left, Expr right) {
		this.op = op;
		this.left = left;
		this.right = right;
	}

	@Override
	public String toString() {
		return "(" + left + " " + op.name + " " + right + ")";
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

	private SuValue eval2(SuValue x, SuValue y) {
		switch (op) {
		case IS :	return x.equals(y) ? SuBoolean.TRUE : SuBoolean.FALSE;
		case ISNT :	return ! x.equals(y) ? SuBoolean.TRUE : SuBoolean.FALSE;
		case LT :	return x.compareTo(y) < 0 ? SuBoolean.TRUE : SuBoolean.FALSE;
		case LTE :	return x.compareTo(y) <= 0 ? SuBoolean.TRUE : SuBoolean.FALSE;
		case GT :	return x.compareTo(y) > 0 ? SuBoolean.TRUE : SuBoolean.FALSE;
		case GTE :	return x.compareTo(y) >= 0 ? SuBoolean.TRUE : SuBoolean.FALSE;
		case ADD :	return x.add(y);
		case SUB :	return x.sub(y);
		case CAT: 	return SuString.valueOf(x.string() + y.string());
		case MUL :	return x.mul(y);
		case DIV :	return x.div(y);
		case MOD :	return SuInteger.valueOf(x.integer() % y.integer());
		case LSHIFT :	return SuInteger.valueOf(x.integer() << y.integer());
		case RSHIFT :	return SuInteger.valueOf(x.integer() >> y.integer());
		case BITAND :	return SuInteger.valueOf(x.integer() & y.integer());
		case BITOR :	return SuInteger.valueOf(x.integer() | y.integer());
		case BITXOR:	return SuInteger.valueOf(x.integer() ^ y.integer());
		case MATCH :	return matches(x.string(), y.string());
		case MATCHNOT : return matches(x.string(), y.string()).not();
		default : 	throw unreachable();
		}
	}

	// TODO convert from Suneido regex and cache compiled patterns
	private SuBoolean matches(String s, String rx) {
		return Regex.contains(s, rx) ? SuBoolean.TRUE : SuBoolean.FALSE;
	}

	@Override
	public boolean isTerm(List<String> fields) {
		if (op.ordinal() > Op.GTE.ordinal())
			return false;
		if (left.isField(fields) && right instanceof Constant)
			return true;
		if (left instanceof Constant && right.isField(fields)) {
			Expr tmp = left; left = right; right = tmp;
			switch (op) {
			case LT : op = Op.GT; break ;
			case LTE : op = Op.GTE; break ;
			case GT : op = Op.LT; break ;
			case GTE : op = Op.LTE; break ;
			case IS : case ISNT : break ;
			default : throw unreachable();
			}
			return true;
		}
		return false;
	}

	@Override
	public SuValue eval(Header hdr, Row row) {
		if (isTerm(hdr.columns())) {
			Identifier id = (Identifier) left;
			ByteBuffer field = row.getraw(hdr, id.ident);
			Constant c = (Constant) right;
			ByteBuffer value = c.packed;
			boolean result;
			switch (op) {
			case IS :	result = field.equals(value); break;
			case ISNT :	result = ! field.equals(value); break;
			case LT :	result = field.compareTo(value) < 0; break;
			case LTE :	result = field.compareTo(value) <= 0; break;
			case GT :	result = field.compareTo(value) > 0; break;
			case GTE :	result = field.compareTo(value) >= 0; break;
			default :	throw unreachable();
			}
			return result ? SuBoolean.TRUE : SuBoolean.FALSE;
		}
	else
		return eval2(left.eval(hdr, row), right.eval(hdr, row));
	}

}
