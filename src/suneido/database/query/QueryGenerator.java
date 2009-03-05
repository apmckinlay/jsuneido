package suneido.database.query;

import suneido.language.Generator;
import suneido.language.Token;

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

	T project(T query, T columns);

	T remove(T query, T columns);

	T times(T query1, T query2);

	T union(T query1, T query2);

	T minus(T query1, T query2);

	T intersect(T query1, T query2);

	T join(T query1, T by, T query2);

	T leftjoin(T query1, T by, T query2);

	T renames(T renames, String from, String to);

	T rename(T query, T renames);

	T extendList(T list, String column, T expr);

	T extend(T query, T list);

	T where(T query, T expr);

	T sumops(T sumops, String name, Token op, String field);

	T summarize(T query, T by, T ops);

}
