/* Copyright 2015 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import suneido.SuObject;
import suneido.compiler.AstNode;
import suneido.compiler.Token;
import suneido.runtime.Ops;
import suneido.runtime.Params;
import suneido.runtime.VirtualContainer;

public class AstParse {

	@Params("string")
	public static Object AstParse(Object a) {
		String src = Ops.toStr(a);
		AstNode ast = suneido.compiler.Compiler.parse(null, src);
		return new AstWrapper(ast);
	}

	private static class AstWrapper extends VirtualContainer {
		final AstNode node;
		SoftReference<SuObject> ob = new SoftReference<>(null);

		public AstWrapper(AstNode node) {
			this.node = node;
		}

		@Override
		public String typeName() {
			return "AstNode";
		}

		@Override
		protected SuObject value() {
			SuObject c = ob.get();
			if (c == null) {
				ob = new SoftReference<>(c = new SuObject());
				c.put("Token", node.token.toString());
				if (node.value != null)
					c.put("Value", handleNested(node.value));
				c.put("Line", node.lineNumber);
				if (node.children == null)
					c.put("Children", SuObject.EMPTY);
				else {
					SuObject c2 = new SuObject(node.children.size());
					int i = 0;
					for (AstNode child : node.children)
						if (child != null)
							c2.put(i++, new AstWrapper(child));
						else if (node.token == Token.FOR ||
								node.token == Token.IF)
							i++;
					c.put("Children", c2);
				}
			}
			return c;
		}

		private static Object handleNested(Object value) {
			var wrapped = nested(value);
			return wrapped == null ? value : wrapped;
		}

		// returns null if object didn't need wrapping
		// recursive to handle nested objects/classes
		private static Object nested(Object value) {
			if (value instanceof AstNode) {
				return new AstWrapper((AstNode) value);
			} else if (value instanceof SuObject) {
				var ob = (SuObject) value;
				for (int i = 0; i < ob.vecSize(); ++i) {
					var val = nested(ob.vecGet(i));
					if (val != null)
						ob.vecSet(i, val);
				}
				for (var e : ob.mapEntrySet()) {
					var val = nested(e.getValue());
					if (val != null)
						ob.putMap(e.getKey(), val);
				}
				return value;
			} else if (value instanceof Map) { // class
				@SuppressWarnings("unchecked")
				var map = (HashMap<String,Object>) value;
				var ob = new SuObject();
				for (var e : map.entrySet())
					ob.put(e.getKey(), handleNested(e.getValue()));
				return ob;
			} else
				return null;
		}

		@Override
		public String toString() {
			return ob.get() == null ? "AstNode" : super.toString();
		}
	}
}
