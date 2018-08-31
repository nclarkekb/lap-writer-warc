package dk.netarkivet.lap;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.collections.StoredKeySet;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class TestBDB {

	public static void main(String[] args) {
		TestBDB p = new TestBDB();
		p.Main(args);
	}

	Environment env;

	//private ClassCatalog catalog;

    private Database db;

    private Set<String> set;

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

		//Database catalogDb = env.openDatabase(null, "deduplication_cdb", dbConfig);
		//catalog = new StoredClassCatalog(catalogDb);

		TupleBinding<String> keyBinding = TupleBinding.getPrimitiveBinding(String.class);

		try {
			System.out.println(env.truncateDatabase(null, "deduplication_db", true));
		}
		catch (DatabaseNotFoundException e) {
		}

		db = env.openDatabase(null, "deduplication_db", dbConfig);

		set = new StoredKeySet<String>(db, keyBinding, true);

		String[] strings = new String[] {"Hello", "Database", "World"};
		for (int i=0; i<strings.length; ++i) {
			System.out.println(set.contains(strings[i]));
			if (!set.contains(strings[i])) {
				set.add(strings[i]);
			}
		}

		Iterator<String> iter = set.iterator();
		while (iter.hasNext()) {
			System.out.println(iter.next());
		}

		//set.clear();

		try {
			Thread.sleep(5*60*1000);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}

		//db.sync();
		db.close();
		env.sync();
		env.cleanLog();
		env.close();
    }

}
