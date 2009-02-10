grammar Query;

options {
	language = Java;
}
 
@header {
package suneido.database.query;

import suneido.*;
import suneido.database.query.expr.*;
import suneido.database.server.ServerData;
import static suneido.database.Database.theDB; // used for view expansion
}
@lexer::header {
package suneido.database.query;
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
	public ServerData serverData = null; // for sview
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

top_query returns [Query result]
	: query EOF
		{ $result = $query.result; }
	;
	
query returns [Query result]
	: query2 sort[$query2.result]?
    	{ $result = $sort.result == null ? $query2.result : $sort.result; }
    | insert
    	{ $result = $insert.result; }
    | INSERT query2 INTO id
    	{ $result = new InsertQuery($query2.result, $id.text); }
    | update
    	{ $result = $update.result; }
    | DELETE query2
    	{ $result = new Delete($query2.result); }
	;
	
query2 returns [Query result]
	scope { Query source; }
	: source 
		{ $query2::source = $source.result; }
	  (op 
	  	{ $query2::source = $op.result; } 
	  )*
    	{ $result = $query2::source; } 
	;

sort[Query source] returns [Query result]
	: SORT r=REVERSE ? cols
		 { $result = new Sort($source, $r != null, $cols.list); }
	;

insert returns [Query result]
	scope { SuRecord record; }
	@init { $insert::record = new SuRecord(); }
	: INSERT ('{'|'['|'(') field (','? field )* ('}'|']'|')') INTO query2
		{ $result = new InsertRecord($query2.result, $insert::record); }
	;
field
	:	id ':' constant
			{ $insert::record.putdata($id.text, $constant.value); }
	;

update returns [Query result]
	scope { List<String> fields; List<Expr> exprs; }
	@init { $update::fields = new ArrayList<String>();
			$update::exprs = new ArrayList<Expr>(); }
	: UPDATE query2 SET set (',' set )*
		{ $result = new Update($query2.result, $update::fields, $update::exprs); } 
	;
set
	: id '=' expr
		{ $update::fields.add($id.text); $update::exprs.add($expr.expr); } 
	;
  
source returns [Query result]
	: id
		{			
		String def = serverData.getSview($id.text);
		if (def == null) 
			def = theDB.getView($id.text);
			
		if (def == null || serverData.inView($id.text))
			$result = new Table($id.text);
		else {
			try {
				serverData.enterView($id.text);
				$result = ParseQuery.parse(serverData, def);
			} finally {
				serverData.leaveView($id.text);
			}
		} 
		}
    | HISTORY '(' id ')'
    	{ $result = new History($id.text); }
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
	: f=id TO t=id
		{ $rename::froms.add($f.text); $rename::tos.add($t.text); }
	;
    
cols returns [List<String> list]  
	@init { list = new ArrayList<String>(); }
	: i=id { list.add($i.text); } 
		(','? j=id { list.add($j.text); }
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
	: id
		{ $summarize::by.add($id.text); }
    | (id eq='=')? COUNT
    	{
    	$summarize::cols.add($eq == null ? null : $id.text);
    	$summarize::funcs.add("count");
 		$summarize::on.add(null);
    	}
    | (c=id eq='=')? f=(TOTAL|AVERAGE|MAX|MIN|LIST) o=id
    	{
    	$summarize::cols.add($eq == null ? null : $c.text);
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
	: id (a='=' expr)?
		{ if ($a == null) $extend::rules.add($id.text);
			else { $extend::fields.add($id.text); $extend::exprs.add($expr.expr); } } 
	;
  
// for testing
expression returns [Expr expression]  
	: expr EOF
		{ $expression = $expr.expr; }
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
	  ('|' y=bitxor
		{ $expr = new BinOp(BinOp.Op.BITOR, $expr, $y.expr); } 
	  )* ;
bitxor returns [Expr expr]  
	: x=bitand 
   		{ $expr = $x.expr; } 
	  ('^' y=bitand
		{ $expr = new BinOp(BinOp.Op.BITXOR, $expr, $y.expr); } 
	  )* ;
bitand returns [Expr expr]  
	: x=is 
   		{ $expr = $x.expr; } 
	  ('&' y=is
		{ $expr = new BinOp(BinOp.Op.BITAND, $expr, $y.expr); } 
	  )* ;
is returns [Expr expr]  
    : x=cmp 
   		{ $expr = $x.expr; } 
      (isop y=cmp
		{ $expr = new BinOp($isop.op, $expr, $y.expr); } 
      )* ;
cmp returns [Expr expr]  
   : x=shift 
   		{ $expr = $x.expr; } 
     (cmpop y=shift
		{ $expr = new BinOp($cmpop.op, $expr, $y.expr); } 
     )* ;
shift returns [Expr expr]  
	: x=add 
   		{ $expr = $x.expr; } 
	  (shiftop y=add
		{ $expr = new BinOp($shiftop.op, $expr, $y.expr); } 
	  )* ;
add returns [Expr expr]  
   : x=mul
   		{ $expr = $x.expr; } 
     (addop y=mul
		{ $expr = new BinOp($addop.op, $expr, $y.expr); } 
     )* ;
mul returns [Expr expr]  
   : x=unary
   		{ $expr = $x.expr; }
     (mulop y=unary
		{ $expr = new BinOp($mulop.op, $expr, $y.expr); } 
     )* ;
unary returns [Expr expr]  
	: unop? term
		{ $expr = ($unop.op == null) ? $term.expr : new UnOp($unop.op, $term.expr); }
    | constant
    	{ $expr = Constant.valueOf($constant.value); }
	;
term returns [Expr expr]
	: id
		{ $expr = Identifier.valueOf($text); }
	| id '('
		{ $expr = new FunCall($id.text); } 
	  ( e1=expr
	  	{ ((FunCall) $expr).add($e1.expr); }
	  (',' e2=expr
	  	{ ((FunCall) $expr).add($e2.expr); }
	  )* )? ')'
	| TRUE
		{ $expr = Constant.TRUE; }
	| FALSE
		{ $expr = Constant.FALSE; }
    | '(' expr ')'
    	{ $expr = $expr.expr; }
    ;
id	:	ID
	|	SET
	;
constant returns [SuValue value]
	: '-'? NUM
		{ $value = SuNumber.valueOf($text); }
    | STR
    	{ $value = SuString.literal($text); }
    | '#' id
    	{ $value = Symbols.symbol($id.text); }
    | DATE
    	{ 
    	$value = SuDate.literal($text);
    	if ($value == null)
    		throw new SuException("invalid date: " + $text); 
    	}
	| '#' '(' members[new SuContainer()] ')'
		{ $value = $members.value; }
    | '#' ? '[' members[new SuRecord()] ']'
		{ $value = $members.value; }
    | '#' '{' members[new SuRecord()] '}'
		{ $value = $members.value; }
    ;
members[SuContainer ob] returns [SuValue value]
	: member[ob] (',' member[ob])*
		{ $value = $ob; }
	;
member[SuContainer ob]
	: membervalue
		{ $ob.append($membervalue.value); }
    | id ':' membervalue
  		{ $ob.putdata($id.text, $membervalue.value); }
	;
membervalue returns [SuValue value]
	: constant
		{ $value = $constant.value; }
    | '(' members[new SuContainer()] ')'
		{ $value = $members.value; }
    | '{' members[new SuRecord()] '}'
		{ $value = $members.value; }
    ;
    
isop returns [BinOp.Op op]
	: (IS|'=') { $op = BinOp.Op.IS; } 
	| ISNT { $op = BinOp.Op.ISNT; }
	| '=~' { $op = BinOp.Op.MATCH; }
	| '!~' { $op = BinOp.Op.MATCHNOT; }
	;
cmpop returns [BinOp.Op op]
	: '<' { $op = BinOp.Op.LT; }
	| '<=' { $op = BinOp.Op.LTE; }
	| '>' { $op = BinOp.Op.GT; }
	| '>=' { $op = BinOp.Op.GTE; } 
	;
shiftop returns [BinOp.Op op]
	: '<<' { $op = BinOp.Op.LSHIFT; }
	| '>>' { $op = BinOp.Op.RSHIFT; }
	;
addop returns [BinOp.Op op]
	: '+' { $op = BinOp.Op.ADD; }
	| '-' { $op = BinOp.Op.SUB; }
	| '$' { $op = BinOp.Op.CAT; }
	;
mulop returns [BinOp.Op op]
	: '*' { $op = BinOp.Op.MUL; }
	| '/' { $op = BinOp.Op.DIV; }
	| '%' { $op = BinOp.Op.MOD; }
	;
unop returns [UnOp.Op op]
	: '+' { $op = UnOp.Op.PLUS; }
	| '-' { $op = UnOp.Op.MINUS; }
	| NOT { $op = UnOp.Op.NOT; }
	| '~' { $op = UnOp.Op.BITNOT; }
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
INSERT	: ('i'|'I')('n'|'N')('s'|'S')('e'|'E')('r'|'R')('t'|'T') ;
INTO	: ('i'|'I')('n'|'N')('t'|'T')('o'|'O') ;
UPDATE	: ('u'|'U')('p'|'P')('d'|'D')('a'|'A')('t'|'T')('e'|'E') ;
SET		: ('s'|'S')('e'|'E')('t'|'T') ;
DELETE	: ('d'|'D')('e'|'E')('l'|'L')('e'|'E')('t'|'T')('e'|'E') ;
TRUE	: ('t'|'T')('r'|'R')('u'|'U')('e'|'E') ;
FALSE	: ('f'|'F')('a'|'A')('l'|'L')('s'|'S')('e'|'E') ;

ID		: ('a'..'z'|'A'..'Z'|'_')('a'..'z'|'A'..'Z'|'0'..'9'|'_')*('?'|'!')? ;

fragment
YMD	: '#' ('1'..'2')('0'..'9')('0'..'9')('0'..'9')('0'..'9')('0'..'9')('0'..'9')('0'..'9') ;
fragment
HM	: ('0'..'9')('0'..'9')('0'..'9')('0'..'9') ;
DATE	: YMD
		| YMD '.' HM (('0'..'9')('0'..'9')(('0'..'9')('0'..'9')('0'..'9'))?)?
		;
		
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
EXP   : ('e'|'E')('0'..'9')+ ;
