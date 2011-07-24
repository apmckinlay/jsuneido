package suneido.database.query;

import java.util.List;
import java.util.Set;

import suneido.SuException;
import suneido.SuRecord;
import suneido.database.query.expr.Expr;
import suneido.intfc.database.Record;
import suneido.intfc.database.Transaction;

import com.google.common.collect.ImmutableSet;

public class Update extends QueryAction {
	private final Transaction tran;
	private final List<String> fields;
	private final List<Expr> exprs;

	public Update(Transaction tran, Query source, List<String> fields, List<Expr> exprs) {
		super(source);
		this.tran = tran;
		this.fields = fields;
		this.exprs = exprs;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE ").append(source).append(" SET ");
		for (int i = 0; i < fields.size(); ++i)
			sb.append(fields.get(i)).append("=").append(exprs.get(i)).append(", ");
		return sb.substring(0, sb.length() - 2);
	}

	@Override
	public int execute() {
		Query q = source.transform();
		Set<String> cols = ImmutableSet.copyOf(q.columns());
		List<String> bestKey = q.key_index(cols);
		if (q.optimize(bestKey, cols, noNeeds, false, true) >= IMPOSSIBLE)
			throw new SuException("invalid query");
		q = q.addindex(tran);
		// cSuneido uses source.key_index
		// but this causes problems - maybe need transform first?
		//		List<String> bestKey = source.keys().get(0);
		//		Query q = source.setup();
		if (!q.updateable())
			throw new SuException("update: query not updateable");
		Header hdr = q.header();
		Row row;
		int n = 0;
		for (; null != (row = q.get(Dir.NEXT)); ++n) {
			SuRecord surec = row.surec(hdr);
			for (int i = 0; i < fields.size(); ++i)
				surec.put(fields.get(i), exprs.get(i).eval(hdr, row));
			Record newrec = surec.toDbRecord(hdr);
			tran.updateRecord(q.tblnum(), row.firstData(), newrec);
		}
		return n;
	}

}
