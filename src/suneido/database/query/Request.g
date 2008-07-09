grammar Request;

options {
	language = Java;
}
tokens {
	COLUMNS;
}

@header {
package suneido.database.query;
import java.util.Collections;
}
@lexer::header { package suneido.database.query; }

@members {
public interface IRequest {
	void create(String table, Schema schema);
	void ensure(String table, Schema schema);
	void alter_create(String table, Schema schema);
	void alter_delete(String table, Schema schema);
	void alter_rename(String table, List<Rename> renames);
	void rename(String from, String to);
	void drop(String table);
}
static class PrintRequest implements IRequest {
	public void create(String table, Schema schema) {
		System.out.println("addTable(" + table + ")");
		schema(schema);
	}
	public void ensure(String table, Schema schema) {
		System.out.println("ensure " + table);
		schema(schema);
	}
	public void alter_create(String table, Schema schema) {
		System.out.println("alter create " + table);
		schema(schema);
	}
	public void alter_delete(String table, Schema schema) {
		System.out.println("alter delete " + table);
		schema(schema);
	}
	private void schema(Schema schema) {
		for (String col : schema.columns)
			System.out.println("addColumn(" + col + ")");
		for (Index index : schema.indexes) {
			System.out.print("addIndex(" + index.columns + ", " + index.isKey + index.isUnique);
			if (index.in != null)
				System.out.print(", " + index.in.table + ", " + index.in.columns + ", " + index.in.mode);
			System.out.println(")");
		}
	}
	public void alter_rename(String table, List<Rename> renames) {
		for (Rename r : renames)
			System.out.println("renameColumn(" + table + ", " + r.from + ", " + r.to + ")");
	}
	public void rename(String from, String to) {
		System.out.println("renameTable(" + from + ", " + to + ")");
	}
	public void drop(String table) {
		System.out.println("removeTable(" + table + ")");
	}
}
public IRequest iRequest = new PrintRequest();

static class Schema {
	List<String> columns = Collections.EMPTY_LIST;
	List<Index> indexes = new ArrayList<Index>();
}
static class Index {
	boolean isKey = false;
	boolean isUnique = false;
	List<String> columns;
	In in = In.nil;
	Index(boolean isKey, boolean isUnique, List<String> columns, In in) {
		this.isKey = isKey;
		this.isUnique = isUnique;
		this.columns = columns;
		if (in != null)
			this.in = in;
	}
}
static class In {
	static final In nil = new In(null, Collections.EMPTY_LIST, 0);
	String table;
	List<String> columns;
	int mode;
	In(String table, List<String> columns, int mode) {
		this.table = table;
		this.columns = columns;
		this.mode = mode;
	}
}
static class Rename {
	String from;
	String to;
	Rename(String from, String to) {
		this.from = from;
		this.to = to;
	}
}
}

request 
	scope { Schema schema; }
	@init { $request::schema = new Schema(); }
	: 'create' ID schema
		{ iRequest.create($ID.text, $request::schema); }
	| 'ensure' ID partial
		{ iRequest.ensure($ID.text, $request::schema); }
	| 'alter' ID 'rename' renames
		{ iRequest.alter_rename($ID.text, $renames.list); }
    | 'alter' ID 'create' partial
    	{ iRequest.alter_create($ID.text, $request::schema); }
    | 'alter' ID 'delete' partial
    	{ iRequest.alter_delete($ID.text, $request::schema); }
    | rename
    	{ iRequest.rename($rename.from, $rename.to); }
    | ('drop'|'delete') ID
    	{ iRequest.drop($ID.text); }
    ;
  
schema	: schema_columns (key|index)* ;

partial : schema_columns? (key|index)*;

schema_columns : columns
	{ $request::schema.columns = $columns.list; }
	;

columns returns [List<String> list]
	@init { list = new ArrayList<String>(); }
	:	'(' column[list] (','? column[list] )* ')' ;
column[List<String> list]
	:	ID { list.add($ID.text); } ;
	
key	: 'key' columns in? 
		{ $request::schema.indexes.add(new Index(true, false, $columns.list, $in.in)); }
	;

index : 'index' (u='unique' | 'lower')? columns in? 
		{ $request::schema.indexes.add(new Index(false, $u != null, $columns.list, $in.in)); }
	;

in returns [In in]
	:	'in' ID columns? (c='cascade' u='updates'?)? 
		{
		int mode = 0;
		if ($u != null)
			mode = 1;
		else if ($c != null)
			mode = 3;
		$in = new In($ID.text, $columns.list, mode);
		}
	;

renames returns [List<Rename> list]
	@init { list = new ArrayList<Rename>(); }
	: rename1[list] (',' rename1[list] )* ;
rename1[List<Rename> list]
	:	f=ID 'to' t=ID 
		{ list.add(new Rename($f.text, $t.text)); }
	;
    
rename 	 returns [String from, String to]
	:	'rename' f=ID 'to' t=ID 
		{ $from = $f.text; $to = $t.text; }
	;

ID    : ('a'..'z'|'A'..'Z'|'_')('a'..'z'|'A'..'Z'|'0'..'9'|'_')*('?'|'!')? ;

WS    : (' '|'\t'|'\r'|'\n')+ { $channel = HIDDEN; } ;
COMMENT : '/*' .* '*/' { $channel = HIDDEN; } ;
