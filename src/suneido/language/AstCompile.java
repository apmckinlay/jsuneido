package suneido.language;

import java.io.PrintWriter;
import java.util.*;

import suneido.*;

public class AstCompile {
	private final PrintWriter pw;
	private final String globalName;
	public static final char METHOD_SEPARATOR = '\u00A3';
	private String suClassName = null;
	private boolean inMethod = false;
	private String curName = null;

	public AstCompile(String globalName) {
		this(globalName, null);
	}

	public AstCompile(String globalName, PrintWriter pw) {
		this.globalName = globalName;
		this.pw = pw;
	}

	public Object fold(AstNode ast) {
		return fold(null, ast);
	}

	/** @returns value if ast can be evaluated at compile time, otherwise null */
	public Object fold(String name, AstNode ast) {
		if (ast == null)
			return null;
		Object value;
		switch (ast.token) {
		case TRUE:
			return Boolean.TRUE;
		case FALSE:
			return Boolean.FALSE;
		case STRING:
		case SYMBOL:
			return ast.value;
		case NUMBER:
			return Ops.stringToNumber(ast.value);
		case DATE:
			return Ops.stringToDate(ast.value);
		case CONSTANT:
			return ast.extra;
		case OBJECT:
		case RECORD:
			return foldObject(ast);
		case CLASS:
			return foldClass(name, ast);
		case METHOD:
		case FUNCTION:
			return foldFunction(name, ast);
		case SUB:
			value = fold(ast.first());
			if (value != null)
				return Ops.uminus(value);
			break;
		case ADD:
			return fold(ast.first());
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
		}
		return null;
	}

	private Object foldObject(AstNode ast) {
		SuContainer c =
				ast.token == Token.OBJECT ? new SuContainer() : new SuRecord();
		for (AstNode member : ast.children) {
			AstNode name = member.first();
			Object value = fold(member.second());
			if (name == null)
				c.append(value);
			else
				c.put(fold(name), value);
		}
		c.setReadonly();
		return c;
	}

	private Object foldClass(String outerName, AstNode ast) {
		nameBegin(outerName, "$c");
		String prevSuClassName = suClassName;
		suClassName = curName;
		String base = ast.first() == null ? null : ast.first().value;
		if (base != null && base.startsWith("_"))
			base = Globals.overload(base);
		Map<String, Object> members = new HashMap<String, Object>();
		for (AstNode member : ast.second().children) {
			String name = (String) fold(member.first());
			Object value = fold(name, member.second());
			members.put(privatize2(name), value);
		}
		suClassName = prevSuClassName;
		SuClass c = new SuClass(curName.replace(METHOD_SEPARATOR, '.'), base, members);
		nameEnd();
		return c;
	}

	private String privatize(AstNode ast, String name) {
		return inMethod && ast.token == Token.SELFREF
				? privatize2(name) : name;
	}

	private String privatize2(String name) {
		if (name.startsWith("get_") &&
				name.length() > 4 && Character.isLowerCase(name.charAt(4)))
			return "Get_" + suClassName + name.substring(3);
		if (Character.isLowerCase(name.charAt(0)))
			return suClassName + "_" + name;
		return name;
	}

	public SuCallable foldFunction(String name, AstNode ast) {
		nameBegin(name, "$f");
		boolean prevInMethod = inMethod;
		inMethod = (ast.token == Token.METHOD);
		SuCallable fn = javaClass(ast, "SuFunction", "call", null, pw);
		inMethod = prevInMethod;
		nameEnd();
		return fn;
	}

	public SuCallable block(ClassGen cg, AstNode ast) {
		nameBegin(null, "$b");
		SuCallable fn = javaClass(ast, "SuCallable", "eval", cg.locals, pw);
		nameEnd();
		return fn;
	}

	private SuCallable javaClass(AstNode ast, String base, String method,
			List<String> locals, PrintWriter pw) {
		List<AstNode> params = ast.first().children;
		ClassGen cg = new ClassGen(base, curName, method, locals, pw);

		for (AstNode param : params)
			cg.param(param.value, fold(param.first()));
		if (base.equals("SuCallable") && params.isEmpty())
			cg.itParam();

		superInit(cg, ast);

		List<AstNode> statements = ast.second().children;
		for (int i = 0; i < statements.size(); ++i) {
			AstNode stat = statements.get(i);
			superChecks(i, stat);
			statement(cg, stat, null, i == statements.size() - 1);
		}
		return cg.end();
	}

	private void superChecks(int i, AstNode stat) {
		if (stat.token == Token.CALL &&
				stat.first().token == Token.SUPER &&
				stat.first().value.equals("New")) {
			onlyAllowSuperInNew(stat);
			onlyAllowSuperFirst(i, stat);
		}
	}

	private void onlyAllowSuperInNew(AstNode stat) {
		if (! curName.endsWith(METHOD_SEPARATOR + "New"))
			throw new SuException("super call only allowed in New");
	}

	private void onlyAllowSuperFirst(int i, AstNode stat) {
		if (i != 0)
			throw new SuException("call to super must come first");
	}

	/** add super init call to New methods if not explicitly called */
	private void superInit(ClassGen cg, AstNode ast) {
		if (ast.token == Token.METHOD &&
				curName.endsWith(METHOD_SEPARATOR + "New") &&
				! explicitSuperCall(ast))
			cg.superInit();
	}

	private boolean explicitSuperCall(AstNode ast) {
		List<AstNode> statements = ast.second().children;
		if (!statements.isEmpty()) {
			AstNode stat = statements.get(0);
			if (stat.token == Token.CALL &&
					stat.first().token == Token.SUPER &&
					stat.first().value.equals("New"))
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

	public static String javify(String name) {
		return name.replace('?', 'Q').replace('!', 'X');
	}

	private void nameEnd() {
		int i = Math.max(curName.lastIndexOf('$'), curName.lastIndexOf(METHOD_SEPARATOR));
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
		default:
			expression(cg, ast, last ? null : ExprOption.POP);
			if (last) {
				// special case - returning call does not do null check
				if (ast.token != Token.CALL)
					addNullCheck(cg, ast);
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

	private static void addNullCheck(ClassGen cg, AstNode ast) {
		String error = needNullCheck(cg, ast);
		if (error != null)
			cg.addNullCheck(error);
	}

	private static String needNullCheck(ClassGen cg, AstNode ast) {
		if (isLocal(ast) && !cg.neverNull(ast.value))
			return "uninitialized variable";
		else if (ast.token == Token.CALL)
			return "no return value";
		else if (ast.token == Token.Q_MARK) {
			if (null != needNullCheck(cg, ast.second())
					|| null != needNullCheck(cg, ast.third()))
				return "no value";
		}
		return null;
	}

	private static void breakStatement(ClassGen cg, AstNode ast, Labels labels) {
		if (cg.isBlock())
			cg.blockThrow("BREAK_EXCEPTION");
		else
			cg.jump(labels.brk);
	}

	private static void continueStatement(ClassGen cg, AstNode ast, Labels labels) {
		if (cg.isBlock())
			cg.blockThrow("CONTINUE_EXCEPTION");
		else
			cg.jump(labels.cont);
	}

	private void dowhileStatement(ClassGen cg, AstNode ast) {
		Labels labels = new Labels(cg);
		Object loop = cg.placeLabel();
		compound(cg, ast.first(), labels);
		cg.placeLabel(labels.cont);
		boolIntExpression(cg, ast.second());
		cg.ifTrue(loop);
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
		if (cond != null)
			cg.jump(labels.cont);
		Object loop = cg.placeLabel();
		compound(cg, body, labels); // body
		if (incr != null)
			compound(cg, incr, labels); // increment
		if (cond == null)
			cg.jump(loop);
		else {
			cg.placeLabel(labels.cont);
			boolIntExpression(cg, cond); // test
			cg.ifTrue(loop);
		}
		cg.placeLabel(labels.brk);
	}

	private void forInStatement(ClassGen cg, AstNode ast) {
		Labels labels = new Labels(cg);
		expression(cg, ast.first());
		int tmp = cg.iter();
		cg.jump(labels.cont);
		Object loop = cg.placeLabel();
		cg.next(ast.value, tmp);
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
		boolIntExpression(cg, ast.first());
		Object label = cg.ifFalse();
		compound(cg, ast.second(), labels);
		if (ast.third() != null) {
			Object skip = cg.jump();
			cg.placeLabel(label);
			compound(cg, ast.third(), labels);
			cg.placeLabel(skip);
		} else
			cg.placeLabel(label);
	}

	private void boolIntExpression(ClassGen cg, AstNode ast) {
		ExprType type = expression(cg, ast, ExprOption.INTBOOL);
		if (type != ExprType.INTBOOL)
			cg.toIntBool();
	}

	private void switchStatement(ClassGen cg, AstNode ast, Labels outerLabels) {
		Labels labels = new Labels(cg);
		if (outerLabels != null)
			labels.cont = outerLabels.cont;
		expression(cg, ast.first());
		int temp = cg.storeTemp();
		List<AstNode> cases = ast.second().children;
		if (cases.isEmpty())
			return;
		for (int i = 0; i < cases.size(); ++i) {
			AstNode c = cases.get(i);
			assert c.token == Token.CASE;
			Object caseBody = cg.label();
			Object nextCase = cg.label();
			List<AstNode> values = c.first().children;
			for (int j = 0; j < values.size(); ++j) {
				AstNode value = values.get(j);
				expression(cg, value);
				cg.loadTemp(temp);
				cg.binaryMethod(Token.IS, true);
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
		cg.thrower();
	}

	private void tryStatement(ClassGen cg, AstNode ast, Labels labels) {
		Object tc = cg.tryCatch("suneido/SuException");
		compound(cg, ast.first(), labels);
		AstNode catcher = ast.second();
		if (catcher == null)
			cg.startCatch(null, null, tc);
		else {
			String var = catcher.value;
			String pattern =
					catcher.first() == null ? null
							: Ops.toStr(fold(catcher.first()));
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
		boolIntExpression(cg, ast.first());
		cg.ifTrue(label);
		cg.placeLabel(labels.brk);
	}

	private void returnStatement(ClassGen cg, AstNode ast) {
		if (ast.first() == null) {
			cg.aconst_null();
			cg.returnValue();
		} else {
			expression(cg, ast.first());
			// special case - returning call does not do null check
			if (ast.first().token != Token.CALL)
				addNullCheck(cg, ast.first());
			cg.returnValue();
		}
	}

	private static boolean isLocal(AstNode ast) {
		return ast.token == Token.IDENTIFIER && !isGlobal(ast.value);
	}

	/** leaves a value on the stack */
	private void expression(ClassGen cg, AstNode ast) {
		expression(cg, ast, null);
	}

	private enum ExprOption {
		POP, INTBOOL
	};

	private enum ExprType {
		INTBOOL, VALUE
	};

	/** leaves a value on the stack unless ExprOption.POP */
	private ExprType expression(ClassGen cg, AstNode ast, ExprOption option) {
		Object folded = fold(ast);
		if (folded != null) {
			if (option != ExprOption.POP)
				cg.constant(folded);
			return ExprType.VALUE;
		}
		ExprType resultType = ExprType.VALUE;
		switch (ast.token) {
		case CONSTANT:
			cg.constant(ast.extra);
			break;
		case IDENTIFIER:
			String name = ast.value;
			if (isGlobal(name))
				cg.globalLoad(name);
			else
				cg.localLoad(name);
			if (option == ExprOption.POP)
				addNullCheck(cg, ast);
			break;
		case SELFREF:
			cg.localLoad("this");
			break;
		case MEMBER:
			member(cg, ast);
			cg.memberLoad();
			break;
		case SUBSCRIPT:
			expression(cg, ast.first());
			expression(cg, ast.second());
			cg.memberLoad();
			break;
		case SUB:
			expression(cg, ast.first());
			cg.unaryMethod("uminus", "Number");
			break;
		case ADD:
			expression(cg, ast.first());
			// should have a uplus, but cSuneido doesn't
			break;
		case NOT:
			expression(cg, ast.first());
			cg.unaryMethod(Token.NOT, option == ExprOption.INTBOOL);
			if (option == ExprOption.INTBOOL)
				resultType = ExprType.INTBOOL;
			break;
		case BITNOT:
			expression(cg, ast.first());
			cg.unaryMethod("bitnot", "Integer");
			break;
		case BINARYOP:
			expression(cg, ast.second());
			expression(cg, ast.third());
			cg.binaryMethod(ast.first().token, option == ExprOption.INTBOOL);
			if (option == ExprOption.INTBOOL
					&& ast.first().token.resultType == TokenResultType.B)
				resultType = ExprType.INTBOOL;
			break;
		case EQ:
			lvalue(cg, ast.first());
			expression(cg, ast.second());
			addNullCheck(cg, ast.second());
			if (option != ExprOption.POP)
				cg.dup_x2();
			store(cg, ast.first());
			return ExprType.VALUE; // skip pop handling below
		case ASSIGNOP:
			lvalue(cg, ast.second());
			cg.dup2();
			load(cg, ast.second());
			expression(cg, ast.third());
			cg.binaryMethod(ast.first().token, false);
			if (option != ExprOption.POP)
				cg.dup_x2();
			store(cg, ast.second());
			return ExprType.VALUE; // skip pop handling below
		case PREINCDEC:
			lvalue(cg, ast.second());
			cg.dup2();
			load(cg, ast.second());
			cg.unaryMethod(ast.first().token == Token.INC ? "add1" : "sub1",
					"Number");
			if (option != ExprOption.POP)
				cg.dup_x2();
			store(cg, ast.second());
			return ExprType.VALUE; // skip pop handling below
		case POSTINCDEC:
			lvalue(cg, ast.second());
			cg.dup2();
			load(cg, ast.second());
			if (option != ExprOption.POP)
				cg.dup_x2(); // original value
			cg.unaryMethod(ast.first().token == Token.INC ? "add1" : "sub1",
					"Number");
			store(cg, ast.second());
			return ExprType.VALUE; // skip pop handling below
		case CALL:
			callExpression(cg, ast);
			break;
		case NEW:
			newExpression(cg, ast);
			break;
		case AND:
			andExpression(cg, ast);
			break;
		case OR:
			orExpression(cg, ast);
			break;
		case Q_MARK:
			trinaryExpression(cg, ast);
			break;
		case BLOCK:
			int iBlockDef = cg.addConstant(block(cg, ast));
			cg.block(iBlockDef);
			break;
		case RVALUE:
			return expression(cg, ast.first(), option);
		default:
			throw new SuException("expression: unhandled: " + ast.token);
		}
		if (option == ExprOption.POP)
			cg.pop();
		return resultType;
	}

	private void member(ClassGen cg, AstNode ast) {
		expression(cg, ast.first());
		cg.constant(privatize(ast.first(), ast.value));
	}

	private void trinaryExpression(ClassGen cg, AstNode ast) {
		boolIntExpression(cg, ast.first());
		Object f = cg.ifFalse();
		expression(cg, ast.second());
		Object end = cg.jump();
		cg.placeLabel(f);
		expression(cg, ast.third());
		cg.placeLabel(end);
	}

	private void andExpression(ClassGen cg, AstNode ast) {
		Object f = cg.label();
		for (AstNode expr : ast.children) {
			boolIntExpression(cg, expr);
			cg.ifFalse(f);
		}
		cg.constant(Boolean.TRUE);
		Object end = cg.jump();
		cg.placeLabel(f);
		cg.constant(Boolean.FALSE);
		cg.placeLabel(end);
	}

	private void orExpression(ClassGen cg, AstNode ast) {
		Object t = cg.label();
		for (AstNode expr : ast.children) {
			boolIntExpression(cg, expr);
			cg.ifTrue(t);
		}
		cg.constant(Boolean.FALSE);
		Object end = cg.jump();
		cg.placeLabel(t);
		cg.constant(Boolean.TRUE);
		cg.placeLabel(end);
	}

	private void callExpression(ClassGen cg, AstNode ast) {
		AstNode fn = ast.first();
		AstNode args = ast.second();
		if (fn.token == Token.MEMBER) {
			member(cg, fn);
			int nargs = callArguments(cg, args);
			cg.invokeMethod(nargs);
		} else if (fn.token == Token.SUBSCRIPT) {
			expression(cg, fn.first());
			expression(cg, fn.second());
			cg.toMethodString();
			int nargs = callArguments(cg, args);
			cg.invokeMethod(nargs);
		} else if (fn.token == Token.SUPER) {
			cg.superCallTarget(fn.value);
			int nargs = callArguments(cg, args);
			cg.invokeSuper(nargs);
		} else if (isDirect(fn)) {
			int nargs = callArguments(cg, args);
			cg.invokeDirect(fn.value, nargs);
		} else {
			expression(cg, fn);
			int nargs = callArguments(cg, args);
			cg.invokeFunction(nargs);
		}
	}

	private static boolean isDirect(AstNode fn) {
		return fn.token == Token.IDENTIFIER && fn.value.equals("Object");
	}

	private void newExpression(ClassGen cg, AstNode ast) {
		expression(cg, ast.first());
		cg.constant("<new>");
		int nargs = callArguments(cg, ast.second());
		cg.invokeMethod(nargs);
	}

	private int callArguments(ClassGen cg, AstNode args) {
		int nargs = 0;
		if (args.token == Token.AT) {
			cg.specialArg(args.value.charAt(0) == '1' ? "EACH1" : "EACH");
			expression(cg, args.first());
			nargs = 2;
		} else {
			SuContainer constNamedArgs = new SuContainer();
			List<AstNode> args2 = splitArgs(args.children, constNamedArgs);
			for (AstNode arg : args2) {
				if (arg.extra != null) {
					cg.specialArg("NAMED");
					cg.constant(arg.extra);
					nargs += 2;
				}
				expression(cg, arg.first());
				addNullCheck(cg, arg.first());
				++nargs;
			}
			if (!constNamedArgs.isEmpty()) {
				cg.specialArg("EACH");
				cg.constant(constNamedArgs);
				nargs += 2;
			}
		}
		return nargs;
	}

	private static final int MIN_NAMED_CONST_ARGS = 10;

	private List<AstNode> splitArgs(List<AstNode> args,
			SuContainer constNamedArgs) {
		List<AstNode> args2 = new ArrayList<AstNode>();
		for (AstNode arg : args) {
			Object k = fold(arg.first());
			if (k != null)
				arg.children.set(0, AstNode.of(Token.CONSTANT, k));
			if (k != null && arg.extra != null)
				constNamedArgs.put(arg.extra, k);
			else
				args2.add(arg);
		}
		if (constNamedArgs.size() < MIN_NAMED_CONST_ARGS) {
			constNamedArgs.clear();
			return args; // return original args folded
		} else
			return args2;
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
			throw new SuException("eval: unhandled: " + token);
		}
	}

	private void lvalue(ClassGen cg, AstNode ast) {
		switch (ast.token) {
		case IDENTIFIER:
			String name = ast.value;
			if (isGlobal(name))
				throw new SuException("globals are read-only");
			else if (name.equals("this") || name.equals("super"))
				throw new SuException("this and super are read-only");
			cg.localRef(name);
			break;
		case MEMBER:
			member(cg, ast);
			break;
		case SUBSCRIPT:
			expression(cg, ast.first());
			expression(cg, ast.second());
			break;
		default:
			throw new SuException("lvalue: unhandled: " + ast.token);
		}
	}

	private static boolean isGlobal(String name) {
		int i = name.startsWith("_") ? 1 : 0;
		return Character.isUpperCase(name.charAt(i));
	}

	private static void store(ClassGen cg, AstNode ast) {
		switch (ast.token) {
		case IDENTIFIER:
			cg.localStore();
			break;
		case MEMBER:
		case SUBSCRIPT:
			cg.memberStore();
			break;
		default:
			throw new SuException("store: unhandled: " + ast.token);
		}
	}

	/** lvalue is already on stack */
	private static void load(ClassGen cg, AstNode ast) {
		switch (ast.token) {
		case IDENTIFIER:
			if (isGlobal(ast.value))
				cg.globalLoad();
			else
				cg.localLoad();
			break;
		case MEMBER:
		case SUBSCRIPT:
			cg.memberLoad();
			break;
		default:
			throw new SuException("store: unhandled: " + ast.token);
		}
	}

	private static class Labels {
		Object brk;
		Object cont;

		Labels(ClassGen cg) {
			brk = cg.label();
			cont = cg.label();
		}
	}

	public static void main(String[] args) {
		Lexer lexer = new Lexer("class { M() { } }");
		PrintWriter pw = new PrintWriter(System.out);
		AstGenerator generator = new AstGenerator();
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		AstNode ast = pc.parse();
		System.out.println(ast);
		Object x = new AstCompile("Test", pw).fold(ast);
//		System.out.println(x);
System.out.println(((SuClass) x).toDebugString());
	}

}
