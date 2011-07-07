package suneido.database.query;

import org.junit.After;
import org.junit.Before;

import suneido.database.*;
import suneido.database.server.ServerData;
import suneido.language.Ops;

public class TestBase {
	protected final Database db = new Database(new DestMem(), Mode.CREATE);
	protected final ServerData serverData = new ServerData();

	@Before
	public void setQuoting() {
		Ops.default_single_quotes = true;
	}

	@Before
	public void create() {
		TheDb.set(db);
		makeDB();
	}

	@After
	public void close() {
		db.close();
	}

	protected void makeDB() {
		adm("create stdlib (group, name, text) key(name,group)");

		// create customer file
		adm("create customer (id, name, city) key(id)");
		req("insert{id: \"a\", name: \"axon\", city: \"saskatoon\"} into customer");
		req("insert{id: \"c\", name: \"calac\", city: \"calgary\"} into customer");
		req("insert{id: \"e\", name: \"emerald\", city: \"vancouver\"} into customer");
		req("insert{id: \"i\", name: \"intercon\", city: \"saskatoon\"} into customer");

		// create hist file
		adm("create hist (date, item, id, cost) index(date) key(date,item,id)");
		req("insert{date: 970101, item: \"disk\", id: \"a\", cost: 100} into hist");
		req("insert{date: 970101, item: \"disk\", id: \"e\", cost: 200} into hist");
		req("insert{date: 970102, item: \"mouse\", id: \"c\", cost: 200} into hist");
		req("insert{date: 970103, item: \"pencil\", id: \"e\", cost: 300} into hist");

		// create hist2 file - for leftjoin test
		adm("create hist2 (date, item, id, cost) key(date) index(id)");
		req("insert{date: 970101, item: \"disk\", id: \"a\", cost: 100} into hist2");
		req("insert{date: 970102, item: \"disk\", id: \"e\", cost: 200} into hist2");
		req("insert{date: 970103, item: \"pencil\", id: \"e\", cost: 300} into hist2");

		// create trans file
		adm("create trans (item, id, cost, date) index(item) key(date,item,id)");
		req("insert{item: \"mouse\", id: \"e\", cost: 200, date: 960204} into trans");
		req("insert{item: \"disk\", id: \"a\", cost: 100, date: 970101} into trans");
		req("insert{item: \"mouse\", id: \"c\", cost: 200, date: 970101} into trans");
		req("insert{item: \"eraser\", id: \"c\", cost: 150, date: 970201} into trans");

		// create supplier file
		adm("create supplier (supplier, name, city) index(city) key(supplier)");
		req("insert{supplier: \"mec\", name: \"mtnequipcoop\", city: \"calgary\"} into supplier");
		req("insert{supplier: \"hobo\", name: \"hoboshop\", city: \"saskatoon\"} into supplier");
		req("insert{supplier: \"ebs\", name: \"ebssail&sport\", city: \"saskatoon\"} into supplier");
		req("insert{supplier: \"taiga\", name: \"taigaworks\", city: \"vancouver\"} into supplier");

		// create inven file
		adm("create inven (item, qty) key(item)");
		req("insert{item: \"disk\", qty: 5} into inven");
		req("insert{item: \"mouse\", qty:2} into inven");
		req("insert{item: \"pencil\", qty: 7} into inven");

		// create alias file
		adm("create alias(id, name2) key(id)");
		req("insert{id: \"a\", name2: \"abc\"} into alias");
		req("insert{id: \"c\", name2: \"trical\"} into alias");

		// create cus, task, co tables
		adm("create cus(cnum, abbrev, name) key(cnum) key(abbrev)");
		req("insert { cnum: 1, abbrev: 'a', name: 'axon' } into cus");
		req("insert { cnum: 2, abbrev: 'b', name: 'bill' } into cus");
		req("insert { cnum: 3, abbrev: 'c', name: 'cron' } into cus");
		req("insert { cnum: 4, abbrev: 'd', name: 'dick' } into cus");
		adm("create task(tnum, cnum) key(tnum)");
		req("insert { tnum: 100, cnum: 1 } into task");
		req("insert { tnum: 101, cnum: 2 } into task");
		req("insert { tnum: 102, cnum: 3 } into task");
		req("insert { tnum: 103, cnum: 4 } into task");
		req("insert { tnum: 104, cnum: 1 } into task");
		req("insert { tnum: 105, cnum: 2 } into task");
		req("insert { tnum: 106, cnum: 3 } into task");
		req("insert { tnum: 107, cnum: 4 } into task");
		adm("create co(tnum, signed) key(tnum)");
		req("insert { tnum: 100, signed: 990101 } into co");
		req("insert { tnum: 102, signed: 990102 } into co");
		req("insert { tnum: 104, signed: 990103 } into co");
		req("insert { tnum: 106, signed: 990104 } into co");

		adm("create dates(date) key(date)");
		req("insert { date: #20010101 } into dates");
		req("insert { date: #20010102 } into dates");
		req("insert { date: #20010301 } into dates");
		req("insert { date: #20010401 } into dates");
	}

	protected void adm(String s) {
		Request.execute(s);
	}

	protected int req(String s) {
		Transaction tran = db.readwriteTran();
		try {
			Query q = CompileQuery.parse(tran, serverData, s);
			int n = ((QueryAction) q).execute();
			tran.ck_complete();
			return n;
		} finally {
			tran.abortIfNotComplete();
		}
	}

}
