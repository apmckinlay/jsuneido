package suneido.database.query;

import static suneido.SuException.unreachable;

import java.util.List;

public class LeftJoin extends Join {
	LeftJoin(Query source1, Query source2, List<String> by) {
		super(source1, source2, by);
	}

	@Override
	protected String name() {
		return "LEFTJOIN";
	}

	@Override
	protected boolean can_swap() {
		return false;
	}

	@Override
	List<List<String>> keys() {
		switch (type) {
		case ONE_ONE:
		case N_ONE:
			return source.keys();
		case ONE_N:
		case N_N:
			return keypairs();
		default:
			throw unreachable();
		}
	}

	@Override
	double nrecords() {
		return source.nrecords();
	}
}
