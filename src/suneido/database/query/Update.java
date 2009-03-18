package suneido.database.query;

import static suneido.database.server.Command.theDbms;

import java.util.List;

import suneido.SuException;
import suneido.SuRecord;
import suneido.database.Record;
import suneido.database.Transaction;
import suneido.database.query.expr.Expr;


public class Update extends QueryAction {
	private final List<String> fields;
	private final List<Expr> exprs;

	public Update(Query source, List<String> fields, List<Expr> exprs) {
		super(source);
		this.fields = fields;
		this.exprs = exprs;
	}

	@Override
	public String toString() {
		String s = "UPDATE " + source + " SET ";
		for (int i = 0; i < fields.size(); ++i)
			s += fields.get(i) + "=" + exprs.get(i) + ", ";
		return s.substring(0, s.length() - 2);
	}

	@Override
	public int execute(Transaction tran) {
		Query q = source.setup();
		if (!q.updateable())
			throw new SuException("update: query not updateable");
		q.setTransaction(tran);
		Header hdr = q.header();
		Row row;
		int n = 0;
		for (; null != (row = q.get(Dir.NEXT)); ++n) {
			SuRecord surec = row.surec(hdr);
			for (int i = 0; i < fields.size(); ++i)
				surec.put(fields.get(i), exprs.get(i).eval(hdr, row));
			Record newrec = surec.toDbRecord(hdr);
			theDbms.update(tran, row.getFirstData().off(), newrec);
		}
		return n;
	}

}
