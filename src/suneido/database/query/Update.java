package suneido.database.query;

import static suneido.database.Database.theDB;

import java.util.ArrayList;
import java.util.List;

import suneido.SuException;
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
		List<Expr> fldexprs = new ArrayList<Expr>();
		for (String field : hdr.fields()) {
			int i = fields.indexOf(field);
			fldexprs.add(i == -1 ? null : exprs.get(i));
		}
		Row row;
		int n = 0;
		for (; null != (row = q.get(Dir.NEXT)); ++n) {
			long off = row.getFirstData().off();
			Record rec = theDB.input(off);
			Record newrec = new Record();
			for (Expr expr : fldexprs)
				if (expr == null)
					newrec.add(rec.getraw(newrec.size()));
				else
					newrec.add(expr.eval(hdr, row));
			theDB.updateRecord(tran, off, newrec);
		}
		return n;
	}

}
