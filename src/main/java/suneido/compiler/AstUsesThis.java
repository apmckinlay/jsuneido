/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

/**
 * Determine if a function references "this" or "super"
 * Used by {@link AstCompile} to choose between eval and call
 */
public class AstUsesThis {

	public static boolean check(AstNode ast) {
		Visitor v = new Visitor(ast);
		ast.depthFirst(v);
		return v.usesThis;
	}

	private static class Visitor extends AstNode.Visitor {
		private final AstNode root;
		public boolean usesThis = false;

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
			case SELFREF:
			case SUPER:
				usesThis = true;
				break;
			case IDENTIFIER:
				if ("this".equals(ast.value) || "super".equals(ast.value))
					usesThis = true;
				break;
			default:
			}
			return usesThis == false;
		}

	}

}
