package suneido;

import static suneido.Suneido.verify;
import static suneido.database.server.Command.theDbms;

import java.util.Iterator;

import suneido.database.Record;
import suneido.database.Transaction;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.language.Pack;

public class SuRecord extends SuContainer {
	//private final Header hdr;
	//private final Transaction trans;
	private final long recadr;
	//private final Status status;

	enum Status {
		NEW, OLD, DELETED
	};

	public SuRecord() {
		//hdr = null;
		//trans = null;
		recadr = 0;
		//status = Status.NEW;
	}

	public SuRecord(Row row, Header hdr, Transaction trans) {
		//this.hdr = hdr;
		//this.trans = trans;
		this.recadr = row.recadr;
		//status = Status.OLD;

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

	@Override
	public String toString() {
		return toString("[", "]");
	}

	@Override
	public Object get(Object key) {
		Object x = super.get(key);
		return x == null ? "" : x;
	}

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

}
