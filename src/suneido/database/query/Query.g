grammar Query;

options {
	language = Java;
}
 
@header {
package suneido.database.query;
}
@lexer::header { package suneido.database.query; }

// need this extra rule 
// because ANTLR wants a start rule that is not referenced anywhere
query returns [Query result] 
	: query2 sort[$query2.result]?
    	{ $result = $sort.result == null ? $query2.result : $sort.result; }
	;
	
sort[Query source] returns [Query result]
	: SORT r=REVERSE ? cols
		 { $result = new QuerySort($source, $r != null, $cols.list); }
	;

query2 returns [Query result]
	scope { Query source; }
	: source { $query2::source = $source.result; } op*
    	{ $result = $op.result == null ? $source.result : $op.result; } 
	;

source returns [Query result]
	: ID
		{ $result = new QueryTable($ID.text); }
    | HISTORY '(' ID ')'
    | '(' query2 ')'
    	{ $result = $query2.result; }
    ;

op returns [Query result]
	: (p=PROJECT|REMOVE) cols
		{ $result = new QueryProject($query2::source, $cols.list, $p == null); }
    | rename
    	{ $result = $rename.result; }
    | (JOIN|LEFTJOIN) (BY '(' cols ')')? source
    | UNION source
    	{ $result = new QueryUnion($query2::source, $source.result); }
    | TIMES source
    	{ $result = new QueryProduct($query2::source, $source.result); }
    | MINUS source
     	{ $result = new QueryDifference($query2::source, $source.result); }
    | INTERSECT source
     	{ $result = new Intersect($query2::source, $source.result); }
    | SUMMARIZE summary (',' summary)*
    | EXTEND extend (',' extend)*
    | WHERE expr
    ;
    
rename returns [Query result]
	scope { List<String> froms; List<String> tos; }
	@init { $rename::froms = new ArrayList<String>(); 
		$rename::tos = new ArrayList<String>(); }
	: RENAME rename1 (',' rename1)*
		{ $result = new QueryRename($query2::source, $rename::froms, $rename::tos); }
	;
    
rename1
	: f=ID TO t=ID
		{ $rename::froms.add($f.text); $rename::tos.add($t.text); }
	;
    
columns : '(' cols ')' ;

cols returns [List<String> list]  
	@init { list = new ArrayList<String>(); }
	: i=ID { list.add($i.text); } 
		(','? j=ID { list.add($j.text); }
		)* ;

summary : ID
    | (ID '=')? (TOTAL|AVERAGE|MAX|MIN|COUNT|LIST) ID
    ;
  
extend  : ID '=' expr ;
  
expr  : or ('?' expr ':' expr)? ;
or    : and (OR and)* ;
and   : in (AND in)* ;
in    : bitor ('in' '(' constant (','? constant)* ')')? ;
bitor : bitxor ('|' bitxor)* ;
bitxor  : bitand ('^' bitand)* ;
bitand  : is ('&' is)* ;
is    : cmp ((IS|'=') cmp)* ;
cmp   : shift (('<'|'<='|'>='|'>') shift)* ;
shift : add (('<<'|'>>') add)* ;
add   : mul (('+'|'-'|'$') mul)* ;
mul   : unary (('*'|'/'|'%') unary)* ;
unary : ('-'|'+'|'~'|NOT)? term ;
term  : ID
    | constant
    | '(' expr ')'
    ;
constant : NUM
    | STRING
    | '[' members ']'
    | '#' '(' members ')'
    | '#' '{' members '}'
    ;
members : member (',' member)* ;
member  : constant
    | '(' members ')'
    | '{' members '}'
    | ID ':' member
    ;

IS    : '=='|(('i'|'I')('s'|'S')) ;
ISNT  : '!='|(('i'|'I')('s'|'S')('n'|'N')('t'|'T')) ;
NOT   : '!'|(('n'|'N')('o'|'O')('t'|'T')) ;
AND   : '&&'|(('a'|'A')('n'|'N')('d'|'D')) ;
OR    : '||'|(('o'|'O')('r'|'R')) ;

TOTAL	: ('t'|'T')('o'|'O')('t'|'T')('a'|'A')('l'|'L') ;
SORT	: ('s'|'S')('o'|'O')('r'|'R')('t'|'T') ;
PROJECT	: ('p'|'P')('r'|'R')('o'|'O')('j'|'J')('e'|'E')('c'|'C')('t'|'T') ;
MAX		: ('m'|'M')('a'|'A')('x'|'X') ;
MINUS	: ('m'|'M')('i'|'I')('n'|'N')('u'|'U')('s'|'S') ;
INTERSECT	: ('i'|'I')('n'|'N')('t'|'T')('e'|'E')('r'|'R')('s'|'S')('e'|'E')('c'|'C')('t'|'T') ;
TO		: ('t'|'T')('o'|'O') ;
LIST	: ('l'|'L')('i'|'I')('s'|'S')('t'|'T') ;
UNION	: ('u'|'U')('n'|'N')('i'|'I')('o'|'O')('n'|'N') ;
REMOVE	: ('r'|'R')('e'|'E')('m'|'M')('o'|'O')('v'|'V')('e'|'E') ;
HISTORY	: ('h'|'H')('i'|'I')('s'|'S')('t'|'T')('o'|'O')('r'|'R')('y'|'Y') ;
EXTEND	: ('e'|'E')('x'|'X')('t'|'T')('e'|'E')('n'|'N')('d'|'D') ;
COUNT	: ('c'|'C')('o'|'O')('u'|'U')('n'|'N')('t'|'T') ;
TIMES	: ('t'|'T')('i'|'I')('m'|'M')('e'|'E')('s'|'S') ;
BY		: ('b'|'B')('y'|'Y') ;
SUMMARIZE	: ('s'|'S')('u'|'U')('m'|'M')('m'|'M')('a'|'A')('r'|'R')('i'|'I')('z'|'Z')('e'|'E') ;
WHERE	: ('w'|'W')('h'|'H')('e'|'E')('r'|'R')('e'|'E') ;
LEFTJOIN	: ('l'|'L')('e'|'E')('f'|'F')('t'|'T')('j'|'J')('o'|'O')('i'|'I')('n'|'N') ;
JOIN	: ('j'|'J')('o'|'O')('i'|'I')('n'|'N') ;
RENAME	: ('r'|'R')('e'|'E')('n'|'N')('a'|'A')('m'|'M')('e'|'E') ;
REVERSE	: ('r'|'R')('e'|'E')('v'|'V')('e'|'E')('r'|'R')('s'|'S')('e'|'E') ;
MIN		: ('m'|'M')('i'|'I')('n'|'N') ;
AVERAGE	: ('a'|'A')('v'|'V')('e'|'E')('r'|'R')('a'|'A')('g'|'G')('e'|'E') ;
IN		: ('i'|'I')('n'|'N') ;

ID    : ('a'..'z'|'A'..'Z'|'_')('a'..'z'|'A'..'Z'|'0'..'9'|'_')*('?'|'!')? ;

WS    : (' '|'\t'|'\r'|'\n')+ {skip();} ; // ignore whitespace
COMMENT : '/*' .* '*/' {skip();} ; // ignore comments

STRING  : '"' .* '"'
    | '\'' .* '\''
    ;

NUM   : '0x' ('0'..'9'|'a'..'f'|'A'..'F')+
    | '0' ('0'..'7')+
    | SIGN? ('1'..'9') DIG* ('.' DIG*)? EXP?
    | SIGN? '.' DIG+ EXP?
    ;
fragment
SIGN  : '+'|'-' ;
fragment
DIG   : '0'..'9' ;
fragment
EXP   : ('e'|'E')('0'..'9')+ ;

