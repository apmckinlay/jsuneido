package suneido.database.query;

import static suneido.language.Token.*;
import suneido.database.Index;
import suneido.language.*;

public class ParseRequest<T> extends Parse<T, RequestGenerator<T>> {
	ParseRequest(Lexer lexer, RequestGenerator<T> generator) {
		super(lexer, generator);
	}

	ParseRequest(Parse<T, RequestGenerator<T>> parse) {
		super(parse);
	}

	T parse() {
		return matchReturn(EOF, request());
	}

	private T request() {
		switch (lexer.getKeyword()) {
		case CREATE:
			return create();
		case ENSURE:
			return ensure();
		case ALTER:
			return alter();
		case RENAME:
			return rename();
		case VIEW:
			return view();
		case SVIEW:
			return sview();
		case DROP:
			return drop();
		default:
			syntaxError();
			return null;
		}
	}

	private T create() {
		match(CREATE);
		String table = lexer.getValue();
		match(IDENTIFIER);
		return generator.create(table, schema());
	}

	private T ensure() {
		match(ENSURE);
		String table = lexer.getValue();
		match(IDENTIFIER);
		return generator.ensure(table, partialSchema());
	}

	private T alter() {
		match(ALTER);
		String table = lexer.getValue();
		match(IDENTIFIER);
		switch (lexer.getKeyword()) {
		case CREATE:
			return alterCreate(table);
		case DROP:
		case DELETE:
			return alterDrop(table);
		case RENAME:
			return alterRename(table);
		default:
			syntaxError();
			return null;
		}
	}

	private T alterCreate(String table) {
		match(CREATE);
		return generator.alterCreate(table, partialSchema());
	}

	private T alterDrop(String table) {
		match();
		return generator.alterDrop(table, partialSchema());
	}

	private T alterRename(String table) {
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
		verifyMatch(EOF);
		return generator.alterRename(table, renames);
	}

	private T schema() {
		if (token != L_PAREN)
			syntaxError();
		return partialSchema();
	}

	private T partialSchema() {
		T columns = token == L_PAREN ? columns() : null;
		T indexes = indexes();
		verifyMatch(EOF);
		return generator.schema(columns, indexes);
	}

	private T columns() {
		T columns = null;
		match(L_PAREN);
		while (token != R_PAREN) {
			if (token == SUB) {
				columns = generator.columns(columns, "-");
				match();
			} else {
				columns = generator.columns(columns, lexer.getValue());
				match(IDENTIFIER);
			}
			if (token == COMMA)
				match(COMMA);
		}
		match(R_PAREN);
		return columns;
	}

	private T indexes() {
		T index;
		T indexes = null;
		while (null != (index = index()))
			indexes = generator.indexes(indexes, index);
		return indexes;
	}

	private T index() {
		Token token = lexer.getKeyword();
		if (token != KEY && token != INDEX)
			return null;
		boolean key = lexer.getKeyword() == KEY;
		match();
		boolean unique = false;
		boolean lower = false;
		for (;; match())
			if (lexer.getKeyword() == UNIQUE)
				unique = true;
			else if (lexer.getKeyword() == LOWER)
				lower = true;
			else
				break;
		T columns = columnList();
		T foreignKey = foreignKey();
		return generator.index(key, unique, lower, columns, foreignKey);
	}

	private T foreignKey() {
		if (lexer.getKeyword() != IN)
			return null;
		match();
		String table = lexer.getValue();
		match(IDENTIFIER);
		T columns = token == L_PAREN ? columnList() : null;
		int mode = Index.BLOCK;
		if (lexer.getKeyword() == CASCADE) {
			match();
			mode = Index.CASCADE;
			if (lexer.getKeyword() == UPDATE) {
				match();
				mode = Index.CASCADE_UPDATES;
			}
		}
		return generator.foreignKey(table, columns, mode);
	}

	private T columnList() {
		match(L_PAREN);
		T columns = null;
		while (token != R_PAREN) {
			columns = generator.columns(columns, lexer.getValue());
			match(IDENTIFIER);
			if (token == COMMA)
				match(COMMA);
		}
		match(R_PAREN);
		return columns;
	}

	private T rename() {
		match(RENAME);
		String from = lexer.getValue();
		match(IDENTIFIER);
		match(TO);
		String to = lexer.getValue();
		match(IDENTIFIER);
		verifyMatch(EOF);
		return generator.rename(from, to);
	}

	private T view() {
		String name = viewName();
		return generator.view(name, lexer.remaining().trim());
	}

	private T sview() {
		String name = viewName();
		return generator.sview(name, lexer.remaining().trim());
	}

	private String viewName() {
		match();
		String name = lexer.getValue();
		match(IDENTIFIER);
		verifyMatch(EQ);
		lexer.nextAll(); // don't skip comments
		token = EOF;
		return name;
	}

	private T drop() {
		match(DROP);
		String name = lexer.getValue();
		match(token == STRING ? STRING : IDENTIFIER);
		verifyMatch(EOF);
		return generator.drop(name);
	}
}
