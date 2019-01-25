package suneido.util;

/**
 * Exploratory implementation, not used
 * Simplest possible version, no attempt to minimize storage space
 */
public class RadixTree {
	Node root = new Node();

	public void put(String key, Object val) {
		var node = root;
		for (var i = 0; i < key.length(); ++i) {
			var c = key.charAt(i);
			var x = node.children[c];
			if (x == null) {
				x = node.children[c] = new Node();
			}
			node = x;
		}
		node.value = val;
	}

	public Object get(String key) {
		var node = root;
		for (var i = 0; i < key.length(); ++i) {
			var c = key.charAt(i);
			var x = node.children[c];
			if (x == null)
				return null;
			node = x;
		}
		return node.value;
	}

	/** returns the old value, or null if not found */
	public Object del(String key) {
		return del(root, key);
	}
	private Object del(Node node, String key) {
		if (key.length() == 0) {
			if (node.value == null)
				return null; // not found
			var val = node.value;
			node.value = null;
			return val;
		}
		var c = key.charAt(0);
		var x = node.children[c];
		var val = del(x, key.substring(1)); // recurse
		if (x.empty())
			node.children[c] = null;
		return val;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		toString(sb, "", 0, root);
		return sb.toString();
	}

	private void toString(StringBuilder sb, String indent, int c, Node node) {
		sb.append(indent);
		if (c == 0)
			sb.append("node");
		else
			sb.append((char) c);
		if (node.value != null)
			sb.append(" ").append(node.value);
		sb.append("\n");
		indent += "    ";
		for (var i = 0; i < node.children.length; ++i) {
			if (node.children[i] != null)
				toString(sb, indent, i, node.children[i]);
		}
	}

	private static class Node {
		Object value;
		Node children[] = new Node[128];

		private boolean empty() {
			if (value != null)
				return false;
			for (var i = 0; i < children.length; ++i)
				if (children[i] != null)
					return false;
			return true;
		}
	}

	public static void main(String[] args) {
		var rt = new RadixTree();
		String[] data = { "foo", "bar", "fat", "by", "", "funny", "a", "fun" };
		for (var i = 0; i < data.length; ++i) {
			rt.put(data[i], i);
			for (var j = 0; j < data.length; ++j) {
				if (j <= i)
					assert rt.get(data[j]).equals(j);
				else
					assert rt.get(data[j]) == null;
			}
		}
		System.out.println(rt);
		for (var i = 0; i < data.length; ++i) {
			assert rt.del(data[i]).equals(i);
		}
		System.out.println(rt);
	}
}
