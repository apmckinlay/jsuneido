/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

/**
 * Determine if a block body uses an "it" variable
 */
public class AstBlockIt {

	public static boolean check(AstNode ast) {
		Visitor hsv = new Visitor();
		ast.depthFirst(hsv);
		return hsv.usesIt;
	}

	private static class Visitor extends AstNode.Visitor {
		public boolean usesIt = false;

		@Override
		boolean topDown(AstNode ast) {
			switch (ast.token) {
			// don't process nested classes or functions or blocks
			case CLASS:
			case FUNCTION:
			case BLOCK:
				return false;
			case IDENTIFIER:
				if ("it".equals(ast.value))
					usesIt = true;
				break;
			default:
			}
			return usesIt == false;
		}

	}

}
