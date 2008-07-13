package suneido.database.query;

public abstract class QueryCompatible extends Query2 {
	String disjoint;

	QueryCompatible(Query source1, Query source2) {
		super(source1, source2);
	}
}
