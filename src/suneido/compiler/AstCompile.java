/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import static suneido.compiler.ClassGen.javify;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.Label;

import suneido.SuException;
import suneido.SuInternalError;
import suneido.SuObject;
import suneido.runtime.*;

/**
 * Compiles an AST (from parsing) into Java classes
 */
public class AstCompile {
	private final PrintWriter pw;
	private final String library;
	private final String globalName;
	private final String sourceFile;
	private final String sourceCode;
	/* British pound sign - needs to be valid in identifiers */
	public static final char METHOD_SEPARATOR = '\u00A3';
	private static final int MAX_DIRECT_ARGS = 4;
	private String suClassName = null;
	private SuClass suClass = null;
	private boolean inMethod = false;
	private String curName = null;
	private static final AtomicInteger nextFnId = new AtomicInteger();
	private int fnId = -1;
	private final ContextLayered context;
	private final boolean wantLineNumbers;

	public static Object fold(String library, String globalName, String src,
			PrintWriter pw, ContextLayered context, SuObject warnings,
			boolean wantLineNumbers, AstNode ast) {
		return new AstCompile(library, globalName, src, pw, context, warnings,
				wantLineNumbers).fold(ast);
	}

	private AstCompile(String library, String globalName, String src,
			PrintWriter pw, ContextLayered context, SuObject warnings,
			boolean wantLineNumbers) {
		this.library = library;
		this.globalName = globalName;
		this.sourceCode = src;
		this.sourceFile = library.isEmpty() ? "<" + globalName + ">"
				: "library[" + library + "]->" + globalName;
		this.pw = pw;
		this.context = context;
		this.wantLineNumbers = wantLineNumbers;
	}

	private Object fold(AstNode ast) {
		return fold(null, ast);
	}

	/**
	 * Evaluate constant expressions at compile time.
	 * @returns value if ast can be evaluated at compile time, otherwise null
	 */
	private Object fold(String name, AstNode ast) {
		if (ast == null)
			return null;
		Object value;
		switch (ast.token) {
		case VALUE:
			return ast.value;
		case OBJECT:
			return foldObject(ast);
		case CLASS:
			return foldClass(name, ast);
		case METHOD:
			return foldFunction(name, ast, CallableType.METHOD);
		case FUNCTION:
			return foldFunction(name, ast, CallableType.FUNCTION);
		case SUB: // unary
			value = fold(ast.first());
			if (value != null)
				return Ops.uminus(value);
			break;
		case ADD: // unary
			value = fold(ast.first());
			if (value != null)
				return Ops.uplus(value);
			break;
		case AND:
			for (AstNode expr : ast.children) {
				value = fold(expr);
				if (value == Boolean.FALSE)
					return Boolean.FALSE;
			}
			break;
		case OR:
			for (AstNode expr : ast.children) {
				value = fold(expr);
				if (value == Boolean.TRUE)
					return Boolean.TRUE;
			}
			break;
		case NOT:
			value = fold(ast.first());
			if (value != null)
				return Ops.not(value);
			break;
		case BITNOT:
			value = fold(ast.first());
			if (value != null)
				return Ops.bitnot(value);
			break;
		case BINARYOP:
			Object left = fold(ast.second());
			Object right = fold(ast.third());
			if (left != null && right != null)
				return evalBinary(ast.first().token, left, right);
			break;
		default:
		}
		return null;
	}

	private Object foldObject(AstNode ast) {
		SuObject c = (SuObject) ast.value;
		for (int i = 0; i < c.vecSize(); ++i) {
			Object e = c.vec.get(i);
			if (e instanceof AstNode)
				c.vec.set(i, fold((AstNode) e));
		}
		for (var e : c.mapEntrySet()) {
			if (e.getKey() instanceof AstNode)
				throw new SuException("object member names must be scalar");
			if (e.getValue() instanceof AstNode)
				c.put(e.getKey(), fold((AstNode) e.getValue()));
		}
		c.setReadonly();
		return c;
	}

	private Object foldClass(String outerName, AstNode ast) {
		nameBegin(outerName, "$c");
		String base = ast.first() == null ? null : ast.first().strval();
		if (base != null && base.startsWith("_"))
			base = context.overload(base);
		@SuppressWarnings("unchecked")
		Map<String, Object> members = (Map<String, Object>) ast.second().value;
		SuClass prevSuClass = suClass;
		SuClass c = suClass = new SuClass(library, curName, base, members);
		String prevSuClassName = suClassName;
		suClassName = ast.strval();
		for (var e : members.entrySet())
			if (e.getValue() instanceof AstNode) {
				var name = e.getKey();
				members.put(e.getKey(), fold(name, (AstNode) e.getValue()));
			}
		suClassName = prevSuClassName;
		suClass = prevSuClass;
		nameEnd();
		return c;
	}

	private SuCompiledCallable foldFunction(String name, AstNode ast,
			CallableType callableType) {
		int prevFnId = fnId;
		fnId = nextFnId.incrementAndGet();
		boolean prevInMethod = inMethod;
		inMethod = CallableType.METHOD == callableType;
		SuCompiledCallable fn = function(name, ast, callableType);
		inMethod = prevInMethod;
		fnId = prevFnId;
		return fn;
	}

	/** used for functions, methods, and blocks that don't need to be closures */
	private SuCompiledCallable function(String name, AstNode ast,
			CallableType callableType) {
		boolean isEval = CallableType.METHOD == callableType
				|| AstUsesThis.check(ast);
		nameBegin(name, callableType.compilerNameSuffix());
		SuCompiledCallable fn = javaClass(ast, isEval ? BaseClassSet.EVALBASE
				: BaseClassSet.CALLBASE, callableType, null);
		nameEnd();
		return fn;
	}

	private void block(ClassGen cg, AstNode ast) {
		if (ast.third() == null) {
			// Third will be null where a previous call to AstSharesVars.check()
			// by this.closure(...) did detect a closure.
			SuCompiledCallable f = function(null, ast, CallableType.BLOCK);
			cg.constant(f);
		} else {
			assert Token.CLOSURE == ast.third().token;
			// Third will be Token.CLOSURE where a previous call to
			// AstSharesVars.check() by this.closure(...) found a closure and
			// replaced the null that was originally inserted by the parser.
			int iBlockDef = cg.addConstant(closure(cg, ast));
			List<AstNode> params = ast.first().children;
			final int nParams = params.size();
			boolean useArgsArray = false;
			if ((nParams > MAX_DIRECT_ARGS) || (nParams > 0 && params.get(0).strval().startsWith("@")))
				useArgsArray = true;
			cg.wrapBlockWithClosure(iBlockDef, nParams, useArgsArray);
		}
		cg.addBlockReturnCatcher();
	}

	private SuCompiledCallable closure(ClassGen cg, AstNode ast) {
		// needed to check if child blocks share with this block
		AstSharesVars.check(ast); // Has side-effects: may modify ast
		nameBegin(null, CallableType.WRAPPED_BLOCK.compilerNameSuffix());
		SuCompiledCallable fn = javaClass(ast, BaseClassSet.EVALBASE,
				CallableType.WRAPPED_BLOCK, cg.locals);
		nameEnd();
		return fn;
	}

	/**
	 * Used by foldFunction and block
	 *
	 * @param locals
	 *            The outer locals for a block. Not used for functions.
	 */
	private SuCompiledCallable javaClass(AstNode ast, BaseClassSet baseClassSet,
			CallableType callableType, List<String> locals) {
		List<AstNode> params = ast.first().children;
		ClassGen cg = new ClassGen(context, baseClassSet, curName, locals,
				useArgsArray(ast, callableType, params), callableType,
				params.size(), fnId, sourceFile, pw);
		putLineNumber(cg, ast);

		for (AstNode param : params)
			cg.param(param.strval(), fold(param.first()), inMethod ? suClassName
					+ "_" : "");

		superInit(cg, ast);

		if (AstSetsDynamic.check(ast))
			cg.addDynamicPushPop();

		List<AstNode> statements = ast.second().children;
		for (int i = 0; i < statements.size(); ++i) {
			AstNode stat = statements.get(i);
			superChecks(i, stat);
			statement(cg, stat, null, i == statements.size() - 1);
		}
		try {
			return cg.end(suClass).setSource(library, globalName, sourceCode);
		} catch (Error e) {
			throw new SuException("error compiling " + curName, e);
		}
	}

	private static boolean useArgsArray(AstNode ast, CallableType callableType,
			List<AstNode> params) {
		if (CallableType.WRAPPED_BLOCK == callableType) // closure block
			return true;
		// need to call this regardless to process child blocks
		boolean shares = AstSharesVars.check(ast);
		if (params.size() > MAX_DIRECT_ARGS)
			return true;
		if (params.size() > 0 && params.get(0).strval().startsWith("@"))
			return true;
		return shares;
	}

	private void superChecks(int i, AstNode stat) {
		if (stat.token == Token.CALL && stat.first().token == Token.SUPER
				&& stat.first().strval().equals("New")) {
			onlyAllowSuperInNew(stat);
			onlyAllowSuperFirst(i, stat);
		}
	}

	private void onlyAllowSuperInNew(AstNode stat) {
		if (!curName.endsWith(METHOD_SEPARATOR + "New"))
			throw new SuException("super call only allowed in New");
	}

	private static void onlyAllowSuperFirst(int i, AstNode stat) {
		if (i != 0)
			throw new SuException("super(...) must be first statement");
	}

	/** add super init call to New methods if not explicitly called */
	private void superInit(ClassGen cg, AstNode ast) {
		if (ast.token == Token.METHOD
				&& curName.endsWith(METHOD_SEPARATOR + "New")
				&& !explicitSuperCall(ast))
			cg.superInit();
	}

	private static boolean explicitSuperCall(AstNode ast) {
		List<AstNode> statements = ast.second().children;
		if (!statements.isEmpty()) {
			AstNode stat = statements.get(0);
			if (stat.token == Token.CALL && stat.first().token == Token.SUPER
					&& stat.first().strval().equals("New"))
				return true;
		}
		return false;
	}

	private void nameBegin(String memberName, String def) {
		if (curName == null)
			curName = javify(globalName);
		else if (memberName == null)
			curName += def;
		else
			curName += METHOD_SEPARATOR + javify(memberName);
	}

	private void nameEnd() {
		int i = Math.max(curName.lastIndexOf('$'),
				curName.lastIndexOf(METHOD_SEPARATOR));
		curName = i == -1 ? "" : curName.substring(0, i);
	}

	private void compound(ClassGen cg, AstNode ast, Labels labels) {
		if (ast == null)
			return;
		if (ast.token == Token.LIST)
			for (AstNode statement : ast.children)
				statement(cg, statement, labels);
		else
			statement(cg, ast, labels);
	}

	private void statement(ClassGen cg, AstNode ast, Labels labels) {
		statement(cg, ast, labels, false);
	}

	private void statement(ClassGen cg, AstNode ast, Labels labels, boolean last) {
		switch (ast.token) {
		case NIL:
			break;
		case BREAK:
			breakStatement(cg, ast, labels);
			break;
		case CONTINUE:
			continueStatement(cg, ast, labels);
			break;
		case DO:
			dowhileStatement(cg, ast);
			break;
		case FOR:
			forStatement(cg, ast);
			break;
		case FOR_IN:
			forInStatement(cg, ast);
			break;
		case FOREVER:
			foreverStatement(cg, ast);
			break;
		case IF:
			ifStatement(cg, ast, labels);
			break;
		case RETURN:
			returnStatement(cg, ast);
			return; // skip last handling below
		case SWITCH:
			switchStatement(cg, ast, labels);
			break;
		case THROW:
			throwStatement(cg, ast);
			break;
		case TRY:
			tryStatement(cg, ast, labels);
			break;
		case WHILE:
			whileStatement(cg, ast);
			break;
		case LIST:
			compound(cg, ast, labels);
			break;
		default:
			expression(cg, ast, last ? null : ExprOption.POP);
			if (last) {
				returnNullCheck(cg, ast);
				cg.areturn();
			}
			return; // skip last handling below
		}
		// default is return null
		if (last) {
			cg.aconst_null();
			cg.areturn();
		}
	}

	private static void returnNullCheck(ClassGen cg, AstNode expr) {
		expr = stripRvalue(expr);
		// special case - returning call or ?: does not do null check
		if (expr.token != Token.CALL && expr.token != Token.Q_MARK)
			addNullCheck(cg, expr);
	}

	private static void addNullCheck(ClassGen cg, AstNode ast) {
		String error = needNullCheck(cg, ast);
		if (error != null)
			cg.addNullCheck(error);
	}

	private static String needNullCheck(ClassGen cg, AstNode ast) {
		ast = stripRvalue(ast);
		if (isLocal(ast) && !cg.neverNull(ast.strval()))
			return "UninitializedVariable";
		else if (ast.token == Token.CALL)
			return "NoReturnValue";
		else if (ast.token == Token.Q_MARK) {
			if (null != needNullCheck(cg, ast.second())
					|| null != needNullCheck(cg, ast.third()))
				return "NoValue";
		}
		return null;
	}

	private static AstNode stripRvalue(AstNode expr) {
		while (expr.token == Token.RVALUE)
			expr = expr.first();
		return expr;
	}

	private void breakStatement(ClassGen cg, AstNode ast, Labels labels) {
		putLineNumber(cg, ast);
		if (labels != null)
			cg.jump(labels.brk);
		else
			cg.blockThrow("BREAK_EXCEPTION");
	}

	private void continueStatement(ClassGen cg, AstNode ast,
			Labels labels) {
		putLineNumber(cg, ast);
		if (labels != null && labels.cont != null) // switch sets cont = null
			cg.jump(labels.cont);
		else
			cg.blockThrow("CONTINUE_EXCEPTION");
	}

	private void dowhileStatement(ClassGen cg, AstNode ast) {
		Labels labels = new Labels(cg);
		Object loop = cg.placeLabel();
		compound(cg, ast.first(), labels);
		cg.placeLabel(labels.cont);
		whileExpr(cg, loop, ast.second());
		cg.placeLabel(labels.brk);
	}

	private void forStatement(ClassGen cg, AstNode ast) {
		final AstNode init = ast.first();
		final AstNode cond = ast.second();
		final AstNode incr = ast.third();
		final AstNode body = ast.fourth();

		Labels labels = new Labels(cg);
		if (init != null)
			compound(cg, init, labels);
		Label start = null;
		if (cond != null)
			start = cg.jump();
		Label loop = cg.placeLabel();
		compound(cg, body, labels); // body
		cg.placeLabel(labels.cont);
		if (incr != null)
			compound(cg, incr, labels); // increment
		if (cond == null)
			cg.jump(loop);
		else {
			cg.placeLabel(start);
			whileExpr(cg, loop, cond); // condition
		}
		cg.placeLabel(labels.brk);
	}

	private void forInStatement(ClassGen cg, AstNode ast) {
		Labels labels = new Labels(cg);
		expression(cg, ast.first());
		int tmp = cg.iter();
		cg.jump(labels.cont);
		Object loop = cg.placeLabel();
		cg.next(ast.strval(), tmp);
		compound(cg, ast.second(), labels);
		cg.placeLabel(labels.cont);
		cg.hasNext(tmp);
		cg.ifTrue(loop);
		cg.placeLabel(labels.brk);
	}

	private void foreverStatement(ClassGen cg, AstNode ast) {
		Labels labels = new Labels(cg);
		labels.cont = cg.placeLabel();
		compound(cg, ast.first(), labels);
		cg.jump(labels.cont);
		cg.placeLabel(labels.brk);
	}

	private void ifStatement(ClassGen cg, AstNode ast, Labels labels) {
		Label ifFalse = cg.label();
		ifExpr(cg, ifFalse, ast.first());
		compound(cg, ast.second(), labels);
		if (ast.third() != null) {
			Label skip = cg.jump();
			cg.placeLabel(ifFalse);
			compound(cg, ast.third(), labels);
			cg.placeLabel(skip);
		} else
			cg.placeLabel(ifFalse);
	}

	private void ifExpr(ClassGen cg, Object ifFalse, AstNode expr) {
		if (expr.token == Token.AND) {
			for (AstNode e : expr.children) {
				boolIntExpression(cg, e);
				cg.ifFalse(ifFalse);
			}
		} else if (expr.token == Token.OR) {
			Label ifTrue = cg.label();
			int n = expr.children.size();
			for (int i = 0; i < n; ++i) {
				AstNode e = expr.children.get(i);
				boolIntExpression(cg, e);
				if (i < n - 1)
					cg.ifTrue(ifTrue);
				else
					// last one
					cg.ifFalse(ifFalse);
			}
			cg.placeLabel(ifTrue);
		} else {
			boolIntExpression(cg, expr);
			cg.ifFalse(ifFalse);
		}
	}

	private void boolIntExpression(ClassGen cg, AstNode ast) {
		ExprType type = expression(cg, ast, ExprOption.INTBOOL);
		if (type != ExprType.INTBOOL)
			cg.toIntBool();
	}

	private void switchStatement(ClassGen cg, AstNode ast, Labels outerLabels) {
		Labels labels = new Labels(cg);
		if (outerLabels == null)
			labels.cont = null;
		else
			labels.cont = outerLabels.cont;
		expression(cg, ast.first());
		int temp = cg.storeTemp();
		List<AstNode> cases = ast.second().children;
		if (cases.isEmpty())
			return;
		for (int i = 0; i < cases.size(); ++i) {
			AstNode c = cases.get(i);
			assert c.token == Token.CASE;
			Label caseBody = cg.label();
			Label nextCase = cg.label();
			List<AstNode> values = c.first().children;
			for (int j = 0; j < values.size(); ++j) {
				AstNode value = values.get(j);
				expression(cg, value);
				cg.loadTemp(temp);
				cg.binaryOp(Token.IS, true);
				if (j == values.size() - 1)
					cg.ifFalse(nextCase);
				else
					cg.ifTrue(caseBody);
			}
			if (!values.isEmpty())
				cg.placeLabel(caseBody);
			compound(cg, c.second(), labels);
			if (i != cases.size() - 1)
				cg.jump(labels.brk);
			cg.placeLabel(nextCase);
		}
		cg.placeLabel(labels.brk);
	}

	private void throwStatement(ClassGen cg, AstNode ast) {
		expression(cg, ast.first());
		putLineNumber(cg, ast);
		cg.thrower();
	}

	private void tryStatement(ClassGen cg, AstNode ast, Labels labels) {
		Object tc = cg.tryCatch("java/lang/Throwable");
		compound(cg, ast.first(), labels);
		AstNode catcher = ast.second();
		if (catcher == null)
			cg.startCatch(null, null, tc);
		else {
			String var = catcher.strval();
			String pattern = (catcher.first() == null)
					? null : Ops.toStr(fold(catcher.first()));
			cg.startCatch(var, pattern, tc);
			compound(cg, catcher.second(), labels);
		}
		cg.endCatch(tc);
	}

	private void whileStatement(ClassGen cg, AstNode ast) {
		Labels labels = new Labels(cg);
		cg.jump(labels.cont);
		Object label = cg.placeLabel();
		compound(cg, ast.second(), labels);
		cg.placeLabel(labels.cont);
		whileExpr(cg, label, ast.first());
		cg.placeLabel(labels.brk);
	}

	/** opposite of ifExpr */
	private void whileExpr(ClassGen cg, Object ifTrue, AstNode expr) {
		if (expr.token == Token.OR) {
			for (AstNode e : expr.children) {
				boolIntExpression(cg, e);
				cg.ifTrue(ifTrue);
			}
		} else if (expr.token == Token.AND) {
			Label ifFalse = cg.label();
			int n = expr.children.size();
			for (int i = 0; i < n; ++i) {
				AstNode e = expr.children.get(i);
				boolIntExpression(cg, e);
				if (i < n - 1)
					cg.ifFalse(ifFalse);
				else
					// last one
					cg.ifTrue(ifTrue);
			}
			cg.placeLabel(ifFalse);
		} else {
			boolIntExpression(cg, expr);
			cg.ifTrue(ifTrue);
		}
	}

	private void returnStatement(ClassGen cg, AstNode ast) {
		AstNode expr = ast.first();
		if (expr == null) {
			putLineNumber(cg, ast);
			cg.aconst_null();
			cg.returnValue();
		} else {
			expression(cg, expr);
			putLineNumber(cg, ast);
			returnNullCheck(cg, expr);
			cg.returnValue();
		}
	}

	private static boolean isLocal(AstNode ast) {
		return ast.token == Token.IDENTIFIER && !isGlobal(ast.strval());
	}

	/** leaves a value on the stack */
	private void expression(ClassGen cg, AstNode ast) {
		expression(cg, ast, null);
	}

	private enum ExprOption {
		POP, INTBOOL
	}

	private enum ExprType {
		INTBOOL, VALUE
	}

	/** leaves a value on the stack unless ExprOption.POP */
	private ExprType expression(ClassGen cg, AstNode ast, ExprOption option) {
		Object folded = fold(ast);
		if (folded != null) {
			if (option != ExprOption.POP) {
				putLineNumber(cg, ast);
				cg.constant(folded);
			}
			return ExprType.VALUE;
		}
		int ref;
		ExprType resultType = ExprType.VALUE;
		switch (ast.token) {
		case IDENTIFIER:
			identifier(cg, ast, option);
			break;
		case SELFREF:
			putLineNumber(cg, ast);
			cg.localLoad("this");
			break;
		case MEMBER:
			member(cg, ast);
			cg.memberLoad();
			break;
		case SUBSCRIPT:
			expression(cg, ast.first());
			AstNode r = ast.second();
			switch (r.token) {
			case RANGETO:
			case RANGELEN:
				expression(cg, r.first());
				expression(cg, r.second());
				if (r.token == Token.RANGETO)
					cg.rangeTo();
				else
					cg.rangeLen();
				break;
			default:
				expression(cg, ast.second());
				cg.memberLoad();
			}
			break;
		case SUB:
			expression(cg, ast.first());
			cg.unaryOp("uminus", "Number");
			break;
		case ADD:
			expression(cg, ast.first());
			cg.unaryOp("uplus", "Number");
			break;
		case NOT:
			expression(cg, ast.first());
			cg.unaryOp(Token.NOT, option == ExprOption.INTBOOL);
			if (option == ExprOption.INTBOOL)
				resultType = ExprType.INTBOOL;
			break;
		case BITNOT:
			expression(cg, ast.first());
			cg.unaryOp("bitnot", "Integer");
			break;
		case BINARYOP:
			expression(cg, ast.second());
			expression(cg, ast.third());
			cg.binaryOp(ast.first().token, option == ExprOption.INTBOOL);
			if (option == ExprOption.INTBOOL
					&& ast.first().token.resultType == TokenResultType.B)
				resultType = ExprType.INTBOOL;
			break;
		case EQ:
			ref = lvalue(cg, ast.first());
			expression(cg, ast.second());
			addNullCheck(cg, ast.second());
			if (option != ExprOption.POP)
				cg.dupUnderLvalue(ref);
			store(cg, ref);
			return ExprType.VALUE; // skip pop handling below
		case ASSIGNOP:
			ref = lvalue(cg, ast.second());
			cg.dupLvalue(ref);
			load(cg, ref);
			expression(cg, ast.third());
			cg.binaryOp(ast.first().token, false);
			if (option != ExprOption.POP)
				cg.dupUnderLvalue(ref);
			store(cg, ref);
			return ExprType.VALUE; // skip pop handling below
		case PREINCDEC:
			ref = lvalue(cg, ast.second());
			cg.dupLvalue(ref);
			load(cg, ref);
			cg.unaryOp(ast.first().token == Token.INC ? "add1" : "sub1",
					"Number");
			if (option != ExprOption.POP)
				cg.dupUnderLvalue(ref);
			store(cg, ref);
			return ExprType.VALUE; // skip pop handling below
		case POSTINCDEC:
			ref = lvalue(cg, ast.second());
			cg.dupLvalue(ref);
			load(cg, ref);
			if (option != ExprOption.POP)
				cg.dupUnderLvalue(ref); // original value
			cg.unaryOp(ast.first().token == Token.INC ? "add1" : "sub1",
					"Number");
			store(cg, ref);
			return ExprType.VALUE; // skip pop handling below
		case CALL:
			callExpression(cg, ast);
			break;
		case NEW:
			newExpression(cg, ast);
			break;
		case AND:
			andExpression(cg, ast, option == ExprOption.INTBOOL);
			if (option == ExprOption.INTBOOL)
				resultType = ExprType.INTBOOL;
			break;
		case OR:
			orExpression(cg, ast, option == ExprOption.INTBOOL);
			if (option == ExprOption.INTBOOL)
				resultType = ExprType.INTBOOL;
			break;
		case Q_MARK:
			trinaryExpression(cg, ast);
			break;
		case BLOCK:
			block(cg, ast);
			break;
		case RVALUE:
			return expression(cg, ast.first(), option);
		case IN:
			inExpression(cg, ast, option == ExprOption.INTBOOL);
			if (option == ExprOption.INTBOOL)
				resultType = ExprType.INTBOOL;
			break;
		default:
			throw SuInternalError.unhandledEnum(ast.token);
		}
		if (option == ExprOption.POP)
			cg.pop();
		return resultType;
	}

	private void identifier(ClassGen cg, AstNode ast, ExprOption option) {
		putLineNumber(cg, ast);
		String name = ast.strval();
		if (isOverload(name))
			cg.constant(context.get(context.slotForName(name.substring(1))));
		else if (isGlobal(name))
			cg.globalLoad(context.slotForName(name));
		else if (isDynamic(name))
			cg.dynamicLoad(name);
		else
			cg.localLoad(name);
		if (option == ExprOption.POP)
			addNullCheck(cg, ast);
	}

	static boolean isOverload(String name) {
		return name.startsWith("_") && isGlobal(name.substring(1));
	}

	static boolean isDynamic(String name) {
		return name.startsWith("_");
	}

	private void member(ClassGen cg, AstNode ast) {
		expression(cg, ast.first());
		putLineNumber(cg, ast);
		cg.constant(privatizeRef(ast.first(), ast.strval()));
	}

	// privatize .name member references in code
	private String privatizeRef(AstNode ast, String name) {
		if (inMethod && ast.token == Token.SELFREF &&
				Character.isLowerCase(name.charAt(0))) {
			return suClassName + "_" + name;
		}
		return name;
	}

	private void trinaryExpression(ClassGen cg, AstNode ast) {
		Label ifFalse = cg.label();
		ifExpr(cg, ifFalse, ast.first());
		expression(cg, ast.second());
		Label end = cg.jump();
		cg.placeLabel(ifFalse);
		expression(cg, ast.third());
		cg.placeLabel(end);
	}

	private void inExpression(ClassGen cg, AstNode ast, boolean intBool) {
		if (ast.second() == null) {
			cg.bool(false, intBool);
			return;
		}
		expression(cg, ast.first());
		int temp = cg.storeTemp();
		Label t = cg.label();
		for (AstNode value : ast.second().children) {
			expression(cg, value);
			cg.loadTemp(temp);
			cg.binaryOp(Token.IS, true);
			cg.ifTrue(t);
		}
		cg.bool(false, intBool);
		Label end = cg.jump();
		cg.placeLabel(t);
		cg.bool(true, intBool);
		cg.placeLabel(end);
	}

	private void andExpression(ClassGen cg, AstNode ast, boolean intBool) {
		Label f = cg.label();
		for (AstNode expr : ast.children) {
			boolIntExpression(cg, expr);
			cg.ifFalse(f);
		}
		cg.bool(true, intBool);
		Label end = cg.jump();
		cg.placeLabel(f);
		cg.bool(false, intBool);
		cg.placeLabel(end);
	}

	private void orExpression(ClassGen cg, AstNode ast, boolean intBool) {
		Label t = cg.label();
		for (AstNode expr : ast.children) {
			boolIntExpression(cg, expr);
			cg.ifTrue(t);
		}
		cg.bool(false, intBool);
		Label end = cg.jump();
		cg.placeLabel(t);
		cg.bool(true, intBool);
		cg.placeLabel(end);
	}

	private void callExpression(ClassGen cg, AstNode ast) {
		AstNode fn = ast.first();
		AstNode args = ast.second();
		if (fn.token == Token.MEMBER) {
			if ("New".equals(fn.strval()))
				throw new SuException("can't explicitly call New method");
			member(cg, fn);
			if (args.token != Token.AT
					&& args.children.size() <= MAX_DIRECT_ARGS
					&& !hasNamed(args)) {
				directArguments(cg, args);
				putLineNumber(cg, ast);
				cg.invokeMethod(args.children.size());
			} else {
				callArguments(cg, args);
				putLineNumber(cg, ast);
				cg.invokeMethod();
			}
		} else if (fn.token == Token.SUBSCRIPT) {
			expression(cg, fn.first());
			expression(cg, fn.second());
			cg.toMethodString();
			if (args.token != Token.AT
					&& args.children.size() <= MAX_DIRECT_ARGS
					&& !hasNamed(args)) {
				directArguments(cg, args);
				putLineNumber(cg, ast);
				cg.invokeMethod(args.children.size());
			} else {
				callArguments(cg, args);
				putLineNumber(cg, ast);
				cg.invokeMethod();
			}
		} else if (fn.token == Token.SUPER) {
			cg.superCallTarget(fn.strval());
			callArguments(cg, args);
			putLineNumber(cg, ast);
			cg.invokeSuper();
		} else if (isDirect(fn)) {
			// PERF if args are all constant e.g. [a: 1, b: 'fred']
			// then call a copy function (or better copy-on-write)
			callArguments(cg, args);
			putLineNumber(cg, ast);
			String f = fn.strval();
			if (f.equals("["))
				f = hasUnnamed(args) ? "Object" : "Record";
			cg.invokeDirect(f);
		} else if (isGlobal(fn)) {
			cg.pushThis();
			cg.iconst(context.slotForName(fn.strval()));
			if (args.token != Token.AT
					&& args.children.size() <= MAX_DIRECT_ARGS
					&& !hasNamed(args)) {
				directArguments(cg, args);
				putLineNumber(cg, ast);
				cg.invokeGlobal(args.children.size());
			} else {
				callArguments(cg, args);
				putLineNumber(cg, ast);
				cg.invokeGlobal();
			}
		} else {
			expression(cg, fn);
			if (args.token != Token.AT
					&& args.children.size() <= MAX_DIRECT_ARGS
					&& !hasNamed(args)) {
				directArguments(cg, args);
				putLineNumber(cg, ast);
				cg.callFunction(args.children.size());
			} else {
				callArguments(cg, args);
				putLineNumber(cg, ast);
				cg.callFunction();
			}
		}
	}

	/** Helper calls to Object and Record */
	private static boolean isDirect(AstNode fn) {
		return fn.token == Token.IDENTIFIER && (fn.value.equals("[") ||
				fn.value.equals("Object") || fn.value.equals("Record"));
	}

	private static boolean hasUnnamed(AstNode args) {
		return args.children.size() > 0 && args.first().first() == null;
	}

	private static boolean isGlobal(AstNode fn) {
		return fn.token == Token.IDENTIFIER
				&& Character.isUpperCase(fn.strval().charAt(0));
	}

	private class VarArgs {
		int i = 0;
		ClassGen cg;

		VarArgs(ClassGen cg, int nargs) {
			this.cg = cg;
			cg.anewarray(nargs);
		}

		void constant(Object constant) {
			cg.dup();
			cg.iconst(i++);
			cg.constant(constant);
			cg.aastore();
		}

		void special(String which) {
			cg.dup();
			cg.iconst(i++);
			cg.specialArg(which);
			cg.aastore();
		}

		void expression(AstNode expr) {
			cg.dup();
			cg.iconst(i++);
			AstCompile.this.expression(cg, expr);
			addNullCheck(cg, expr);
			cg.aastore();
		}

		void named(AstNode arg) {
			if (arg.first() != null)
				named(fold(arg.first()));
			expression(arg.second());
		}

		void named(Object name) {
			special("NAMED");
			constant(name);
		}
	}

	private void newExpression(ClassGen cg, AstNode ast) {
		expression(cg, ast.first());
		cg.constant("<new>");
		callArguments(cg, ast.second());
		putLineNumber(cg, ast);
		cg.invokeMethod();
	}

	private void callArguments(ClassGen cg, AstNode args) {
		if (args.token == Token.AT)
			atArgument(cg, args);
		else if (args.children.size() < MIN_TO_OPTIMIZE)
			simpleArguments(cg, args);
		else
			optimizeArguments(cg, args);
	}

	private static final int MIN_TO_OPTIMIZE = 10;

	private void atArgument(ClassGen cg, AstNode args) {
		VarArgs vargs = new VarArgs(cg, 2);
		vargs.special(args.strval().charAt(0) == '1' ? "EACH1" : "EACH");
		vargs.expression(args.first());
	}

	private static boolean hasNamed(AstNode args) {
		for (AstNode arg : args.children)
			if (arg.first() != null)
				return true;
		return false;
	}

	private void simpleArguments(ClassGen cg, AstNode args) {
		namedArgs(new VarArgs(cg, countArgs(args)), args.children);
	}

	private void directArguments(ClassGen cg, AstNode args) {
		for (AstNode arg : args.children) {
			AstNode expr = arg.second();
			expression(cg, expr);
			addNullCheck(cg, expr);
		}
	}

	private static int countArgs(AstNode args) {
		int nargs = 0;
		for (AstNode arg : args.children) {
			if (arg.first() != null)
				nargs += 2;
			++nargs;
		}
		return nargs;
	}

	/**
	 * If there are at least MIN_TO_OPTIMIZE constant arguments then put them in
	 * an SuContainer and pass them with EACH
	 */
	private void optimizeArguments(ClassGen cg, AstNode args) {
		SuObject constArgs = new SuObject();
		List<Object> unnamed = new ArrayList<>();
		List<AstNode> named = new ArrayList<>();
		splitArgs(args, unnamed, named, constArgs);
		pushArgs(cg, unnamed, named, constArgs);
	}

	private void splitArgs(AstNode args, List<Object> unnamed,
			List<AstNode> named, SuObject constArgs) {
		for (AstNode arg : args.children) {
			Object name = fold(arg.first());
			Object value = fold(arg.second());
			if (name == null) {
				if (value != null)
					constArgs.add(value);
				else {
					unnamed.addAll(constArgs.vec);
					constArgs.vec.clear();
					unnamed.add(arg.second());
				}
			} else { // named
				if (value == null)
					named.add(arg);
				else
					constArgs.put(name, value);
			}
		}
	}

	private void pushArgs(ClassGen cg, List<Object> unnamed,
			List<AstNode> named, SuObject constArgs) {
		int nargs = unnamed.size() + 3 * named.size();
		if (constArgs.size() >= MIN_TO_OPTIMIZE) {
			nargs += 2; // EACH constArgs
			VarArgs vargs = new VarArgs(cg, nargs);
			unamedArgs(vargs, unnamed);
			namedArgs(vargs, named);
			vargs.special("EACH");
			vargs.constant(constArgs);
		} else {
			nargs += constArgs.vecSize() + 3 * constArgs.mapSize();
			VarArgs vargs = new VarArgs(cg, nargs);
			unamedArgs(vargs, unnamed);
			unamedArgs(vargs, constArgs.vec);
			namedArgs(vargs, named);
			for (Map.Entry<Object, Object> e : constArgs.mapEntrySet()) {
				vargs.named(e.getKey());
				vargs.constant(e.getValue());
			}
		}
	}

	private static void namedArgs(VarArgs vargs, List<AstNode> args) {
		for (AstNode arg : args)
			vargs.named(arg);
	}

	private static void unamedArgs(VarArgs vargs, List<Object> args) {
		for (Object arg : args)
			if (arg instanceof AstNode)
				vargs.expression((AstNode) arg);
			else
				vargs.constant(arg);
	}

	private static Object evalBinary(Token token, Object left, Object right) {
		switch (token) {
		case CAT:
			return Ops.cat(left, right);
		case ADD:
			return Ops.add(left, right);
		case SUB:
			return Ops.sub(left, right);
		case MUL:
			return Ops.mul(left, right);
		case DIV:
			return Ops.div(left, right);
		case MOD:
			return Ops.mod(left, right);
		case LSHIFT:
			return Ops.lshift(left, right);
		case RSHIFT:
			return Ops.rshift(left, right);
		case BITOR:
			return Ops.bitor(left, right);
		case BITAND:
			return Ops.bitand(left, right);
		case BITXOR:
			return Ops.bitxor(left, right);
		case IS:
			return Ops.is(left, right);
		case ISNT:
			return Ops.isnt(left, right);
		case LT:
			return Ops.lt(left, right);
		case LTE:
			return Ops.lte(left, right);
		case GT:
			return Ops.gt(left, right);
		case GTE:
			return Ops.gte(left, right);
		case MATCH:
			return Ops.match(left, right);
		case MATCHNOT:
			return Ops.matchnot(left, right);
		default:
			throw SuInternalError.unhandledEnum(token);
		}
	}

	private int lvalue(ClassGen cg, AstNode ast) {
		switch (ast.token) {
		case IDENTIFIER:
			putLineNumber(cg, ast);
			String name = ast.strval();
			if (isGlobal(name))
				throw new SuException("globals are read-only");
			else if (isDynamic(name))
				return cg.dynamicRef(name);
			else if (name.equals("this") || name.equals("super"))
				throw new SuException("this and super are read-only");
			else
				return cg.localRef(name);
		case MEMBER:
			member(cg, ast);
			return ClassGen.MEMBER_REF;
		case SUBSCRIPT:
			AstNode sub = ast.second();
			if (sub.token == Token.RANGETO || sub.token == Token.RANGELEN)
				throw new SuException("ranges are read-only");
			expression(cg, ast.first());
			expression(cg, sub);
			return ClassGen.MEMBER_REF;
		default:
			throw SuInternalError.unhandledEnum(ast.token);
		}
	}

	private static boolean isGlobal(String name) {
		if (name.isEmpty())
			return false;
		int i = name.startsWith("_") && name.length() > 1 ? 1 : 0;
		return Character.isUpperCase(name.charAt(i));
	}

	private static void store(ClassGen cg, int ref) {
		if (ref == ClassGen.MEMBER_REF)
			cg.memberStore();
		else if (ref == ClassGen.DYNAMIC_REF)
			cg.dynamicStore();
		else
			cg.localRefStore(ref);
	}

	/** lvalue is already on stack */
	private static void load(ClassGen cg, int ref) {
		if (ref == ClassGen.MEMBER_REF)
			cg.memberLoad();
		else if (ref == ClassGen.DYNAMIC_REF)
			cg.dynamicLoad();
		else
			cg.localRefLoad(ref);
	}

	private static class Labels {
		Label brk;
		Label cont;

		Labels(ClassGen cg) {
			brk = cg.label();
			cont = cg.label();
		}
	}

	private void putLineNumber(ClassGen cg, AstNode ast) {
		if (wantLineNumbers) {
			cg.putLineNumber(ast.lineNumber);
		}
	}
}
