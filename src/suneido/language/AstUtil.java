/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.HashSet;
import java.util.Set;

import suneido.util.Stack;

public class AstUtil {

	// would be more efficient to stop search as soon as you know
	// but later will need to identify all shared vars
	public static boolean hasSharedVars(AstNode ast) {
		HasSharedVars hsv = new HasSharedVars(ast);
		ast.depthFirst(hsv);
		return hsv.hasSharedVars;
	}
	private static class HasSharedVars extends AstNode.Visitor {
		private final AstNode root;
		public boolean hasSharedVars;
		private int blockNest = 0; // > 0 means in block
		private final Set<String> outerVars = new HashSet<String>();
		private final Stack<Set<String>> blockParams = new Stack<Set<String>>();
		private boolean inBlockParams;
		HasSharedVars(AstNode root) {
			this.root = root;
		}
		@Override
		boolean topDown(AstNode ast) {
			switch (ast.token) {
			// don't process nested classes or functions
			case CLASS:
				return false;
			case FUNCTION:
				return ast == root ? true : false;
			case BLOCK:
				inBlockParams = true;
				blockParams.push(new HashSet<String>());
				++blockNest;
				break;
			case IDENTIFIER:
				if (blockNest == 0)
					outerVars.add(ast.value);
				else if (inBlockParams)
					blockParams.top().add(ast.value);
				else if (outerVars.contains(ast.value) &&
						! blockParams.top().contains(ast.value))
					hasSharedVars = true;
				break;
			}
			return true;
		}
		@Override
		void bottomUp(AstNode ast) {
			switch (ast.token) {
			case LIST:
				inBlockParams = false;
				break;
			case BLOCK:
				blockParams.pop();
				--blockNest;
				break;
			}
		}
	}

}
