/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

public class AstSetsDynamic {

	public static boolean check(AstNode ast) {
		Visitor v = new Visitor(ast);
		ast.depthFirst(v);
		return v.setsDynamic;
	}

	private static class Visitor extends AstNode.Visitor {
		private final AstNode root;
		public boolean setsDynamic = false;

		Visitor(AstNode root) {
			this.root = root;
		}

		@Override
		boolean topDown(AstNode ast) {
			switch (ast.token) {
			// don't process nested classes or functions
			// but do process blocks
			case CLASS:
				return false;
			case FUNCTION:
				return ast == root;
			case EQ:
				checkLvalue(ast.first());
				break;
			case ASSIGNOP:
			case PREINCDEC:
			case POSTINCDEC:
				checkLvalue(ast.second());
				break;
			}
			return setsDynamic == false;
		}

		private void checkLvalue(AstNode lvalue) {
			if (lvalue.token == Token.IDENTIFIER &&
					AstCompile.isDynamic(lvalue.value))
				setsDynamic = true;
		}

	}

}
