package dk.netarkivet.lap;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;

import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class Deduplication {

    protected ClassCatalog catalog;

	protected Environment env = null;

	protected Database db = null;

    protected SortedMap<String, SizeDigest> map;

    public static class SizeDigest implements Serializable {
    	/**
		 * UID.
		 */
		private static final long serialVersionUID = -9015323801372972651L;

		public String key;
		public String recordId;
		public Set<String> urls = new HashSet<String>();
		public SizeDigest(String key) {
			this.key = key;
		}
    }

	public Deduplication() {
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

		Database catalogDb = env.openDatabase(null, "deduplication_cdb", dbConfig);
		catalog = new StoredClassCatalog(catalogDb);

		TupleBinding<String> keyBinding = TupleBinding.getPrimitiveBinding(String.class);

        SerialBinding<SizeDigest> dataBinding = new SerialBinding<SizeDigest>(catalog, SizeDigest.class);

		try {
			System.out.println(env.truncateDatabase(null, "deduplication_db", true));
		}
		catch (DatabaseNotFoundException e) {
		}

		db = env.openDatabase(null, "deduplication_db", dbConfig);

		map = new StoredSortedMap<String, SizeDigest>(db, keyBinding, dataBinding, true);
	}

	public SizeDigest lookup(String key) {
		SizeDigest sizeDigest = map.get(key);
		if (sizeDigest == null) {
			sizeDigest = new SizeDigest(key);
		}
		return sizeDigest;
	}

	public void persistSizeDigest(SizeDigest sizeDigest) {
		map.put(sizeDigest.key, sizeDigest);
	}

	public void close() {
		if (db != null) {
			db.close();
			db = null;
		}
		if (env != null) {
			env.sync();
			env.cleanLog();
			env.close();
			env = null;
		}
	}

}
