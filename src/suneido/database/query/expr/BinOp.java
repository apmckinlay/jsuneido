package suneido.database.query.expr;

import static suneido.SuException.unreachable;
import static suneido.Util.union;

import java.util.List;

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
	public boolean is_term(List<String> fields) {
		if (op.ordinal() > Op.GTE.ordinal())
			return false;
		if (left.isfield(fields) && right instanceof Constant)
			return true;
		if (left instanceof Constant && right.isfield(fields)) {
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

}
