package suneido.database.query;

import suneido.language.Generator;

public interface QueryGenerator<T> extends Generator<T> {

	T delete(T query);

	T columns(T columns, String column);

	T sort(T query, boolean reverse, T columns);

	T updates(T updates, String column, T expr);

	T update(T query, T updates);

	T insertRecord(T record, T query);

	T insertQuery(T query, String table);

	T history(String table);

	T table(String table);
}
