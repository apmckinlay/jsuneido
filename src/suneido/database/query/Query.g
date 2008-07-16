grammar Query;

options {
	language = Java;
}
 
@header {
package suneido.database.query;

import suneido.database.query.expr.*;
import suneido.SuNumber;
import suneido.SuString;
import suneido.SuValue;
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
		 { $result = new Sort($source, $r != null, $cols.list); }
	;

query2 returns [Query result]
	scope { Query source; }
	: source { $query2::source = $source.result; } op*
    	{ $result = $op.result == null ? $source.result : $op.result; } 
	;

source returns [Query result]
	: ID
		{ $result = new Table($ID.text); }
    | HISTORY '(' ID ')'
    	{ $result = new History($ID.text); }
    | '(' query2 ')'
    	{ $result = $query2.result; }
    ;

op returns [Query result]
	: (p=PROJECT|REMOVE) cols
		{ $result = new Project($query2::source, $cols.list, $p == null); }
    | rename
    	{ $result = $rename.result; }
    | JOIN (BY '(' cols ')')? source
    	{ $result = new Join($query2::source, $source.result, $cols.list); }
    | LEFTJOIN (BY '(' cols ')')? source
    	{ $result = new LeftJoin($query2::source, $source.result, $cols.list); }
    | UNION source
    	{ $result = new Union($query2::source, $source.result); }
    | TIMES source
    	{ $result = new Product($query2::source, $source.result); }
    | MINUS source
     	{ $result = new Difference($query2::source, $source.result); }
    | INTERSECT source
     	{ $result = new Intersect($query2::source, $source.result); }
    | summarize
    	{ $result = $summarize.result; }
    | extend
    	{ $result = $extend.result; }
    | WHERE expr
    	{ $result = new Select($query2::source, $expr.expr); }
    ;
    
rename returns [Query result]
	scope { List<String> froms; List<String> tos; }
	@init { $rename::froms = new ArrayList<String>(); 
		$rename::tos = new ArrayList<String>(); }
	: RENAME rename1 (',' rename1)*
		{ $result = new Rename($query2::source, $rename::froms, $rename::tos); }
	;
    
rename1
	: f=ID TO t=ID
		{ $rename::froms.add($f.text); $rename::tos.add($t.text); }
	;
    
cols returns [List<String> list]  
	@init { list = new ArrayList<String>(); }
	: i=ID { list.add($i.text); } 
		(','? j=ID { list.add($j.text); }
		)* ;
		
summarize returns [Query result]
	scope { List<String> by; List<String> cols;
			List<String> funcs; List<String> on; }
	@init { $summarize::by = new ArrayList<String>(); 
			$summarize::cols = new ArrayList<String>(); 
			$summarize::funcs = new ArrayList<String>(); 
			$summarize::on = new ArrayList<String>(); }
	: SUMMARIZE summary (',' summary)*
		{ $result = new Summarize($query2::source, $summarize::by, 
			$summarize::cols, $summarize::funcs, $summarize::on); }
	;
summary 
	: ID
		{ $summarize::by.add($ID.text); }
    | (ID '=')? COUNT
    	{
    	$summarize::cols.add($ID == null ? null : $ID.text);
    	$summarize::funcs.add("count");
 		$summarize::on.add(null);
    	}
    | (c=ID '=')? f=(TOTAL|AVERAGE|MAX|MIN|LIST) o=ID
    	{
    	$summarize::cols.add($c == null ? null : $c.text);
    	$summarize::funcs.add($f.text);
		$summarize::on.add($o.text);
    	}
    ;

extend returns [Query result]
	scope { List<String> fields; List<Expr> exprs; List<String> rules; }
	@init { $extend::fields = new ArrayList<String>();
			$extend::exprs = new ArrayList<Expr>();
			$extend::rules = new ArrayList<String>(); }
	: EXTEND extend1 (',' extend1 )*
		{ $result = new Extend($query2::source, $extend::fields, $extend::exprs, 
			$extend::rules); } 
	;

extend1
	: ID (a='=' expr)?
		{ if ($a == null) $extend::rules.add($ID.text);
			else { $extend::fields.add($ID.text); $extend::exprs.add($expr.expr); } } 
	;
  
expr returns [Expr expr]  
	: or
		{ $expr = $or.expr; } 
	  ('?' x=expr ':' y=expr
	  	{ $expr = new TriOp($expr, $x.expr, $y.expr); }
	  )?;
or returns [Expr expr]  
    : x=and
    	{ $expr = $x.expr; }
      (OR y=and 
    	{ $expr = new Or().add($x.expr).add($y.expr); }
      (OR z=and
      	{ ((Multi) $expr).add($z.expr); }
      )*)? ;
and returns [Expr expr]  
	: x=in
    	{ $expr = $x.expr; }
	  (AND y=in
		{ $expr = new And().add($x.expr).add($y.expr); }
	  (AND z=in
      	{ ((Multi) $expr).add($z.expr); }
	  )*)? ;
in returns [Expr expr]  
    : bitor
    	{ $expr = $bitor.expr; } 
      ('in' '(' x=constant
      	{ $expr = new In($expr).add($x.value); } 
      (','? y=constant
      	{ ((In) $expr).add($y.value); }
      )* ')' )? ;
bitor returns [Expr expr]  
	: x=bitxor 
   		{ $expr = $x.expr; } 
	  (o='|' y=bitxor
		{ $expr = new BinOp($o.text, $expr, $y.expr); } 
	  )* ;
bitxor returns [Expr expr]  
	: x=bitand 
   		{ $expr = $x.expr; } 
	  (o='^' y=bitand
		{ $expr = new BinOp($o.text, $expr, $y.expr); } 
	  )* ;
bitand returns [Expr expr]  
	: x=is 
   		{ $expr = $x.expr; } 
	  (o='&' y=is
		{ $expr = new BinOp($o.text, $expr, $y.expr); } 
	  )* ;
is returns [Expr expr]  
    : x=cmp 
   		{ $expr = $x.expr; } 
      (o=(IS|'=') y=cmp
		{ $expr = new BinOp($o.text, $expr, $y.expr); } 
      )* ;
cmp returns [Expr expr]  
   : x=shift 
   		{ $expr = $x.expr; } 
     (o=('<'|'<='|'>='|'>') y=shift
		{ $expr = new BinOp($o.text, $expr, $y.expr); } 
     )* ;
shift returns [Expr expr]  
	: x=add 
   		{ $expr = $x.expr; } 
	  (o=('<<'|'>>') y=add
		{ $expr = new BinOp($o.text, $expr, $y.expr); } 
	  )* ;
add returns [Expr expr]  
   : x=mul
   		{ $expr = $x.expr; } 
     (o=('+'|'-'|'$') y=mul
		{ $expr = new BinOp($o.text, $expr, $y.expr); } 
     )* ;
mul returns [Expr expr]  
   : x=unary
   		{ $expr = $x.expr; }
     (o=('*'|'/'|'%') y=unary
		{ $expr = new BinOp($o.text, $expr, $y.expr); } 
     )* ;
unary returns [Expr expr]  
	: o=('-'|'+'|'~'|NOT)? term
		{ $expr = $o == null ? $term.expr : new UnOp($o.text, $term.expr); } 
	;
term returns [Expr expr]
	: ID
		{ $expr = Identifier.valueOf($text); }
	| ID '('
		{ $expr = new FunCall($ID.text); } 
	  ( e1=expr
	  	{ ((FunCall) $expr).add($e1.expr); }
	  (',' e2=expr
	  	{ ((FunCall) $expr).add($e2.expr); }
	  )* )? ')'
    | constant
    	{ $expr = new Constant($constant.value); }
    | '(' expr ')'
    	{ $expr = $expr.expr; }
    ;
constant returns [SuValue value]  
	: NUM
		{ $value = SuNumber.valueOf($text); }
    | STRING
    	{ $value = new SuString($text); }
    | '[' members ']'
    | '#' '(' members ')'
    | '#' '{' members '}'
    ;
members : member (',' member)* ;
member  
	: constant
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


