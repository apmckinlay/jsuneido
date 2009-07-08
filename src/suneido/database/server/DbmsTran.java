package suneido.database.server;

public interface DbmsTran {
	String complete();

	void abort();

	boolean isReadonly();
}
