package suneido.language.builtin;

import java.util.Iterator;

import suneido.SuContainer;
import suneido.SuValue;

/**
 * A wrapper for an iterator that can be treated like an SuObject
 * but doesn't instantiate the object if you just iterate over it.
 *
 * @author Andrew McKinlay
 */
public class SuSequence extends SuValue
		implements Iterator<Object>, Iterable<Object> {
	private final Iterable<Object> iterable;
	private final Iterator<Object> iter;
	private SuContainer ob = null;

	public SuSequence(Iterable<Object> iterable) {
		this.iterable = iterable;
		iter = iterable.iterator();
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
			for (Object value : iterable)
				ob.append(value);
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

	public Iterator<Object> iterator() {
		return iterable.iterator();
	}

	@Override
	public SuContainer toContainer() {
		return getOb();
	}

	@Override
	public Object get(Object key) {
		return getOb().get(key);
	}

}
