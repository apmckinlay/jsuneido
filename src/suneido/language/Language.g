grammar Language;

options {
	language = Java;
	backtrack = true;
	k = 3;
}

@header {
package suneido.language;

import suneido.*;
}
@lexer::header {
package suneido.language;
import suneido.SuException;
}

@members {
	protected void mismatch(IntStream input, int ttype, BitSet follow)
		throws RecognitionException {
		throw new MismatchedTokenException(ttype, input);
	}
	public void recoverFromMismatchedSet(IntStream input, 
		RecognitionException e, BitSet follow)
		throws RecognitionException {
		throw e;
	}
}
@rulecatch {
	catch (RecognitionException e) {
		throw e;
	}
}

@lexer::members {
	@Override
	public void reportError(RecognitionException e) {
		Thrower.sneakyThrow(e);
	}
	
	// See: How can I make the lexer exit upon first lexical error?
	// http://www.antlr.org/wiki/pages/viewpage.action?pageId=5341217
	static class Thrower {
		private static Throwable t;
		private Thrower() throws Throwable {
			throw t;
		}
		public static synchronized void sneakyThrow(Throwable t) {
			Thrower.t = t;
			try {
				Thrower.class.newInstance();
			} catch (InstantiationException e) {
				throw new IllegalArgumentException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			} finally {
				Thrower.t = null; // Avoid memory leak
			}
		}
	}
}

top_constant returns [SuValue result]
	: constant EOF
		{ $result = $constant.result; }
	;

constant returns [SuValue result]
	: '-'? NUM
		{ $result = SuNumber.valueOf($text); }
    | STR
    	{ $result = SuString.literal($text); }
    | '#' ID
    	{ $result = Symbols.symbol($ID.text); }
    | DATE
    	{ 
    	$result = SuDate.literal($text);
    	if ($result == null)
    		throw new SuException("invalid date: " + $text); 
    	}
	| FUNCTION '(' ')' compound
		{ $result = SuString.literal("FUNCTION"); }
	;

statement
	: (NL | ';')* stmt
	;
stmt
	: expr
	| compound
	| IF NL* expr NL* statement
	| RETURN expr?
		{ System.out.println($text); }
	;
	
compound
	: '{' statement* (NL | ';')* '}'
	;
	
expr : triop ;

triop : term ( '?' triop ':' triop )? ;

term
	: '(' expr ')'
	| constant
	| ID
	| ID '=' constant
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
		
NL	: '\n' ;
WS	: (' '|'\t'|'\r')+ { skip(); } ; // ignore whitespace
COMMENT : '/*' .* '*/' { skip(); } ; // ignore comments

STR	: '"' ( ESCAPE | ~('"'|'\\') )* '"'
		{ String s = getText(); setText(s.substring(1, s.length() - 1)); }
	| '\'' ( ESCAPE | ~('\''|'\\') )* '\''
		{ String s = getText(); setText(s.substring(1, s.length() - 1)); }
	;
fragment
ESCAPE : '\\' . ;
    
NUM : '0x' HEX +
    | '0' OCT *
    | ('1'..'9') DIG* ('.' DIG*)? EXP?
    | '.' DIG+ EXP?
    ;
fragment
HEX : '0'..'9'|'a'..'f'|'A'..'F' ;
fragment
OCT : '0'..'7' ;
fragment
DIG   : '0'..'9' ;
fragment
EXP   : ('e'|'E')('+'|'-')?('0'..'9')+ ;
