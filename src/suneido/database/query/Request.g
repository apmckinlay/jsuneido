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
	: CREATE ID schema
		{ iRequest.create($ID.text, $request::schema); }
	| ENSURE ID partial
		{ iRequest.ensure($ID.text, $request::schema); }
	| ALTER ID RENAME renames
		{ iRequest.alter_rename($ID.text, $renames.list); }
    | ALTER ID CREATE partial
    	{ iRequest.alter_create($ID.text, $request::schema); }
    | ALTER ID (DROP|DELETE) partial
    	{ iRequest.alter_delete($ID.text, $request::schema); }
    | rename
    	{ iRequest.rename($rename.from, $rename.to); }
    | (DROP|DELETE) ID
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
	
key	: KEY columns in? 
		{ $request::schema.indexes.add(new Index(true, false, $columns.list, $in.in)); }
	;

index : INDEX (u=UNIQUE | LOWER)? columns in? 
		{ $request::schema.indexes.add(new Index(false, $u != null, $columns.list, $in.in)); }
	;

in returns [In in]
	:	IN ID columns? (c=CASCADE u=UPDATES ?)? 
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
	:	RENAME f=ID TO t=ID 
		{ $from = $f.text; $to = $t.text; }
	;
	
CREATE	: ('c'|'C')('r'|'R')('e'|'E')('a'|'A')('t'|'T')('e'|'E') ;
ENSURE	: ('e'|'E')('n'|'N')('s'|'S')('u'|'U')('r'|'R')('e'|'E') ;
DELETE	: ('d'|'D')('e'|'E')('l'|'L')('e'|'E')('t'|'T')('e'|'E') ;
DROP	: ('d'|'D')('r'|'R')('o'|'O')('p'|'P') ;
ALTER	: ('a'|'A')('l'|'L')('t'|'T')('e'|'E')('r'|'R') ;
RENAME	: ('r'|'R')('e'|'E')('n'|'N')('a'|'A')('m'|'M')('e'|'E') ;
IN		: ('i'|'I')('n'|'N') ;
TO		: ('t'|'T')('o'|'O') ;
UNIQUE	: ('u'|'U')('n'|'N')('i'|'I')('q'|'Q')('u'|'U')('e'|'E') ;
LOWER	: ('l'|'L')('o'|'O')('w'|'W')('e'|'E')('r'|'R') ;
CASCADE	: ('c'|'C')('a'|'A')('s'|'S')('c'|'C')('a'|'A')('d'|'D')('e'|'E') ;
UPDATES	: ('u'|'U')('p'|'P')('d'|'D')('a'|'A')('t'|'T')('e'|'E')('s'|'S') ;
INDEX	: ('i'|'I')('n'|'N')('d'|'D')('e'|'E')('x'|'X') ;
KEY		: ('k'|'K')('e'|'E')('y'|'Y') ;

ID    : ('a'..'z'|'A'..'Z'|'_')('a'..'z'|'A'..'Z'|'0'..'9'|'_')*('?'|'!')? ;

WS    : (' '|'\t'|'\r'|'\n')+ { $channel = HIDDEN; } ;
COMMENT : '/*' .* '*/' { $channel = HIDDEN; } ;
