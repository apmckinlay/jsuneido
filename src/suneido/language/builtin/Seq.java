package suneido.language.builtin;

import static suneido.util.Util.array;

import java.util.Iterator;

import suneido.SuValue;
import suneido.language.*;

public class Seq extends SuValue implements Iterable<Object> {
	private Object from;
	private Object to;
	private Object by;

	@Override
	public String toString() {
		return "Seq(" + from + ", " + to + ", " + by + ")";
	}

	@Override
	public Object call(Object... args) {
		return new Seq().init(args);
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "<new>")
			return new Seq().init(args);
		if (method == "_init")
			return init(args);
		return super.invoke(self, method, args);
	}

	private static final FunctionSpec initFS =
			new FunctionSpec(array("from", "to", "by"), false, 1);
	private Object init(Object[] args) {
		args = Args.massage(initFS, args);
		from = args[0];
		to = args[1];
		by = args[2];
		if (to == Boolean.FALSE) {
			to = from;
			from = 0;
		}
		return this;
	}

	public Iterator<Object> iterator() {
		return new Iter();
	}

	class Iter implements Iterator<Object> {
		Object i = from;

		public boolean hasNext() {
			return Ops.cmp(i, to) < 0;
		}

		public Object next() {
			Object x = i;
			i = Ops.add(i, by);
			return x;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	@Override
	public boolean equals(Object other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}

}
