package suneido.language;

import java.util.Iterator;

import suneido.SuContainer;
import suneido.SuValue;
import suneido.language.builtin.ContainerMethods;

/**
 * A wrapper for an iterator that can be treated like an SuObject
 * but doesn't instantiate the object if you just iterate over it.
 *
 * @author Andrew McKinlay
 */
public class SuSequence extends SuValue implements Iterator<Object> {
	private final Iterator<Object> originalIter;
	private final Iterator<Object> iter;
	private SuContainer ob = null;

	public SuSequence(Iterator<Object> iter) {
		this.iter = originalIter = iter;
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		return ContainerMethods.invoke(getOb(), method, args);
	}

	// TODO handle compareTo (also in Ops.cmp)

	@Override
	public boolean equals(Object other) {
		return getOb().equals(other);
	}

	@Override
	public int hashCode() {
		return getOb().hashCode();
	}

	@Override
	public String toString() {
		return getOb().toString();
	}

	private SuContainer getOb() {
		if (ob == null) {
			ob = new SuContainer();
			for (Iterator<Object> i = originalIter; i.hasNext();)
				ob.append(i.next());
		}
		return ob;
	}

	public boolean hasNext() {
		return iter.hasNext();
	}

	public Object next() {
		return iter.next();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
