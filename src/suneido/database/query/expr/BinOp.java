package suneido.database.query.expr;

import static suneido.Util.union;

import java.util.List;

public class BinOp extends Expr {
	public final String op;
	public final Expr left;
	public final Expr right;
	public enum Op {
		IS, ISNT, MATCH, MATCHNOT, LT, LTE, GT, GTE
	};

	public BinOp(String op, Expr left, Expr right) {
		this.op = op;
		this.left = left;
		this.right = right;
	}


	@Override
	public String toString() {
		return "(" + left + " " + op + " " + right + ")";
	}


	@Override
	public List<String> fields() {
		return union(left.fields(), right.fields());
	}

	//	@Override
	//	public boolean is_term(List<String> fields) {
	//		// NOTE: MATCH and MATCHNOT are NOT terms
	//		if (! (op.equals("=") || op.equals("!=") ||
	//				op == I_LT || op == I_LTE || op == I_GT || op == I_GTE))
	//			return false;
	//		if (left->isfield(fields) && dynamic_cast<Constant*>(right))
	//			return true;
	//		if (dynamic_cast<Constant*>(left) && right->isfield(fields))
	//		{
	//			std::swap(left, right);
	//			switch (op)
	//			{
	//			case I_LT : op = I_GT; break ;
	//			case I_LTE : op = I_GTE; break ;
	//			case I_GT : op = I_LT; break ;
	//			case I_GTE : op = I_LTE; break ;
	//			case I_IS : case I_ISNT : break ;
	//			default :
	//				unreachable();
	//			}
	//			return true;
	//		}
	//		return false;
	//	}

}
