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
	:	query2 
    	{ $result = $query2.result; }
	;

query2 returns [Query result] 
	: source op* sort?
    	{ $result = $source.result; } // TEMPORARY
	;

source returns [Query result]
	: ID
		{ $result = new QueryTable($ID.text); }
    | 'history' '(' ID ')'
    | '(' query2 ')'
    	{ $result = $query2.result; }
    ;

op	: ('project'|'remove') cols
    | 'rename' (ID 'to' ID) (',' ID 'to' ID)*
    | ('join'|'leftjoin') ('by' '(' cols ')')? source
    | ('union'|'times'|'difference'|'intersect') source
    | 'summarize' summary (',' summary)*
    | 'extend' extend (',' extend)*
    | 'where' expr
    ;
    
sort : 'sort' 'reverse'? cols ;

columns : '(' ID (','? ID)* ')' ;

cols  : ID (','? ID)* ;

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

