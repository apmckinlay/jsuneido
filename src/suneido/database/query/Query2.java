package suneido.database.query;


public abstract class Query2 extends Query1 {
	Query source2;

	Query2(Query source1, Query source2) {
		super(source1);
		this.source2 = source2;
	}

	@Override
	Query transform() { // also defined by Query2
		source = source.transform();
		return this;
	}

	@Override
	boolean updateable() {
		return false;
	}

}
