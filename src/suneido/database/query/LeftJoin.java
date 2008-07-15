package suneido.database.query;

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

}
