package dk.netarkivet.lap;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;

import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class TestBDB2 {

	public static void main(String[] args) {
		TestBDB2 p = new TestBDB2();
		p.Main(args);
	}

	private Environment env;

	private Database catalogDb;

	private ClassCatalog catalog;

    private Database db;

    private SortedMap<String, Info> map;

    public void Main(String[] args) {
		EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setTransactional(false);
		envConfig.setLocking(false);
		envConfig.setSharedCache(true);
		envConfig.setAllowCreate(true);
		envConfig.setConfigParam(EnvironmentConfig.CLEANER_EXPUNGE, "true");
		envConfig.setConfigParam(EnvironmentConfig.ENV_RUN_CLEANER, "true");
		envConfig.setConfigParam(EnvironmentConfig.CLEANER_MIN_AGE, "1");
		envConfig.setConfigParam(EnvironmentConfig.CLEANER_MIN_FILE_UTILIZATION, "20");
		envConfig.setConfigParam(EnvironmentConfig.CLEANER_MIN_UTILIZATION, "80");

		System.out.println(envConfig.getCachePercent());

		String dir = ".";

		env = new Environment(new File(dir), envConfig);

		DatabaseConfig dbConfig = new DatabaseConfig();
		dbConfig.setTransactional(false);
		dbConfig.setAllowCreate(true);

		catalogDb = env.openDatabase(null, "deduplication_cdb", dbConfig);
		catalog = new StoredClassCatalog(catalogDb);

        // use Integer tuple binding for key entries
        TupleBinding<String> keyBinding = TupleBinding.getPrimitiveBinding(String.class);

        // use String serial binding for data entries
        SerialBinding<Info> dataBinding = new SerialBinding<Info>(catalog, Info.class);

        /*
		try {
			System.out.println(env.truncateDatabase(null, "deduplication_db", true));
		}
		catch (DatabaseNotFoundException e) {
		}
		*/

		db = env.openDatabase(null, "deduplication_db", dbConfig);

		System.out.println("--");

		map = new StoredSortedMap<String, Info>(db, keyBinding, dataBinding, true);

        Info info = map.get("size:digest");
        if (info == null) {
        	info = new Info();
        	map.put("size:digest", info);
        }

		Iterator<String> iter = info.urls.iterator();
		while (iter.hasNext()) {
			System.out.println(iter.next());
		}

		info.urls.add(Long.toString(System.currentTimeMillis()));

		map.put("size:digest", info);

		iter = info.urls.iterator();
		while (iter.hasNext()) {
			System.out.println(iter.next());
		}

		/*
		String[] strings = new String[] {"Hello", "Database", "World"};
		for (int i=0; i<strings.length; ++i) {
			System.out.println(set.contains(strings[i]));
			if (!set.contains(strings[i])) {
				set.add(strings[i]);
			}
		}
		*/

        /*
		Iterator<String> iter = set.iterator();
		while (iter.hasNext()) {
			System.out.println(iter.next());
		}
		*/

		//set.clear();

		/*
		try {
			Thread.sleep(5*60*1000);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		*/

		//db.sync();
		catalogDb.close();
		db.close();
		env.sync();
		env.cleanLog();
		env.close();
    }

    public static class Info implements Serializable {
    	Set<String> urls = new HashSet<String>();
    }

}
