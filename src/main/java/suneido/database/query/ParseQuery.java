package suneido.database.query;

import static suneido.language.Token.*;
import suneido.database.server.ServerData;
import suneido.language.*;

public class ParseQuery<T, G extends QueryGenerator<T>> extends Parse<T, G> {
	private ServerData serverData;

	ParseQuery(Lexer lexer, G generator) {
		super(lexer, generator);
	}

	ParseQuery(Parse<T, G> parse) {
		super(parse);
	}

	public void serverData(ServerData serverData) {
		this.serverData = serverData;
	}

	public T parse() {
		return matchReturn(EOF, query());
	}

	private T query() {
		switch (lexer.getKeyword()) {
		case INSERT:
			return insert();
		case UPDATE:
			return update();
		case DELETE:
			return delete();
		default:
			return query2();
		}
	}

	private T insert() {
		match(INSERT);
		return token == L_CURLY || token == L_BRACKET
				? insertRecord() : insertQuery();
	}

	private T insertRecord() {
		T record = record();
		match(INTO);
		T query = baseQuery();
		return generator.insertRecord(record, query);
	}

	private T record() {
		if (token != L_CURLY && token != L_BRACKET)
			syntaxError("record expected e.g. { a: 1, b: 2 }");
		ParseConstant<T, G> p = new ParseConstant<>(this);
		T result = p.object();
		token = p.token;
		return result;
	}

	private T insertQuery() {
		T query = baseQuery();
		match(INTO);
		String table = lexer.getValue();
		match(IDENTIFIER);
		return generator.insertQuery(query, table);
	}

	private T update() {
		match(UPDATE);
		T query = baseQuery();
		match(SET);
		T updates = null;
		while (token == IDENTIFIER) {
			String column = lexer.getValue();
			match(IDENTIFIER);
			match(EQ);
			T expr = expression();
			updates = generator.updates(updates, column, expr);
			if (token == COMMA)
				match();
		}
		return generator.update(query, updates);
	}

	private T delete() {
		match(DELETE);
		return generator.delete(baseQuery());
	}

	private T query2() {
		T query = baseQuery();
		return lexer.getKeyword() == SORT ? sort(query) : query;
	}

	private T sort(T query) {
		match(SORT);
		boolean reverse = matchIf(REVERSE);
		return generator.sort(query, reverse, commaList());
	}

	private T baseQuery() {
		T q = source();
		T qq;
		while ((qq = operation(q)) != null)
			q = qq;
		return q;
	}

	private T source() {
		if (lexer.getKeyword() == HISTORY)
			return history();
		else if (token == L_PAREN) {
			match(L_PAREN);
			T query = baseQuery();
			match(R_PAREN);
			return query;
		} else
			return table();
	}

	private T table() {
		String tablename = lexer.getValue();
		match(IDENTIFIER);

		if (serverData == null)
			return generator.table(tablename);

		String def = serverData.getSview(tablename);
		if (def == null)
			def = generator.getView(tablename);

		if (def == null || serverData.inView(tablename))
			return generator.table(tablename);
		else {
			try {
				serverData.enterView(tablename);
				Lexer lexer = new Lexer(def);
				lexer.ignoreCase();
				ParseQuery<T, QueryGenerator<T>> pc =
						new ParseQuery<T, QueryGenerator<T>>(lexer, generator);
				pc.serverData(serverData);
				return pc.parse();
			} finally {
				serverData.leaveView(tablename);
			}
		}
	}

	private T history() {
		match(HISTORY);
		match(L_PAREN);
		String table = lexer.getValue();
		match(IDENTIFIER);
		match(R_PAREN);
		return generator.history(table);
	}

	private T operation(T q) {
		switch (lexer.getKeyword()) {
		case PROJECT:
			return project(q);
		case REMOVE:
			return remove(q);
		case RENAME:
			return rename(q);
		case JOIN:
			return join(q);
		case LEFTJOIN:
			return leftjoin(q);
		case TIMES:
			return times(q);
		case UNION:
			return union(q);
		case MINUS:
			return minus(q);
		case INTERSECT:
			return intersect(q);
		case SUMMARIZE:
			return summarize(q);
		case EXTEND:
			return extend(q);
		case WHERE:
			return where(q);
		default:
			return null;
		}

	}

	private T project(T q) {
		match(PROJECT);
		return generator.project(q, commaList());
	}

	private T remove(T q) {
		match(REMOVE);
		return generator.remove(q, commaList());
	}

	private T commaList() {
		T columns = null;
		do {
			columns = generator.columns(columns, lexer.getValue());
			match(IDENTIFIER);
		} while (matchIf(COMMA));
		return columns;
	}

	private T rename(T q) {
		match(RENAME);
		T renames = null;
		do {
			String from = lexer.getValue();
			match(IDENTIFIER);
			match(TO);
			String to = lexer.getValue();
			match(IDENTIFIER);
			renames = generator.renames(renames, from, to);
		} while (matchIf(COMMA));
		return generator.rename(q, renames);
	}

	private T join(T q) {
		match(JOIN);
		T by = joinBy();
		return generator.join(q, by, source());
	}

	private T leftjoin(T q) {
		match(LEFTJOIN);
		T by = joinBy();
		return generator.leftjoin(q, by, source());
	}

	private T joinBy() {
		if (!matchIf(BY))
			return null;
		T by = parenList();
		if (by == null)
			syntaxError("empty join by not allowed");
		return by;
	}

	private T parenList() {
		match(L_PAREN);
		T columns = null;
		while (token != R_PAREN) {
			columns = generator.columns(columns, lexer.getValue());
			match(IDENTIFIER);
			matchIf(COMMA);
		}
		match(R_PAREN);
		return columns;
	}

	private T times(T q) {
		match(TIMES);
		return generator.times(q, source());
	}

	private T union(T q) {
		match(UNION);
		return generator.union(q, source());
	}

	private T minus(T q) {
		match(MINUS);
		return generator.minus(q, source());
	}

	private T intersect(T q) {
		match(INTERSECT);
		return generator.intersect(q, source());
	}

	private T summarize(T q) {
		match(SUMMARIZE);
		T by = null;
		while (token == IDENTIFIER && !lexer.getKeyword().sumop()
				&& lookAhead() != EQ) {
			by = generator.columns(by, lexer.getValue());
			match(IDENTIFIER);
			match(COMMA);
		}
		T ops = null;
		do {
			String name = null;
			if (!lexer.getKeyword().sumop()) {
				name = lexer.getValue();
				match(IDENTIFIER);
				match(EQ);
			}
			if (!lexer.getKeyword().sumop())
				syntaxError("summarize operation expected");
			Token op = lexer.getKeyword();
			match();
			String field = null;
			if (op != COUNT) {
				field = lexer.getValue();
				match(IDENTIFIER);
			}
			ops = generator.sumops(ops, name, op, field);
		} while (matchIf(COMMA));
		return generator.summarize(q, by, ops);
	}

	private T extend(T q) {
		match(EXTEND);
		T list = null;
		do {
			String column = lexer.getValue();
			match(IDENTIFIER);
			T expr = matchIf(EQ) ? expression() : null;
			list = generator.extendList(list, column, expr);
		} while (matchIf(COMMA));
		return generator.extend(q, list);
	}

	private T where(T q) {
		match(WHERE);
		return generator.where(q, expression());
	}

	private T expression() {
		ParseExpression<T, G> p = new ParseExpression<>(this);
		p.eq_as_is();
		T result = p.expression();
		token = p.token;
		return result;
	}


}
