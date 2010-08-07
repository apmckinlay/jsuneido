package suneido.language.builtin;

import static suneido.util.Util.array;

import java.util.Iterator;
import java.util.NoSuchElementException;

import suneido.SuContainer;
import suneido.SuValue;
import suneido.language.*;

import com.google.common.base.Objects;

public final class Seq extends BuiltinClass {

	@Override
	public Object newInstance(Object[] args) {
		return new SuSequence(new SuSeq(args));
	}

	private static class SuSeq extends SuValue implements Iterable<Object> {
		private Object from;
		private Object to;
		private final Object by;

		private static final FunctionSpec initFS =
				new FunctionSpec(array("from", "to", "by"), false, 1);

		SuSeq(Object[] args) {
			args = Args.massage(initFS, args);
			from = args[0];
			to = args[1];
			by = args[2];
			if (to == Boolean.FALSE) {
				to = from;
				from = 0;
			}
		}

		@Override
		public String toString() {
			return "Seq(" + from + ", " + to + ", " + by + ")";
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
				if (!hasNext())
					throw new NoSuchElementException();
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
			if (!(other instanceof SuSeq))
				return false;
			SuSeq that = (SuSeq) other;
			return Ops.is_(from, that.from) &&
					Ops.is_(to, that.to) &&
					Ops.is_(by, that.by);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(from, to, by);
		}

		@Override
		public SuContainer toContainer() {
			SuContainer c = new SuContainer();
			for (Object x : this)
				c.append(x);
			return c;
		}

	}

}
