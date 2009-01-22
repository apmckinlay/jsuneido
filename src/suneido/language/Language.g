grammar Language;

options {
	language = Java;
}

@header {
package suneido.language;
}
@lexer::header { 
package suneido.language; 
}

@members {
}
@lexer::members {
	private String escape(String s, int radix) {
		return String.valueOf((char) Integer.parseInt(s.substring(1), radix));
	}
}

constant
	: '-'? NUM
	| STR
	| DATE
	;

AND			: 'and' ;
BOOL		: 'bool' ;
BREAK		: 'break' ;
BUFFER		: 'buffer' ;
CALLBACK	: 'callback' ;
CASE		: 'case' ;
CATCH		: 'catch' ;
CHAR		: 'char' ;
CLASS		: 'class' ;
CONTINUE	: 'continue' ;
DEFAULT		: 'default' ;
DLL			: 'dll' ;
DO			: 'do' ;
DOUBLE		: 'double' ;
ELSE		: 'else' ;
FALSE		: 'false' ;
FLOAT		: 'float' ;
FOR			: 'for' ;
FOREACH		: 'foreach' ;
FOREVER		: 'forever' ;
FUNCTION	: 'function' ;
GDIOBJ		: 'gdiobj' ;
HANDLE		: 'handle' ;
IF			: 'if' ;
IN			: 'in' ;
INT64		: 'int64' ;
IS			: 'is' ;
ISNT		: 'isnt' ;
LIST		: 'list' ;
LONG		: 'long' ;
NEW			: 'new' ;
NOT			: 'not' ;
OR			: 'or' ;
RESOURCE	: 'resource' ;
RETURN		: 'return' ;
SHORT		: 'short' ;
STRING		: 'string' ;
STRUCT		: 'struct' ;
SUPER		: 'super' ;
SWITCH		: 'switch' ;
THIS		: 'this' ;
THROW		: 'throw' ;
TRUE		: 'true' ;
TRY			: 'try' ;
VALUE		: 'value' ;
VOID		: 'void' ;
WHILE		: 'while' ;

ID		: ('a'..'z'|'A'..'Z'|'_')('a'..'z'|'A'..'Z'|'0'..'9'|'_')*('?'|'!')? ;

DATE	: YMD
		| YMD '.' HM (('0'..'9')('0'..'9')(('0'..'9')('0'..'9')('0'..'9'))?)?
		;
fragment
YMD	: '#' ('1'..'2')('0'..'9')('0'..'9')('0'..'9')('0'..'9')('0'..'9')('0'..'9')('0'..'9') ;
fragment
HM	: ('0'..'9')('0'..'9')('0'..'9')('0'..'9') ;
		
WS    : (' '|'\t'|'\r'|'\n')+ {skip();} ; // ignore whitespace
COMMENT : '/*' .* '*/' {skip();} ; // ignore comments

STR	: '"' ( ESCAPE | ~('"'|'\\') )* '"'
		{ String s = getText(); setText(s.substring(1, s.length() - 1)); }
	| '\'' ( ESCAPE | ~('\''|'\\') )* '\''
		{ String s = getText(); setText(s.substring(1, s.length() - 1)); }
	;
fragment
ESCAPE : '\\' . ;
    
NUM : '0x' HEX +
    | '0' OCT *
    | SIGN? ('1'..'9') DIG* ('.' DIG*)? EXP?
    | SIGN? '.' DIG+ EXP?
    ;
fragment
HEX : '0'..'9'|'a'..'f'|'A'..'F' ;
fragment
OCT : '0'..'7' ;
fragment
SIGN  : '+'|'-' ;
fragment
DIG   : '0'..'9' ;
fragment
EXP   : ('e'|'E')('0'..'9')+ ;
