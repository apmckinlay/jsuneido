package suneido.database.query;


public interface RequestGenerator<T> {

	T drop(String name);

	T rename(String from, String to);

	T view(String name, String definition);

	T sview(String name, String definition);

	T create(String table, T schema);

	T ensure(String table, T schema);

	T columns(T columns, String column);

	T indexes(T indexes, T index);

	T schema(T columns, T indexes);

	T index(boolean key, boolean unique, T columns, T foreignKey);

	T foreignKey(String table, T columns, int mode);

	T alterCreate(String table, T schema);

	T alterDrop(String table, T schema);

	T alterRename(String table, T renames);

	T renames(T renames, String from, String to);

}
