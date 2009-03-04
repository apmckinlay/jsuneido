package suneido.database.query;

import static suneido.language.Token.*;
import suneido.language.*;

public class ParseQuery<T, G extends QueryGenerator<T>> extends Parse<T, G> {
	ParseQuery(Lexer lexer, G generator) {
		super(lexer, generator);
	}

	ParseQuery(Parse<T, G> parse) {
		super(parse);
	}

	public T parse() {
		return matchReturn(EOF, query());
	}

	public T query() {
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
		return token == L_CURLY ? insertRecord() : insertQuery();
	}

	private T insertRecord() {
		T record = record();
		match(INTO);
		T query = baseQuery();
		return generator.insertRecord(record, query);
	}

	private T record() {
		// TODO Auto-generated method stub
		return null;
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
		T columns = null;
		do {
			columns = generator.columns(columns, lexer.getValue());
			match(IDENTIFIER);
		} while (matchIf(COMMA));
		return generator.sort(query, reverse, columns);
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
			return matchReturn(IDENTIFIER, generator.table(lexer.getValue()));
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
			return leftjoin(q);
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
		// TODO Auto-generated method stub
		return null;
	}

	private T remove(T q) {
		// TODO Auto-generated method stub
		return null;
	}

	private T rename(T q) {
		// TODO Auto-generated method stub
		return null;
	}

	private T leftjoin(T q) {
		// TODO Auto-generated method stub
		return null;
	}

	private T times(T q) {
		// TODO Auto-generated method stub
		return null;
	}

	private T union(T q) {
		// TODO Auto-generated method stub
		return null;
	}

	private T minus(T q) {
		// TODO Auto-generated method stub
		return null;
	}

	private T intersect(T q) {
		// TODO Auto-generated method stub
		return null;
	}

	private T summarize(T q) {
		// TODO Auto-generated method stub
		return null;
	}

	private T extend(T q) {
		// TODO Auto-generated method stub
		return null;
	}

	private T where(T q) {
		// TODO Auto-generated method stub
		return null;
	}

	private T expression() {
		ParseExpression<T, G> p = new ParseExpression<T, G>(this);
		T result = p.expression();
		token = p.token;
		return result;
	}


}
