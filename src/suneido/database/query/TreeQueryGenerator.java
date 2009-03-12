package suneido.database.query;

import static suneido.language.Token.COUNT;

import java.util.*;

import suneido.*;
import suneido.database.query.expr.*;
import suneido.language.Generator;
import suneido.language.Token;
import suneido.language.ParseExpression.Value;

public class TreeQueryGenerator implements QueryGenerator<Object> {
	TreeQueryGenerator() {
	}

	public Generator<Object> create() {
		return new TreeQueryGenerator();
	}

	public Object columns(Object columns, String column) {
		List<String> list =	columns == null ? new ArrayList<String>()
						: (List<String>) columns;
		list.add(column);
		return list;
	}

	public Object delete(Object query) {
		return new Delete((Query) query);
	}

	public Object extend(Object query, Object list) {
		Extends e = (Extends) list;
		return new Extend((Query) query, e.cols, e.exprs, e.rules);
	}
	static class Extends {
		List<String> cols = new ArrayList<String>();
		List<Expr> exprs = new ArrayList<Expr>();
		List<String> rules = new ArrayList<String>();
	}
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

	public Object history(String table) {
		return new History(table);
	}

	public Object insertQuery(Object query, String table) {
		return new InsertQuery((Query) query, table);
	}

	public Object insertRecord(Object record, Object query) {
		return new InsertRecord((Query) query, (SuRecord) record);
	}

	public Object intersect(Object query1, Object query2) {
		return new Intersect((Query) query1, (Query) query2);
	}

	public Object join(Object query1, Object by, Object query2) {
		return new Join((Query) query1, (Query) query2, (List<String>) by);
	}

	public Object leftjoin(Object query1, Object by, Object query2) {
		return new LeftJoin((Query) query1, (Query) query2, (List<String>) by);
	}

	public Object minus(Object query1, Object query2) {
		return new Difference((Query) query1, (Query) query2);
	}

	public Object project(Object query, Object columns) {
		return new Project((Query) query, (List<String>) columns);
	}

	public Object remove(Object query, Object columns) {
		return new Project((Query) query, (List<String>) columns, true);
	}

	public Object rename(Object query, Object renames) {
		// TODO Auto-generated method stub
		Renames r = (Renames) renames;
		return new Rename((Query) query, r.froms, r.tos);
	}
	static class Renames {
		List<String> froms = new ArrayList<String>();
		List<String> tos = new ArrayList<String>();
	}
	public Object renames(Object renames, String from, String to) {
		Renames list = renames == null ? new Renames() : (Renames) renames;
		list.froms.add(from);
		list.tos.add(to);
		return list;
	}

	public Object sort(Object query, boolean reverse, Object columns) {
		return new Sort((Query) query, reverse, (List<String>) columns);
	}

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
	public Object sumops(Object sumops, String name, Token op, String field) {
		Sumops list = sumops == null ? new Sumops() : (Sumops) sumops;
		if (name == null)
			name = op == COUNT ? "count" : op.string + "_" + field;
		list.cols.add(name);
		list.funcs.add(op.string);
		list.on.add(field);
		return list;
	}

	public Object table(String table) {
		return new Table(table);
	}

	public Object times(Object query1, Object query2) {
		return new Product((Query) query1, (Query) query2);
	}

	public Object union(Object query1, Object query2) {
		return new Union((Query) query1, (Query) query2);
	}

	public Object update(Object query, Object updates) {
		Updates u = (Updates) updates;
		return new Update((Query) query, u.cols, u.exprs);
	}

	static class Updates {
		List<String> cols = new ArrayList<String>();
		List<Expr> exprs = new ArrayList<Expr>();
	}
	public Object updates(Object updates, String column, Object expr) {
		Updates list = updates == null ? new Updates() : (Updates) updates;
		list.cols.add(column);
		list.exprs.add((Expr) expr);
		return list;
	}

	public Object where(Object query, Object expr) {
		return new Select((Query) query, (Expr) expr);
	}

	public Object argumentList(Object listOb, String keyword, Object expression) {
		List<Expr> args =
				listOb == null ? new ArrayList<Expr>() : (List<Expr>) listOb;
		if (keyword != null)
			throw new SuException(
					"query expressions don't support keyword arguments");
		args.add((Expr) expression);
		return args;
	}

	public Object assignment(Object term, Value<Object> value, Token op,
			Object expression) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object atArgument(String n, Object expr) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object binaryExpression(Token op, Object expr1, Object expr2) {
		return new BinOp(op, (Expr) expr1, (Expr) expr2);
	}

	public Object block(Object params, Object statements) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object bool(boolean value) {
		return value ? Constant.TRUE : Constant.FALSE;
	}

	public Object breakStatement() {
		return null;
	}

	public Object caseValues(Object values, Object expression) {
		return null;
	}

	public Object catcher(String variable, String pattern, Object statement) {
		return null;
	}

	public Object classConstant(String base, Object members) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object conditional(Object expr, Object iftrue, Object iffalse) {
		return new TriOp((Expr) expr, (Expr) iftrue, (Expr) iffalse);
	}

	public Object constant(Object value) {
		return Constant.valueOf((SuValue) value);
	}

	public Object continueStatement() {
		return null;
	}

	public Object date(String value) {
		return SuDate.valueOf(value);
	}

	public Object dowhileStatement(Object statement, Object expression) {
		return null;
	}

	public Object expressionList(Object list, Object expression) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object expressionStatement(Object expression) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object forClassicStatement(Object expr1, Object expr2, Object expr3,
			Object statement) {
		return null;
	}

	public Object forInStatement(String var, Object expr, Object statement) {
		return null;
	}

	public Object foreverStatement(Object statement) {
		return null;
	}

	public Object function(Object params, Object compound) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object functionCall(Object function, Object arguments) {
		if (!(function instanceof Identifier))
			throw new SuException("query functions must be called by name");
		String fname = ((Identifier) function).ident;
		if (arguments == null)
			arguments = Collections.emptyList();
		return new FunCall(fname, (List<Expr>) arguments);
	}

	public Object identifier(String text) {
		return new Identifier(text);
	}

	public Object ifStatement(Object expression, Object t, Object f) {
		return null;
	}

	public Object member(Object term, String identifier) {
		// TODO Auto-generated method stub
		return null;
	}

	public static class MemDef {
		final public SuValue name;
		final public SuValue value;

		public MemDef(SuValue name, SuValue value) {
			this.name = name;
			this.value = value;
		}
	}
	public Object memberDefinition(Object name, Object value) {
		return new MemDef((SuValue) name, (SuValue) value);
	}
	public Object memberList(ObjectOrRecord which, Object members, Object member) {
		SuRecord rec = members == null ? new SuRecord() : (SuRecord) members;
		MemDef m = (MemDef) member;
		rec.put(m.name, m.value);
		return rec;
	}

	public Object newExpression(Object term, Object arguments) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object number(String value) {
		return SuNumber.valueOf(value);
	}

	public Object object(ObjectOrRecord which, Object members) {
		return members;
	}

	public Object parameters(Object list, String name, Object defaultValue) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object postIncDec(Object term, Token incdec, Value<Object> value) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object preIncDec(Object term, Token incdec, Value<Object> value) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object returnStatement(Object expression) {
		return null;
	}

	public Object self() {
		return null;
	}

	public Object statementList(Object n, Object next) {
		return null;
	}

	public Object string(String value) {
		return SuString.valueOf(value);
	}

	public Object subscript(Object term, Object expression) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object switchCases(Object cases, Object values, Object statements) {
		return null;
	}

	public Object switchStatement(Object expression, Object cases) {
		return null;
	}

	public Object symbol(String identifier) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object throwStatement(Object expression) {
		return null;
	}

	public Object tryStatement(Object tryStatement, Object catcher) {
		return null;
	}

	public Object unaryExpression(Token op, Object expression) {
		return new UnOp(op, (Expr) expression);
	}

	public Object whileStatement(Object expression, Object statement) {
		return null;
	}

	public Object and(Object expr1, Object expr2) {
		return And.make((Expr) expr1, (Expr) expr2);
	}

	public Object or(Object expr1, Object expr2) {
		return Or.make((Expr) expr1, (Expr) expr2);
	}

	public Object in(Object expression, Object constant) {
		if (constant == null)
			return new In((Expr) expression);
		In in = (In) expression;
		in.add((SuValue) constant);
		return in;
	}

	public void startFunction() {
	}

	public void lvalue(Value<Object> value) {
	}

}
