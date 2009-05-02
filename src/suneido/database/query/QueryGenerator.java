package suneido.database.query;

import suneido.language.Generator;
import suneido.language.Token;

public abstract class QueryGenerator<T> extends Generator<T> {

	public abstract T delete(T query);

	public abstract T columns(T columns, String column);

	public abstract T sort(T query, boolean reverse, T columns);

	public abstract T updates(T updates, String column, T expr);

	public abstract T update(T query, T updates);

	public abstract T insertRecord(T record, T query);

	public abstract T insertQuery(T query, String table);

	public abstract T history(String table);

	public abstract T table(String table);

	public abstract T project(T query, T columns);

	public abstract T remove(T query, T columns);

	public abstract T times(T query1, T query2);

	public abstract T union(T query1, T query2);

	public abstract T minus(T query1, T query2);

	public abstract T intersect(T query1, T query2);

	public abstract T join(T query1, T by, T query2);

	public abstract T leftjoin(T query1, T by, T query2);

	public abstract T renames(T renames, String from, String to);

	public abstract T rename(T query, T renames);

	public abstract T extendList(T list, String column, T expr);

	public abstract T extend(T query, T list);

	public abstract T where(T query, T expr);

	public abstract T sumops(T sumops, String name, Token op, String field);

	public abstract T summarize(T query, T by, T ops);

}
