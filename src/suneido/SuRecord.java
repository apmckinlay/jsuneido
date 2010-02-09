package suneido;

import static suneido.SuException.verify;
import static suneido.database.server.Command.theDbms;

import java.nio.ByteBuffer;
import java.util.*;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.Record;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.database.server.DbmsTran;
import suneido.language.*;
import suneido.language.builtin.RecordMethods;
import suneido.language.builtin.TransactionInstance;
import suneido.util.Util;

/**
 * @author Andrew McKinlay
 */
@NotThreadSafe
public class SuRecord extends SuContainer {
	private Header hdr;
	private TransactionInstance tran;
	private long recadr;
	private Status status;
	private final List<Object> observers = new ArrayList<Object>();
	private final Set<Object> invalid = new HashSet<Object>();
	private final Map<Object, Set<Object>> dependencies =
			new HashMap<Object, Set<Object>>();
	private final Deque<Object> activeRules = new ArrayDeque<Object>();
	private final Set<Object> invalidated = new HashSet<Object>();

	enum Status {
		NEW, OLD, DELETED
	};

	public SuRecord() {
		hdr = null;
		tran = null;
		recadr = 0;
		status = Status.NEW;
	}

	public SuRecord(SuRecord r) {
		super(r);
		hdr = null;
		tran = null;
		recadr = 0;
		status = r.status;
		for (Map.Entry<Object, Set<Object>> e : r.dependencies.entrySet())
			dependencies.put(e.getKey(), new HashSet<Object>(e.getValue()));
	}

	public SuRecord(Row row, Header hdr) {
		this(row, hdr, (TransactionInstance) null);
	}

	public SuRecord(Row row, Header hdr, DbmsTran tran) {
		this(row, hdr, tran == null ? null : new TransactionInstance(tran));
	}

	public SuRecord(Row row, Header hdr, TransactionInstance tran) {
		this.hdr = hdr;
		this.tran = tran;
		this.recadr = row.recadr;
		status = Status.OLD;

		verify(recadr >= 0);
		for (Iterator<Row.Entry> iter = row.iterator(hdr); iter.hasNext();) {
			Row.Entry e = iter.next();
			addField(e.field, e.value);
		}
	}

	public SuRecord(Record rec, List<String> flds, TransactionInstance tran) {
		hdr = null;
		this.tran = tran;
		recadr = 0;
		status = Status.OLD;
		int i = 0;
		for (String field : flds)
			addField(field, rec.getraw(i++));
	}

	@Override
	public void clear() {
		super.clear();
		hdr = null;
		tran = null;
		recadr = 0;
		status = Status.NEW;
	}

	private void addField(String field, ByteBuffer buf) {
		if (field.equals("-") || buf.remaining() == 0)
			return;
		Object x = Pack.unpack(buf);
		if (field.endsWith("_deps"))
			dependencies(baseFieldName(field), (String) x);
		else
			super.put(field, x);
	}

	/** remove "_deps" suffix */
	private String baseFieldName(String field) {
		return field.substring(0, field.length() - 5);
	}

	private void dependencies(String mem, String s) {
		for (int pos = 0; pos < s.length();) {
			int end = s.indexOf(',', pos);
			if (end == -1)
				end = s.length();
			String t = s.substring(pos, end);
			addDependency(mem, t);
			pos = end + 1;
		}
	}

	@Override
	public String toString() {
		return toString("[", "]");
	}

	@Override
	public void pack(ByteBuffer buf) {
		super.pack(buf, Pack.Tag.RECORD);
	}

	public static Object unpack(ByteBuffer buf) {
		return unpack(buf, new SuRecord());
	}

	@Override
	public void put(Object key, Object value) {
		Object old = super.get(key);
		if (old != null && old.equals(value))
			return;
		super.put(key, value);
		invalidateDependents(key);
		callObservers(key);
	}

	private void invalidateDependents(Object key) {
		if (dependencies.containsKey(key))
			for (Object dep : dependencies.get(key))
				invalidate(dep);
	}

	@Override
	public Object get(Object key) {
		RuleContext.Rule ar = RuleContext.top();
		if (ar != null && ar.rec == this)
			addDependency(ar.member, key);

		Object result = containsKey(key) ? super.get(key) : null;
		if (result == null || invalid.contains(key)) {
			Object x = callRule(key);
			if (x != null)
				result = x;
			else if (result == null)
				result = "";
		}
		return result;
	}

	private void addDependency(Object src, Object dst) {
		if (!dependencies.containsKey(dst))
			dependencies.put(dst, new HashSet<Object>());
		dependencies.get(dst).add(src);
	}

	private Object callRule(Object key) {
		invalid.remove(key);
		Object rule = Globals.tryget("Rule_" + key);
		if (rule == null)
			return null;
		// prevent cycles
		if (activeRules.contains(key))
			return null;
		activeRules.push(key);
		try {
			RuleContext.push(this, key);
			try {
				if (rule instanceof SuValue) {
					Object x = ((SuValue) rule).eval(this);
					//System.out.println("callRule " + key + " => " + x);
					if (x != null)
						putMap(key, x);
					return x;
				} else
					throw new SuException("invalid Rule_" + key);
			} finally {
				RuleContext.pop(this, key);
			}
		} finally {
			assert activeRules.peek() == key;
			activeRules.pop();
		}
	}

	@Override
	public Record toDbRecord(Header hdr) {
		List<String> fldsyms = hdr.output_fldsyms();
		Map<Object, Set<Object>> deps = getDeps(hdr, fldsyms);

		Record rec = new Record();
		StringBuilder sb = new StringBuilder();
		Object x;
		String ts = hdr.timestamp_field();
		for (String f : fldsyms)
			if (f == null)
				rec.addMin();
			else if (f.equals(ts))
				rec.add(theDbms.timestamp());
			else if (deps.containsKey(f)) {
				// output dependencies
				sb.setLength(0);
				for (Object d : deps.get(f))
					sb.append(",").append(d);
				rec.add(sb.substring(1));
			} else if (null != (x = get(f)))
				rec.add(x);
			else
				rec.addMin();
		return rec;
	}

	private Map<Object, Set<Object>> getDeps(Header hdr, List<String> fldsyms) {
		// access all fields to ensure dependencies are created
		for (String f : hdr.output_fldsyms())
			if (!f.equals("-"))
				get(f);
		// invert dependencies
		Map<Object, Set<Object>> deps = new HashMap<Object, Set<Object>>();
		for (Map.Entry<Object, Set<Object>> e : dependencies.entrySet())
			for (Object x : e.getValue()) {
				String d = x + "_deps";
				if (!fldsyms.contains(d))
					continue;
				if (!deps.containsKey(d))
					deps.put(d, new HashSet<Object>());
				deps.get(d).add(e.getKey());
			}
		return deps;
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
		boolean was_valid = !invalid.contains(member);
		invalidated.add(member); // for observers
		invalid.add(member);
		if (was_valid)
			invalidateDependents(member);
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

		@Override
		public int hashCode() {
			throw SuException.unreachable();
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
		callObservers2(member);
		invalidated.remove(member);
		for (Iterator<Object> iter = invalidated.iterator(); iter.hasNext();) {
			Object m = iter.next();
			iter.remove();
			callObservers2(m);
		}
	}

	public void callObservers2(Object member) {
		List<ActiveObserver> aos = activeObservers.get();
		for (Object observer : observers) {
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

	public List<String> getdeps(String field) {
		List<String> deps = new ArrayList<String>();
		for (Map.Entry<Object, Set<Object>> e : dependencies.entrySet())
			for (Object x : e.getValue())
				if (field.equals(x))
					deps.add(e.getKey().toString());
		return deps;
	}

	public void setdeps(String field, String deps) {
		for (String d : Util.commasToList(deps))
			addDependency(field, d);
	}

}
