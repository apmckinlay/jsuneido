package suneido.database.query;

public abstract class Query2 extends Query1 {
	Query source2;

	Query2(Query source1, Query source2) {
		super(source1);
		this.source2 = source2;
	}

	public Query source2() {
		return source2;
	}

	public void setSource2(Query source) {
		this.source2 = source;
	}

}
