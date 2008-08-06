package suneido.database.query;

import suneido.database.Transaction;


public abstract class Query2 extends Query1 {
	Query source2;

	Query2(Query source1, Query source2) {
		super(source1);
		this.source2 = source2;
	}

	@Override
	Query transform() {
		source = source.transform();
		source2 = source2.transform();
		return this;
	}

	@Override
	Query addindex() {
		source2 = source2.addindex();
		return super.addindex();
	}

	@Override
	boolean updateable() {
		return false;
	}

	@Override
	void setTransaction(Transaction tran) {
		super.setTransaction(tran);
		source2.setTransaction(tran);
	}

	@Override
	public Header header() {
		return new Header(source.header(), source2.header());
	}

}
