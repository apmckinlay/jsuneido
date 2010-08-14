package suneido.database.query;

import static suneido.language.Token.COUNT;

import java.util.*;

import suneido.*;
import suneido.database.Transaction;
import suneido.database.query.expr.*;
import suneido.language.Ops;
import suneido.language.Token;

@SuppressWarnings("unchecked")
public class TreeQueryGenerator extends QueryGenerator<Object> {

	private final Transaction tran;

	public TreeQueryGenerator(Transaction tran) {
		this.tran = tran;
	}

	@Override
	public Object columns(Object columns, String column) {
		List<String> list =	columns == null ? new ArrayList<String>()
						: (List<String>) columns;
		list.add(column);
		return list;
	}

	@Override
	public Object delete(Object query) {
		return new Delete(tran, (Query) query);
	}

	@Override
	public Object extend(Object query, Object list) {
		Extends e = (Extends) list;
		return new Extend((Query) query, e.cols, e.exprs, e.rules);
	}
	static class Extends {
		List<String> cols = new ArrayList<String>();
		List<Expr> exprs = new ArrayList<Expr>();
		List<String> rules = new ArrayList<String>();
	}
	@Override
	public Object extendList(Object listOb, String column, Object expr) {
		Extends list = listOb == null ? new Extends() : (Extends) listOb;
		if (expr == null)
			list.rules.add(column);
		else {
			list.cols.add(column);
			list.exprs.add((Expr) expr);
		}
		return list;
	}

	@Override
	public Object history(String table) {
		return new History(tran, table);
	}

	@Override
	public Object insertQuery(Object query, String table) {
		return new InsertQuery(tran, (Query) query, table);
	}

	@Override
	public Object insertRecord(Object record, Object query) {
		return new InsertRecord((Query) query, (SuRecord) record);
	}

	@Override
	public Object intersect(Object query1, Object query2) {
		return new Intersect((Query) query1, (Query) query2);
	}

	@Override
	public Object join(Object query1, Object by, Object query2) {
		return new Join((Query) query1, (Query) query2, (List<String>) by);
	}

	@Override
	public Object leftjoin(Object query1, Object by, Object query2) {
		return new LeftJoin((Query) query1, (Query) query2, (List<String>) by);
	}

	@Override
	public Object minus(Object query1, Object query2) {
		return new Difference((Query) query1, (Query) query2);
	}

	@Override
	public Object project(Object query, Object columns) {
		return new Project((Query) query, (List<String>) columns);
	}

	@Override
	public Object remove(Object query, Object columns) {
		return new Project((Query) query, (List<String>) columns, true);
	}

	@Override
	public Object rename(Object query, Object renames) {
		Renames r = (Renames) renames;
		return new Rename((Query) query, r.froms, r.tos);
	}
	static class Renames {
		List<String> froms = new ArrayList<String>();
		List<String> tos = new ArrayList<String>();
	}
	@Override
	public Object renames(Object renames, String from, String to) {
		Renames list = renames == null ? new Renames() : (Renames) renames;
		list.froms.add(from);
		list.tos.add(to);
		return list;
	}

	@Override
	public Object sort(Object query, boolean reverse, Object columns) {
		return new Sort((Query) query, reverse, (List<String>) columns);
	}

	@Override
	public Object summarize(Object query, Object by, Object ops) {
		if (by == null)
			by = Collections.emptyList();
		Sumops s = (Sumops) ops;
		return new Summarize((Query) query, (List<String>) by, s.cols, s.funcs,
				s.on);
	}
	static class Sumops {
		List<String> cols = new ArrayList<String>();
		List<String> funcs = new ArrayList<String>();
		List<String> on = new ArrayList<String>();
	}
	@Override
	public Object sumops(Object sumops, String name, Token op, String field) {
		Sumops list = sumops == null ? new Sumops() : (Sumops) sumops;
		if (name == null)
			name = op == COUNT ? "count" : op.string + "_" + field;
		list.cols.add(name);
		list.funcs.add(op.string);
		list.on.add(field);
		return list;
	}

	@Override
	public Object table(String table) {
		return new Table(tran, table);
	}

	@Override
	public Object times(Object query1, Object query2) {
		return new Product((Query) query1, (Query) query2);
	}

	@Override
	public Object union(Object query1, Object query2) {
		return new Union((Query) query1, (Query) query2);
	}

	@Override
	public Object update(Object query, Object updates) {
		Updates u = (Updates) updates;
		return new Update(tran, (Query) query, u.cols, u.exprs);
	}

	static class Updates {
		List<String> cols = new ArrayList<String>();
		List<Expr> exprs = new ArrayList<Expr>();
	}
	@Override
	public Object updates(Object updates, String column, Object expr) {
		Updates list = updates == null ? new Updates() : (Updates) updates;
		list.cols.add(column);
		list.exprs.add((Expr) expr);
		return list;
	}

	@Override
	public Object where(Object query, Object expr) {
		return new Select(tran, (Query) query, (Expr) expr);
	}

	@Override
	public Object argumentList(Object listOb, Object keyword, Object expression) {
		List<Expr> args =
				listOb == null ? new ArrayList<Expr>() : (List<Expr>) listOb;
		if (keyword != null)
			throw new SuException(
					"query expressions don't support keyword arguments");
		args.add((Expr) expression);
		return args;
	}

	@Override
	public Object binaryExpression(Token op, Object expr1, Object expr2) {
		return new BinOp(op, (Expr) expr1, (Expr) expr2);
	}

	@Override
	public Object bool(boolean value) {
		return value ? Boolean.TRUE : Boolean.FALSE;
	}

	@Override
	public Object conditional(Object expr, Object iftrue, Object iffalse,
			Object label) {
		return new TriOp((Expr) expr, (Expr) iftrue, (Expr) iffalse);
	}

	@Override
	public Object constant(Object value) {
		return Constant.valueOf(value);
	}

	@Override
	public Object date(String value) {
		return Ops.stringToDate(value);
	}

	@Override
	public Object functionCall(Object function, Object arguments) {
		if (!(function instanceof Identifier))
			throw new SuException("query functions must be called by name");
		String fname = ((Identifier) function).ident;
		if (arguments == null)
			arguments = Collections.emptyList();
		return new FunCall(fname, (List<Expr>) arguments);
	}

	@Override
	public Object identifier(String text) {
		return new Identifier(text);
	}

	public static class MemDef {
		final public Object name;
		final public Object value;

		public MemDef(Object name, Object value) {
			this.name = name;
			this.value = value;
		}
	}
	@Override
	public Object memberDefinition(Object name, Object value) {
		return new MemDef(name, value);
	}
	@Override
	public Object memberList(MType which, Object members, Object member) {
		SuContainer rec = object(which, members);
		MemDef m = (MemDef) member;
		if (m.name == null)
			rec.append(m.value);
		else
			rec.put(m.name, m.value);
		return rec;
	}

	@Override
	public Object number(String value) {
		return Ops.stringToNumber(value);
	}

	@Override
	public SuContainer object(MType which, Object members) {
		return members == null
				? which == MType.RECORD ? new SuRecord() : new SuContainer()
				: (SuContainer) members;
	}

	@Override
	public Object string(String value) {
		return value;
	}

	@Override
	public Object symbol(String identifier) {
		return identifier;
	}

	@Override
	public Object unaryExpression(Token op, Object expression) {
		return new UnOp(op, (Expr) expression);
	}

	@Override
	public Object and(Object label, Object expr1, Object expr2) {
		return (expr1 == null) ? expr2 : And.make((Expr) expr1, (Expr) expr2);
	}

	@Override
	public Object or(Object label, Object expr1, Object expr2) {
		return (expr1 == null) ? expr2 : Or.make((Expr) expr1, (Expr) expr2);
	}

	@Override
	public Object in(Object expression, Object constant) {
		if (constant == null)
			return new In((Expr) expression);
		In in = (In) expression;
		in.add(constant);
		return in;
	}

	@Override
	public Object rvalue(Object expr) {
		return expr;
	}

	@Override
	public String getView(String name) {
		return tran.getView(name);
	}

}
