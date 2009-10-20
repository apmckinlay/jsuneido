package suneido.database;

import java.util.List;

import net.jcip.annotations.Immutable;

import com.google.common.collect.ImmutableList;

/**
 * @author Andrew McKinlay
 */
@Immutable
public class Table {
	final static int TBLNUM = 0, TABLE = 1, NEXTFIELD = 2, NROWS = 3, TOTALSIZE = 4;
	public final String name;
	public final int num;
	public final Columns columns;
	public final Indexes indexes;
	public final ImmutableList<String> fields;

	public Table(Record record, Columns columns, Indexes indexes) {
		this.columns = columns;
		this.indexes = indexes;
		this.fields = get_fields();
		num = record.getInt(TBLNUM);
		name = record.getString(TABLE);
	}

	public boolean hasColumn(String name) {
		return columns.hasColumn(name);
	}

	public Column getColumn(String name) {
		return columns.find(name);
	}

	public boolean hasIndexes() {
		return !indexes.isEmpty();
	}

	public boolean hasIndex(String columns) {
		return indexes.hasIndex(columns);
	}

	public Index firstIndex() {
		return indexes.first();
	}

	public Index getIndex(String columns) {
		return indexes.get(columns);
	}

	public boolean singleton() {
		return indexes.first().columns.equals("");
	}
	public List<String> getColumns() {
		return columns.names();
	}
	public List<List<String>> indexesColumns() {
		return indexes.columns();
	}
	public List<List<String>> keysColumns() {
		return indexes.keysColumns();
	}

	/**
	 * @return The physical fields. 1:1 match with records.
	 */
	public ImmutableList<String> getFields() {
		return fields;
	}

	private ImmutableList<String> get_fields() {
		ImmutableList.Builder<String> list = ImmutableList.builder();
		int i = 0;
		for (Column cs : columns) {
			if (cs.num < 0)
				continue; // skip rules
			for (; i < cs.num; ++i)
				list.add("-");
			list.add(cs.name);
			++i;
		}
		return list.build();
	}

	public String schema() {
		StringBuilder sb = new StringBuilder();

		// fields
		sb.append("(");
		for (String col : getColumns())
			if (!col.equals("-"))
				sb.append(col).append(",");
		// for (String f : get_rules(table))
		// {
		// gcstring str(f->str()); // copy
		// char* s = str.str();
		// *s = toupper(*s);
		// sb.append(s).append(",");
		// }
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");

		// indexes
		for (Index index : indexes) {
			if (index.isKey)
				sb.append(" key");
			else
				sb.append(" index").append(index.unique ? " unique" : "");
			sb.append("(").append(index.columns).append(")");
			if (index.fksrc != null && !index.fksrc.tablename.equals("")) {
				sb.append(" in ").append(index.fksrc.tablename);
				if (!index.fksrc.columns.equals(index.columns))
					sb.append("(").append(index.fksrc.columns).append(")");
				if (index.fksrc.mode == Index.CASCADE)
					sb.append(" cascade");
				else if (index.fksrc.mode == Index.CASCADE_UPDATES)
					sb.append(" cascade update");
			}
		}
		return sb.toString();
	}

	public static Record record(String name, int num, int nextfield,
			int nrecords) {
		Record r = new Record();
		r.add(num).add(name).add(nextfield).add(nrecords).add(100);
		r.alloc(24); // 24 = 3 fields * max int packsize - min int packsize
		return r;
	}

	@Override
	public String toString() {
		return "Table(" + name + ":" + num + ") " + columns + " " + indexes;
	}

}
