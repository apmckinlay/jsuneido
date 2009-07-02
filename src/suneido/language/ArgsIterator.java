package suneido.language;

import static suneido.SuContainer.IterResult.ENTRY;
import static suneido.SuContainer.IterWhich.ALL;
import static suneido.language.Args.Special.*;

import java.util.AbstractMap;
import java.util.Iterator;

import suneido.SuContainer;
import suneido.util.NullIterator;

public class ArgsIterator implements Iterator<Object>, Iterable<Object> {
	private final Object[] args;
	private int argi = 0;
	Iterator<Object> each = new NullIterator<Object>();

	public ArgsIterator(Object[] args) {
		this.args = args;
	}

	public boolean hasNext() {
		if (each.hasNext())
			return true;
		if (argi >= args.length)
			return false;
		Object x = args[argi];
		if (x != EACH && x != EACH1)
			return true;
		return ((SuContainer) args[argi + 1]).size() > (x == EACH1 ? 1 : 0);
	}

	public Object next() {
		if (each.hasNext())
			return each.next();
		Object x = args[argi++];
		if (x == EACH || x == EACH1) {
			each = ((SuContainer) args[argi++]).iterator(ALL, ENTRY);
			if (x == EACH1 && each.hasNext())
				each.next();
			return next();
		} else if (x == NAMED) {
			x = args[argi++];
			return new AbstractMap.SimpleEntry<Object, Object>(x, args[argi++]);
		}
		return x;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 * to allow: for (x : ArgsIterator(args))
	 */
	public Iterator<Object> iterator() {
		return this;
	}

}
