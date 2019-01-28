package suneido.util;

/**
 * Exploratory implementation, not used
 * Simplest possible version, no attempt to minimize storage space
 * except for prefix compression
 */
public class RadixTree {
	Node root = new Node();

	public void put(String key, Object val) {
		var node = root;
		for (var i = 0; i < key.length(); ++i) {
			if (node.empty()) {
				node.prefix = key.substring(i);
				node.value = val;
				return;
			}
			if (key.startsWith(node.prefix, i))
				i += node.prefix.length();
			else {
				// split
				var prefix = node.prefix;
				// move the contents of the current node to a new node
				var newNode = node.moveToNew();
				// split the prefix
				var cp = commonPrefix(prefix, key.substring(i));
				node.prefix = prefix.substring(0, cp);
				newNode.prefix = substr(prefix, cp+1);
				// insert a pointer to the new node
				node.children[prefix.charAt(cp)] = newNode;
				i += cp;
				// fall through
			}
			if (i >= key.length()) {
				node.value = val;
				return;
			}
			var c = key.charAt(i);
			var x = node.children[c];
			if (x == null)
				x = node.children[c] = new Node();
			node = x;
		}
		if (!node.prefix.isEmpty()) {
			var newNode = new Node();
			newNode.prefix = substr(node.prefix, 1);
			newNode.value = node.value;
			node.children[node.prefix.charAt(0)] = newNode;
			node.prefix = "";
		}
		node.value = val;
	}

	private static int commonPrefix(String s, String t) {
		int i = 0;
		for (; i < s.length() && i < t.length(); ++i)
			if (s.charAt(i) != t.charAt(i))
				break;
		return i;
	}

	private static String substr(String s, int i) {
		return i >= s.length() ? "" : s.substring(i);
	}

	public Object get(String key) {
		var node = root;
		for (var i = 0; ; ++i) {
			if (!key.startsWith(node.prefix, i))
				return null;
			i += node.prefix.length();
			if (i >= key.length())
				break;
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
		if (!key.startsWith(node.prefix))
			return null; // not found
		key = key.substring(node.prefix.length());
		if (key.length() == 0) {
			if (node.value == null)
				return null; // not found
			var val = node.value;
			node.value = null;
			return val;
		}
		var c = key.charAt(0);
		var x = node.children[c];
		if (x == null)
			return null; // not found
		var val = del(x, key.substring(1)); // recurse
		if (x.empty())
			node.children[c] = null;
		return val;
	}

	@Override
	public String toString() {
		return root.toString();
	}

	private static class Node {
		Object value;
		String prefix = "";
		Node children[] = new Node[128];

		private boolean empty() {
			if (value != null)
				return false;
			for (var i = 0; i < children.length; ++i)
				if (children[i] != null)
					return false;
			return true;
		}
		private Node moveToNew() {
			Node node = new Node();
			var tmp = node.children;
			node.children = children;
			children = tmp;
			node.value = value;
			value = null;
			prefix = node.prefix = "";
			return node;
		}
		@Override
		public String toString() {
			var sb = new StringBuilder();
			toString(sb, "", 0, this);
			return sb.toString();
		}
		private static void toString(StringBuilder sb, String indent, int c, Node node) {
			sb.append(indent);
			if (c == 0)
				sb.append("node");
			else
				sb.append((char) c).append(":");
			if (!node.prefix.isEmpty())
				sb.append(" ").append(node.prefix);
			if (node.value != null)
				sb.append(" = ").append(node.value);
			sb.append("\n");
			indent += "    ";
			for (var i = 0; i < node.children.length; ++i) {
				if (node.children[i] != null)
					toString(sb, indent, i, node.children[i]); // recurse
			}
		}

	}

	public static void main(String[] args) {
		var rt = new RadixTree();
		//String[] data = { "foo", "fat", "bars", "by", "", "bar", "funny", "a", "fun" };
		for (var i = 0; i < data.length; ++i) {
			//System.out.println("ADD " + data[i]);
			rt.put(data[i], i);
			//System.out.println(rt);
			for (var j = 0; j < data.length; ++j) {
				if (j <= i)
					assert rt.get(data[j]).equals(j) : data[j];
				else
					assert rt.get(data[j]) == null;
			}
		}
		System.out.println(rt);
		for (var i = 0; i < data.length; ++i) {
			//System.out.println("DEL " + data[i]);
			assert rt.del(data[i]).equals(i);
		}
		System.out.println(rt);
	}

	private static String data[] = {
			"tract",
			"pluck",
			"rumor",
			"choke",
			"abbey",
			"robot",
			"north",
			"dress",
			"pride",
			"dream",
			"judge",
			"coast",
			"frank",
			"suite",
			"merit",
			"chest",
			"youth",
			"throw",
			"drown",
			"power",
			"ferry",
			"waist",
			"moral",
			"woman",
			"swipe",
			"straw",
			"shell",
			"class",
			"claim",
			"tired",
			"stand",
			"chaos",
			"shame",
			"thigh",
			"bring",
			"lodge",
			"amuse",
			"arrow",
			"charm",
			"swarm",
			"serve",
			"world",
			"raise",
			"means",
			"honor",
			"grand",
			"stock",
			"model",
			"greet",
			"basic",
			"fence",
			"fight",
			"level",
			"title",
			"knife",
			"wreck",
			"agony",
			"white",
			"child",
			"sport",
			"cheat",
			"value",
			"marsh",
			"slide",
			"tempt",
			"catch",
			"valid",
			"study",
			"crack",
			"swing",
			"plead",
			"flush",
			"awful",
			"house",
			"stage",
			"fever",
			"equal",
			"fault",
			"mouth",
			"mercy",
			"colon",
			"belly",
			"flash",
			"style",
			"plant",
			"quote",
			"pitch",
			"lobby",
			"gloom",
			"patch",
			"crime",
			"anger",
			"petty",
			"spend",
			"strap",
			"novel",
			"sword",
			"match",
			"tasty",
			"stick",
			"plants"
	};
}
