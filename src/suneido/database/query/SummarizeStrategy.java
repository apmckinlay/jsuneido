package suneido.database.query;

import static suneido.Suneido.dbpkg;

import java.util.ArrayList;
import java.util.List;

import suneido.database.query.Query.Dir;
import suneido.database.query.Summarize.Summary;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;

public abstract class SummarizeStrategy {
	Summarize q;
	Query source;
	Dir curdir;
	Keyrange sel = new Keyrange();

	public SummarizeStrategy(Summarize q) {
		this.q = q;
		source = q.source;
	}

	List<Summary> funcSums() {
		List<Summary> sums = new ArrayList<Summary>();
		for (String f : q.funcs)
			sums.add(Summary.valueOf(f));
		return sums;
	}

	void initSums(List<Summary> sums) {
		for (Summary s : sums)
			s.init();
	}

	Row makeRow(Record r, List<Summary> sums) {
		RecordBuilder rb = dbpkg.recordBuilder();
		rb.addAll(r);
		for (Summary s : sums)
			rb.add(s.result());
		return new Row(dbpkg.minRecord(), rb.build());
	}

	abstract Row get(Dir dir, boolean rewound);

	abstract void select(List<String> index, Record from, Record to);
}
