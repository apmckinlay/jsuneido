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
	
query2 returns [Query result]
	: source op[$source.result]*
    	{ $result = $op.result == null ? $source.result : $op.result; } 
	;

source returns [Query result]
	: ID
		{ $result = new QueryTable($ID.text); }
    | 'history' '(' ID ')'
    | '(' query2 ')'
    	{ $result = $query2.result; }
    ;

op[Query source] returns [Query result]
	: ('project'|'remove') cols
    | 'rename' (ID 'to' ID) (',' ID 'to' ID)*
    | ('join'|'leftjoin') ('by' '(' cols ')')? source
    | ('union'|'times'|'difference'|'intersect') source
    | 'summarize' summary (',' summary)*
    | 'extend' extend (',' extend)*
    | 'where' expr
    ;
    
sort[Query source] returns [Query result]
	: 'sort' r='reverse'? cols
		 { $result = new QuerySort($source, $r != null, $cols.list); }
	;

columns : '(' cols ')' ;

cols returns [List<String> list]  
	@init { list = new ArrayList<String>(); }
	: i=ID { list.add($i.text); } 
		(','? j=ID { list.add($j.text); }
		)* ;

summary : ID
    | (ID '=')? ('total'|'average'|'max'|'min'|'count'|'list') ID
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

IS    : '=='|'is' ;
ISNT  : '!='|'isnt' ;
NOT   : '!'|'not' ;
AND   : '&&'|'and' ;
OR    : '||'|'or' ;

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

