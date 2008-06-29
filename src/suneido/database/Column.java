package suneido.database;

public class Column implements Comparable<Column> {
	public String name;
	public short num;

	public Column(String column, short colnum) {
		this.name = column;
		this.num = colnum;
	}

	public int compareTo(Column other) {
		return num - other.num;
	}
}
