package suneido.database;

public class TheDb {
	private static Database theDb;

	public static void set(Database db) {
		theDb = db;
	}

	public static void open(String filename, Mode mode) {
		theDb = new Database(filename, mode);
	}

	public static void open() {
		open("suneido.db", Mode.OPEN);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				close();
			}
		});
	}

	public static boolean isOpen() {
		return theDb != null;
	}

	public static Database db() {
		if (theDb == null)
			TheDb.open();
		return theDb;
	}

	public static void close() {
		if (theDb != null)
			theDb.close();
		theDb = null;
	}

}
