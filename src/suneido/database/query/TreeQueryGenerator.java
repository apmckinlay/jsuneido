package suneido.database.query;

import static suneido.language.Token.COUNT;

import java.util.*;

import suneido.SuException;
import suneido.SuRecord;
import suneido.database.query.expr.*;
import suneido.language.Ops;
import suneido.language.Token;
import suneido.language.ParseExpression.Value;

@SuppressWarnings("unchecked")
public class TreeQueryGenerator extends QueryGenerator<Object> {

	@Override
	public Object columns(Object columns, String column) {
		List<String> list =	columns == null ? new ArrayList<String>()
						: (List<String>) columns;
		list.add(column);
		return list;
	}

	@Override
	public Object delete(Object query) {
		return new Delete((Query) query);
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
		return new History(table);
	}

	@Override
	public Object insertQuery(Object query, String table) {
		return new InsertQuery((Query) query, table);
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
		return new Table(table);
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
		return new Update((Query) query, u.cols, u.exprs);
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
		return new Select((Query) query, (Expr) expr);
	}

	@Override
	public Object argumentList(Object listOb, String keyword, Object expression) {
		List<Expr> args =
				listOb == null ? new ArrayList<Expr>() : (List<Expr>) listOb;
		if (keyword != null)
			throw new SuException(
					"query expressions don't support keyword arguments");
		args.add((Expr) expression);
		return args;
	}

	@Override
	public Object assignment(Object term, Value<Object> value, Token op,
			Object expression) {
		return null;
	}

	@Override
	public void atArgument(String n) {
	}
	@Override
	public Object atArgument(String n, Object expr) {
		return null;
	}

	@Override
	public Object binaryExpression(Token op, Object expr1, Object expr2) {
		return new BinOp(op, (Expr) expr1, (Expr) expr2);
	}

	@Override
	public Object block(Object params, Object statements) {
		return null;
	}

	@Override
	public Object bool(boolean value) {
		return value ? Boolean.TRUE : Boolean.FALSE;
	}

	@Override
	public Object breakStatement(Object loop) {
		return null;
	}

	public Object caseValues(Object values, Object expression) {
		return null;
	}

	@Override
	public Object catcher(String variable, String pattern, Object statement) {
		return null;
	}

	@Override
	public Object classConstant(String base, Object members) {
		return null;
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
	public Object continueStatement(Object loop) {
		return null;
	}

	@Override
	public Object date(String value) {
		return Ops.stringToDate(value);
	}

	@Override
	public Object dowhileStatement(Object body, Object expr, Object label) {
		return null;
	}

	@Override
	public Object expressionList(Object list, Object expression) {
		return null;
	}

	@Override
	public Object expressionStatement(Object expression) {
		return null;
	}

	@Override
	public Object forClassicStatement(Object expr1, Object expr2, Object expr3,
			Object statement, Object loop) {
		return null;
	}

	@Override
	public Object forInStatement(String var, Object expr, Object statement,
			Object loop) {
		return null;
	}

	@Override
	public Object foreverStatement(Object statement, Object label) {
		return null;
	}

	@Override
	public Object function(Object params, Object compound) {
		return null;
	}

	@Override
	public Object functionCall(Object function, Value<Object> value,
			Object arguments) {
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

	@Override
	public Object ifStatement(Object expr, Object t, Object f, Object label) {
		return null;
	}

	@Override
	public Object member(Object term, Value<Object> value) {
		return null;
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
		SuRecord rec = members == null ? new SuRecord() : (SuRecord) members;
		MemDef m = (MemDef) member;
		rec.put(m.name, m.value);
		return rec;
	}

	@Override
	public Object newExpression(Object term, Object arguments) {
		return null;
	}

	@Override
	public Object number(String value) {
		return Ops.stringToNumber(value);
	}

	@Override
	public Object object(MType which, Object members) {
		return members;
	}

	@Override
	public Object parameters(Object list, String name, Object defaultValue) {
		return null;
	}

	@Override
	public Object postIncDec(Object term, Token incdec, Value<Object> value) {
		return null;
	}

	@Override
	public Object preIncDec(Object term, Token incdec, Value<Object> value) {
		return null;
	}

	@Override
	public Object returnStatement(Object expression, Object context) {
		return null;
	}

	@Override
	public Object selfRef() {
		return null;
	}

	@Override
	public Object superRef() {
		return null;
	}

	@Override
	public Object statementList(Object n, Object next) {
		return null;
	}

	@Override
	public Object string(String value) {
		return value;
	}

	@Override
	public Object subscript(Object term, Object expression) {
		return null;
	}

	@Override
	public Object symbol(String identifier) {
		return null;
	}

	@Override
	public Object throwStatement(Object expression) {
		return null;
	}

	@Override
	public Object tryStatement(Object tryStatement, Object catcher,
			Object trycatch) {
		return null;
	}

	@Override
	public Object unaryExpression(Token op, Object expression) {
		return new UnOp(op, (Expr) expression);
	}

	@Override
	public Object whileStatement(Object expression, Object statement,
			Object loop) {
		return null;
	}

	@Override
	public Object and(Object expr1, Object expr2) {
		return And.make((Expr) expr1, (Expr) expr2);
	}

	@Override
	public Object or(Object expr1, Object expr2) {
		return Or.make((Expr) expr1, (Expr) expr2);
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
	public void startFunction(Object name) {
	}

	@Override
	public Object startBlock() {
		return true;
	}

	@Override
	public void lvalue(Value<Object> value) {
	}

	@Override
	public void afterStatement(Object statements) {
	}

	@Override
	public void argumentName(String keyword) {
	}

	@Override
	public void preFunctionCall(Value<Object> value) {
	}

	@Override
	public Object and(Object prevlabel) {
		return null;
	}

	@Override
	public void andEnd(Object label) {
	}

	@Override
	public Object or(Object label) {
		return null;
	}

	@Override
	public void orEnd(Object label) {
	}

	@Override
	public Object ifExpr(Object expr) {
		return null;
	}
	@Override
	public void ifThen(Object label, Object t) {
	}
	@Override
	public Object ifElse(Object label) {
		return null;
	}

	@Override
	public Object conditionalTrue(Object label, Object first) {
		return null;
	}

	@Override
	public Object loop() {
		return true; // can't be null
	}

	@Override
	public void whileExpr(Object expr, Object loop) {
	}

	@Override
	public void newCall() {
	}

	@Override
	public Object forStart() {
		return null;
	}
	@Override
	public void forIncrement(Object label) {
	}
	@Override
	public void forCondition(Object cond, Object loop) {
	}

	@Override
	public Object caseValues(Object values, Object expression, Object labels,
			boolean more) {
		return null;
	}
	@Override
	public void startCase(Object labels) {
	}
	@Override
	public void startCaseBody(Object labels) {
	}
	@Override
	public Object startSwitch() {
		return null;
	}
	@Override
	public Object switchCases(Object cases, Object values, Object statements,
			Object labels) {
		return null;
	}
	@Override
	public Object switchStatement(Object expression, Object cases, Object labels) {
		return null;
	}
	@Override
	public void startCaseValue() {
	}

	@Override
	public Object forInExpression(String var, Object expr) {
		return null;
	}

	@Override
	public void blockParams() {
	}

	public void startCatch(String var, String pattern, Object trycatch) {
	}

	public Object startTry() {
		return null;
	}

	public void startClass() {
	}

	public void addSuperInit() {
	}

	public Object rvalue(Object expr) {
		return expr;
	}

	public void lvalueForAssign(Value<Object> value, Token op) {
	}

	public void finish() {
	}

}
