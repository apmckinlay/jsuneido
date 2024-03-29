/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static suneido.compiler.Token.COUNT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import suneido.SuException;
import suneido.SuObject;
import suneido.SuRecord;
import suneido.compiler.Token;
import suneido.database.immudb.Transaction;
import suneido.database.query.expr.*;
import suneido.runtime.Ops;

@SuppressWarnings("unchecked")
public class TreeQueryGenerator extends QueryGenerator<Object> {

	private final Transaction tran;

	public TreeQueryGenerator(Transaction tran) {
		this.tran = tran;
	}

	@Override
	public Object columns(Object columns, String column) {
		List<String> list =	columns == null ? new ArrayList<>()
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
		return new Extend((Query) query, e.cols, e.exprs);
	}
	static class Extends {
		List<String> cols = new ArrayList<>();
		List<Expr> exprs = new ArrayList<>();
	}
	@Override
	public Object extendList(Object listOb, String column, Object expr) {
		Extends list = listOb == null ? new Extends() : (Extends) listOb;
		list.cols.add(column);
		list.exprs.add((Expr) expr);
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
		return Project.project((Query) query, (List<String>) columns);
	}

	@Override
	public Object remove(Object query, Object columns) {
		return Project.remove((Query) query, (List<String>) columns);
	}

	@Override
	public Object rename(Object query, Object renames) {
		Renames r = (Renames) renames;
		return new Rename((Query) query, r.froms, r.tos);
	}
	static class Renames {
		List<String> froms = new ArrayList<>();
		List<String> tos = new ArrayList<>();
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
		List<String> cols = new ArrayList<>();
		List<String> funcs = new ArrayList<>();
		List<String> on = new ArrayList<>();
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
		List<String> cols = new ArrayList<>();
		List<Expr> exprs = new ArrayList<>();
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
				listOb == null ? new ArrayList<>() : (List<Expr>) listOb;
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
	public Object subscript(Object term, Object expression) {
		return new BinOp(Token.SUBSCRIPT, (Expr) term, (Expr) expression);
	}

	@Override
	public Object conditional(Object expr, Object iftrue, Object iffalse) {
		return new TriOp((Expr) expr, (Expr) iftrue, (Expr) iffalse);
	}

	// QueryFirst('tables where tablename.Lower() is "columns"')

	@Override
	public Object functionCall(Object function, Object arguments) {
		Expr ob = null;
		String fname;
		if (function instanceof Identifier)
			fname = ((Identifier) function).ident;
		else if (function instanceof Member) {
			Member m = (Member) function;
			ob = m.left;
			fname = m.right;
		} else
			throw new SuException("query functions must be called by name, got " + function.getClass());
		if (arguments == null)
			arguments = Collections.emptyList();
		return new FunCall(ob, fname, (List<Expr>) arguments);
	}

	@Override
	public Object identifier(String text) {
		return new Identifier(text);
	}

	@Override
	public SuObject object(SuObject ob, int lineNumber) {
		return ob;
	}

	@Override
	public Object unaryExpression(Token op, Object expression) {
		if (expression instanceof Constant) {
			switch (op) {
			case ADD:
				return Constant.valueOf(Ops.uplus(((Constant) expression).value));
			case SUB:
				return Constant.valueOf(Ops.uminus(((Constant) expression).value));
			default:
				break;
			}
		}
		return new UnOp(op, (Expr) expression);
	}

	@Override
	public Object and(Object expr1, Object expr2) {
		return (expr1 == null) ? expr2 : And.make((Expr) expr1, (Expr) expr2);
	}

	@Override
	public Object or(Object expr1, Object expr2) {
		return (expr1 == null) ? expr2 : Or.make((Expr) expr1, (Expr) expr2);
	}

	@Override
	public Object in(Object expr, Object list) {
		return new In((Expr) expr, (List<Object>) list);
	}

	@Override
	public Object expressionList(Object listOb, Object expr) {
		// only used by "in" which in queries requires constants
		if (! (expr instanceof Constant))
			throw new RuntimeException("query in values must be constants");
		Object value = ((Constant) expr).value;
		List<Object> list =
				listOb == null ? new ArrayList<>() : (List<Object>) listOb;
		list.add(value);
		return list;
	}

	@Override
	public Object rvalue(Object expr) {
		return expr;
	}

	@Override
	public Object memberRef(Object term, String identifier, int lineNumber) {
	        return new Member(term, identifier);
	}

	@Override
	public String getView(String name) {
		return tran.getView(name);
	}

	@Override
	public Object value(Object value) {
		return Constant.valueOf(value);
	}

}
