package suneido.database.query;

import static suneido.Suneido.verify;

import java.nio.ByteBuffer;
import java.util.List;

import suneido.*;
import suneido.database.Record;
import suneido.database.Transaction;
import suneido.database.query.Header.Which;

public class Row {
	final Record[] data;
	long recadr = 0; // if Row contains single update-able record, this is its
						// address
	Transaction tran = null;
	SuContainer surec = null;

	Row(Record... data) {
		this.data = data;
	}

	public Row(Record record, long recadr) {
		data = new Record[] { record };
		this.recadr = recadr;
		verify(recadr > 0);
	}

	public ByteBuffer getraw(Header hdr, String col) {
		return getraw(find(hdr, col));
	}

	public Record project(Header hdr, List<String> flds) {
		Record key = new Record();
		for (String f : flds)
			key.add(getrawval(hdr, f));
		return key;
	}

	ByteBuffer getrawval(Header hdr, String col) {
		Which w = find(hdr, col);
		if (w != null)
			return getraw(w);
		// else rule
		SuValue val = surec().getdata(SuString.valueOf(col));
		return val.pack();
	}

	private SuContainer surec() {
		if (surec == null)
			surec = new SuContainer(); // TODO
		return surec;
	}

	private ByteBuffer getraw(Which w) {
		return w == null ? ByteBuffer.allocate(0) : data[w.di].getraw(w.ri);
	}

	Which find(Header hdr, String col) {
		Which w = hdr.find(col);
		return w == null || w.di >= data.length ? null : w;
	}

	public SuValue getval(Header hdr, String col) {
		Which w = find(hdr, col);
		if (w != null || !hdr.cols.contains(col))
			return SuValue.unpack(getraw(w));
		// else rule
		return surec().getdata(SuString.valueOf(col));
	}
}
