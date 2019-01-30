package suneido.util;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Exploratory implementation, not used
 * Simple Btree storing pointers rather than keys
 * and storing actual keys in non-leaf nodes (i.e. *not* B+tree)
 */
public class PtrBtree {
	final static int MAX_ITEMS = 6;
	final static int MID = MAX_ITEMS / 2;
	Node root = new LeafNode();

	void put(String item) {
		var pivot = root.put(item);
		if (pivot != null)	{
			var newRoot = new TreeNode();
			newRoot.nodes.add(root);
			newRoot.items.add(pivot.item);
			newRoot.nodes.add(pivot.node);
			root = newRoot;
		}
	}

	boolean has(String item) {
		return root.has(item);
	}

	@Override
	public String toString() {
		return root.toString();
	}

	private static class Pivot {
		String item;
		Node node;
		Pivot(String item, Node node) {
			this.item = item;
			this.node = node;
		}
	}

	//--------------------------------------------------------------------------
	private static abstract class Node {
		ArrayList<String> items = new ArrayList<>();

		// put returns either null or a pivot value if there was a split
		abstract Pivot put(String item);
		abstract boolean has(String item);
		int size() {
			return items.size();
		}
		@Override
		public String toString() {
			return toString(new StringBuilder(), "").toString();
		}
		abstract StringBuilder toString(StringBuilder sb, String indent);
	}

	//--------------------------------------------------------------------------
	private static class TreeNode extends Node {
		ArrayList<Node> nodes = new ArrayList<>();

		@Override
		Pivot put(String item) {
			assert nodes.size() == items.size() + 1;
			var i = Collections.binarySearch(items, item);
			if (i >= 0)
				return null; // already exists
			i = -(i + 1);
			var pivot = nodes.get(i).put(item);
			if (pivot != null) {
				// insert pivot
				items.add(i, pivot.item);
				nodes.add(i+1, pivot.node);
				if (size() > MAX_ITEMS) {
					// split
					var newnode = new TreeNode();
					pivot = new Pivot(items.get(MID), newnode);
					newnode.items.addAll(items.subList(MID+1, items.size()));
					newnode.nodes.addAll(nodes.subList(MID+1, nodes.size()));
					items.subList(MID, items.size()).clear();
					nodes.subList(MID+1, nodes.size()).clear();
					return pivot;
				}
			}
			return null;
		}
		@Override
		StringBuilder toString(StringBuilder sb, String indent) {
			nodes.get(0).toString(sb, indent + "    ");
			for (var i = 0; i < items.size(); ++i) {
				sb.append(indent).append(items.get(i)).append("\n");
				nodes.get(i+1).toString(sb, indent + "    ");
			}
			return sb;
		}
		@Override
		boolean has(String item) {
			assert nodes.size() == items.size() + 1;
			for (var i = 0; i < items.size(); ++i) {
				var cmp = item.compareTo(items.get(i));
				if (cmp == 0)
					return true;
				if (cmp < 0)
					return nodes.get(i).has(item);
			}
			return nodes.get(items.size()).has(item);
		}
	}

	//--------------------------------------------------------------------------
	private static class LeafNode extends Node {
		ArrayList<String> buffer = new ArrayList<>();

		@Override
		Pivot put(String item) {
			buffer.add(item);
			var n = items.size() + buffer.size();
			if (n <= MAX_ITEMS)
				return null;
			// else split
			items.addAll(buffer);
			buffer.clear();
			Collections.sort(items);
			var newnode = new LeafNode();
			var pivot = new Pivot(items.get(MID), newnode);
			newnode.items.addAll(items.subList(MID+1, items.size()));
			items.subList(MID, items.size()).clear();
			return pivot;
		}
		@Override
		StringBuilder toString(StringBuilder sb, String indent) {
			sb.append(indent);
			for (var s : items)
				sb.append(s).append(" ");
			sb.append("| ");
			for (var s : buffer)
				sb.append(s).append(" ");
			sb.append("\n");
			return sb;
		}
		@Override
		boolean has(String item) {
			return items.contains(item) || buffer.contains(item);
		}
	}

	//--------------------------------------------------------------------------
	public static void main(String[] args) {
		PtrBtree t = new PtrBtree();

//		String[] data = { "foo", "bar", "baz", "ant", "ego", "age", "bat" };
//		Arrays.sort(data);
		for (var i = 0; i < data.length; ++i) {
//			System.out.println("+ " + data[i]);
			t.put(data[i]);
			for (var j = 0; j < data.length; ++j)
				assert t.has(data[j]) == (j <= i);
//			System.out.println(t);
		}
		System.out.println(t);
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
	};
}
