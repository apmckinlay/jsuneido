package suneido;

import static suneido.Suneido.verify;
import static suneido.database.server.Command.theDbms;

import java.util.*;

import suneido.database.Record;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.database.server.DbmsTran;
import suneido.language.*;
import suneido.language.builtin.RecordMethods;
import suneido.language.builtin.TransactionInstance;

public class SuRecord extends SuContainer {
	private final Header hdr;
	private final TransactionInstance tran;
	private long recadr;
	private final Status status;
	private final List<Object> observers = new ArrayList<Object>();

	enum Status {
		NEW, OLD, DELETED
	};

	public SuRecord() {
		hdr = null;
		tran = null;
		recadr = 0;
		status = Status.NEW;
	}

	public SuRecord(Row row, Header hdr) {
		this(row, hdr, (TransactionInstance) null);
	}

	public SuRecord(Row row, Header hdr, DbmsTran tran) {
		this(row, hdr, new TransactionInstance(tran));
	}

	public SuRecord(Row row, Header hdr, TransactionInstance tran) {
		this.hdr = hdr;
		this.tran = tran;
		this.recadr = row.recadr;
		status = Status.OLD;

		verify(recadr >= 0);
		for (Iterator<Row.Entry> iter = row.iterator(hdr); iter.hasNext();)
			{
			Row.Entry e = iter.next();
			if (e.field.equals("-") || e.value.remaining() == 0)
				continue ;
			Object x = Pack.unpack(e.value);
//			if (has_suffix(field, "_deps"))
//				dependencies(basename(field), x.gcstr());
//			else
				put(e.field, x);
		}
	}

	public SuRecord(Record rec, List<String> flds, TransactionInstance tran) {
		hdr = null;
		this.tran = tran;
		recadr = 0;
		status = Status.OLD;
		int i = 0;
		for (String field : flds) {
			if (field.equals("-"))
				continue ;
			Object x = Pack.unpack(rec.getraw(i++));
			// dependencies
			if (field.endsWith("_deps")) {
//				char* base = PREFIXA(field, strlen(field) - 5);
//				dependencies(::symnum(base), x.gcstr());
			} else
				put(field, x);
		}
	}

	@Override
	public String toString() {
		return toString("[", "]");
	}

	@Override
	public void put(Object key, Object value) {
		if (get(key).equals(value))
			return;
		super.put(key, value);
		callObservers(key);
	}

	@Override
	public Object get(Object key) {
		Object x = super.get(key);
		return x == null ? "" : x;
	}

	@Override
	public Record toDbRecord(Header hdr) {
		String[] fldsyms = hdr.output_fldsyms();
		// dependencies
		// - access all the fields to ensure dependencies are created
		// Lisp<int> f;
		// for (f = fldsyms; ! nil(f); ++f)
		// if (*f != -1)
		// getdata(symbol(*f));
		// - invert stored dependencies
		// typedef HashMap<ushort, Lisp<ushort> > Deps;
		// Deps deps;
		// for (HashMap<ushort,Lisp<ushort> >::iterator it = dependents.begin();
		// it != dependents.end(); ++it)
		// {
		// for (Lisp<ushort> m = it->val; ! nil(m); ++m)
		// {
		// ushort d = depsname(*m);
		// if (fldsyms.member(d))
		// deps[d].push(it->key);
		// }
		// }

		Record rec = new Record();
		// OstreamStr oss;
		Object x;
		String ts = hdr.timestamp_field();
		for (String f : fldsyms)
			if (f == null)
				rec.addMin();
			else if (f == ts)
				rec.add(theDbms.timestamp());
			// else if (Lisp<ushort>* pd = deps.find(*f))
			// {
			// // output dependencies
			// oss.clear();
			// for (Lisp<ushort> d = *pd; ! nil(d); )
			// {
			// oss << symstr(*d);
			// if (! nil(++d))
			// oss << ",";
			// }
			// rec.addval(oss.str());
			// }
			else if (null != (x = get(f)))
				rec.add(x);
			else
				rec.addMin();
		return rec;
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		assert this == self;
		return RecordMethods.invoke(this, method, args);
	}

	public void update() {
		ck_modify("Update");
		Record newrec = toDbRecord(hdr);
		recadr = theDbms.update(tran.getTransaction(), recadr, newrec);
		verify(recadr >= 0);
	}

	public void delete() {
		ck_modify("Delete");
		theDbms.erase(tran.getTransaction(), recadr);
	}

	private void ck_modify(String op) {
		if (tran == null)
			throw new SuException("record." + op + ": no Transaction");
		if (tran.isEnded())
			throw new SuException("record." + op
					+ ": Transaction already completed");
		if (status != Status.OLD)
			throw new SuException("record." + op + ": not an old record");
		if (recadr == 0)
			throw new SuException("record." + op + ": not a database record");
	}

	@Override
	public String typeName() {
		return "Record";
	}

	public boolean isNew() {
		return status == Status.NEW;
	}

	public TransactionInstance getTransaction() {
		return tran;
	}

	public void addObserver(Object observer) {
		observers.add(observer);
	}

	public void removeObserver(Object observer) {
		observers.remove(observer);
	}

	public void invalidate(Object member) {
		callObservers(member);
	}

	private static class ActiveObserver {
		public Object observer;
		public Object member;

		public ActiveObserver(Object observer, Object member) {
			this.observer = observer;
			this.member = member;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (!(other instanceof ActiveObserver))
				return false;
			ActiveObserver that = (ActiveObserver) other;
			return observer.equals(that.observer) && member.equals(that.member);
		}
	}

	public static final ThreadLocal<List<ActiveObserver>> activeObservers =
			new ThreadLocal<List<ActiveObserver>>() {
				@Override
				public List<ActiveObserver> initialValue() {
					return new ArrayList<ActiveObserver>();
				}
			};

	public void callObservers(Object member) {
		List<ActiveObserver> aos = activeObservers.get();
		for (Object observer : observers) {
			// prevent cycles
			ActiveObserver ao = new ActiveObserver(observer, member);
			if (aos.contains(ao))
				continue;
			aos.add(ao);
			try {
				if (observer instanceof SuMethod)
					((SuMethod) observer).call(Args.Special.NAMED, "member",
							member);
				else if (observer instanceof SuValue)
					((SuValue) observer).eval(this, Args.Special.NAMED,
							"member", member);
				else
					throw new SuException("invalid observer");
			} finally {
				aos.remove(ao);
			}
		}
	}

}
