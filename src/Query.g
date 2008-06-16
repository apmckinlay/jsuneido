grammar Query;

input 	:	 query ;

admin	:	'create' ID schema
	|	'ensure' ID schema
	|	'alter' ID alter
	|	'rename' ID 'to' ID
	|	('drop'|'delete') ID
	;
	
schema	:	columns (key|index)*;

columns	:	'(' ID (','? ID)* ')' ;

key 	:	'key' columns ('in' ID columns? ('cascade' 'updates'?)?)? ;

index	:	'index' ('unique'|'lower')* columns ;

alter	:	('create'|'delete') schema
	|	'rename' ID 'to' ID
	;
	
query	:	source op* ;

source	:	ID
	|	'history' '(' ID ')'
	|	'(' query ')'
	;

op	:	'sort' 'reverse'? cols
	|	('project'|'remove') cols
	|	'rename' (ID 'to' ID) (',' ID 'to' ID)*
	|	('join'|'leftjoin') ('by' columns)? source
	|	('union'|'times'|'difference'|'intersect') source
	|	'summarize' summary (',' summary)*
	|	'extend' extend (',' extend)*
	|	'where' expr
	;

cols	:	ID (','? ID)* ;

summary	:	ID
	|	(ID '=')? ('total'|'average'|'max'|'min'|'count'|'list') ID
	;
	
extend	:	ID '=' expr ;
	
expr	:	or ('?' expr ':' expr)? ;
or	:	and (OR and)* ;
and	:	in (AND in)* ;
in	:	bitor ('in' '(' const (','? const)* ')')? ;
bitor	:	bitxor ('|' bitxor)* ;
bitxor	:	bitand ('^' bitand)* ;
bitand	:	is ('&' is)* ;
is	:	cmp (IS cmp)* ;
cmp	:	shift (('<'|'<='|'>='|'>') shift)* ;
shift	:	add (('<<'|'>>') add)* ;
add	:	mul (('+'|'-'|'$') mul)* ;
mul	:	unary (('*'|'/'|'%') unary)* ;
unary	:	('-'|'+'|'~'|NOT)? term ;
term	:	ID
	|	const
	|	'(' expr ')'
	;
const	:	NUM
	|	STRING
	|	'[' members ']'
	|	'#' '(' members ')'
	|	'#' '{' members '}'
	;
members	:	member (',' member)* ;
member	:	const
	|	'(' members ')'
	|	'{' members '}'
	|	ID ':' member
	;

IS	:	'=='|'='|'is' ;
ISNT	:	'!='|'isnt' ;
NOT	:	'!'|'not' ;
AND	:	'&&'|'and' ;
OR	:	'||'|'or' ;

ID 	:	('a'..'z'|'A'..'Z'|'_')('a'..'z'|'A'..'Z'|'0'..'9'|'_')*('?'|'!')? ;

WS	:	(' '|'\t'|'\r'|'\n')+ {skip();} ; // ignore whitespace
COMMENT	:	'/*' .* '*/' {skip();} ; // ignore comments

STRING	:	'"' .* '"'
	|	'\'' .* '\''
	;

NUM	:	'0x' ('0'..'9'|'a'..'f'|'A'..'F')+
	|	'0' ('0'..'7')+
	|	SIGN? ('1'..'9') DIG* ('.' DIG*)? EXP?
	|	SIGN? '.' DIG+ EXP?
	;
fragment
SIGN	:	'+'|'-' ;
fragment
DIG	:	'0'..'9' ;
fragment
EXP	:	('e'|'E')('0'..'9')+ ;
