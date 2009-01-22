grammar Request;

options {
language = Java;
}

@header {
package suneido.database.query;
import java.util.Collections;
}
@lexer::header {
package suneido.database.query;
}

@members {
public interface IRequest {
	void create(String table, Schema schema);
	void ensure(String table, Schema schema);
	void alter_create(String table, Schema schema);
	void alter_delete(String table, Schema schema);
	void alter_rename(String table, List<Rename> renames);
	void rename(String from, String to);
	void drop(String table);
	void view(String name, String definition);
	void sview(String name, String definition);
	void error(String msg);
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
	public void view(String name, String definition) {
			System.out.println("view " + name + " = " + definition);
	}
	public void sview(String name, String definition) {
			System.out.println("sview " + name + " = " + definition);
	}
	public void error(String msg) {
		System.out.println(msg);
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
public void emitErrorMessage(String msg) {
	iRequest.error(msg);
}
}

request 
	scope { Schema schema; }
	@init { $request::schema = new Schema(); }
	: CREATE id schema
		{ iRequest.create($id.text, $request::schema); }
	| ENSURE id partial
		{ iRequest.ensure($id.text, $request::schema); }
	| ALTER id RENAME renames
		{ iRequest.alter_rename($id.text, $renames.list); }
    | ALTER id CREATE partial
    	{ iRequest.alter_create($id.text, $request::schema); }
    | ALTER id (DROP|DELETE) partial
    	{ iRequest.alter_delete($id.text, $request::schema); }
    | rename
    	{ iRequest.rename($rename.from, $rename.to); }
    | VIEWDEF
    	{
    	String s = $VIEWDEF.text;
    	int i = s.indexOf('='); 
    	iRequest.view(s.substring(0, i), s.substring(i + 1).trim());
    	}
   | SVIEWDEF
    	{
    	String s = $SVIEWDEF.text;
    	int i = s.indexOf('='); 
    	iRequest.sview(s.substring(0, i), s.substring(i + 1).trim());
    	}
    | (DROP|DESTROY) id
    	{ iRequest.drop($id.text); }
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
	:	id { list.add($id.text); } ;
	
key	: KEY columns in? 
		{ $request::schema.indexes.add(new Index(true, false, $columns.list, $in.in)); }
	| KEY '(' ')'
		{ $request::schema.indexes.add(new Index(true, false, new ArrayList<String>(), null)); }
	;

index : INDEX (u=UNIQUE | LOWER)? columns in? 
		{ $request::schema.indexes.add(new Index(false, $u != null, $columns.list, $in.in)); }
	;

in returns [In in]
	:	IN id columns? (c=CASCADE u=UPDATES ?)? 
		{
		int mode = suneido.database.Index.BLOCK;
		if ($u != null)
			mode = suneido.database.Index.CASCADE_UPDATES;
		else if ($c != null)
			mode = suneido.database.Index.CASCADE;
		$in = new In($id.text, $columns.list, mode);
		}
	;

renames returns [List<Rename> list]
	@init { list = new ArrayList<Rename>(); }
	: rename1[list] (',' rename1[list] )* ;
rename1[List<Rename> list]
	:	f=id TO t=id 
		{ list.add(new Rename($f.text, $t.text)); }
	;
    
rename 	 returns [String from, String to]
	:	RENAME f=id TO t=id 
		{ $from = $f.text; $to = $t.text; }
	;
	
id	:	ID
	|	KEY
	;
	
VIEWDEF	: VIEW WHITE+ NAME WHITE* '=' ANY EOF
			{ setText($NAME.text + "=" + $ANY.text); };
			
SVIEWDEF: ('s'|'S') VIEW WHITE+ NAME WHITE* '=' ANY EOF
			{ setText($NAME.text + "=" + $ANY.text); };
			
fragment
VIEW	: ('v'|'V')('i'|'I')('e'|'E')('w'|'W') ;

CREATE	: ('c'|'C')('r'|'R')('e'|'E')('a'|'A')('t'|'T')('e'|'E') ;
ENSURE	: ('e'|'E')('n'|'N')('s'|'S')('u'|'U')('r'|'R')('e'|'E') ;
DELETE	: ('d'|'D')('e'|'E')('l'|'L')('e'|'E')('t'|'T')('e'|'E') ;
DESTROY	: ('d'|'D')('e'|'E')('s'|'S')('t'|'T')('r'|'R')('o'|'O')('y'|'Y') ;
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

ID		: ('a'..'z'|'A'..'Z'|'_')('a'..'z'|'A'..'Z'|'0'..'9'|'_')*('?'|'!')? ;

fragment
NAME	: ('a'..'z'|'A'..'Z'|'_')('a'..'z'|'A'..'Z'|'0'..'9'|'_')* ;

fragment
ANY		: ('\u0000'..'\uFFFE')+ ;

fragment
WHITE	: (' '|'\t'|'\r'|'\n') ;

WS		: WHITE+ { $channel = HIDDEN; } ;
COMMENT : '/*' .* '*/' { $channel = HIDDEN; } ;
