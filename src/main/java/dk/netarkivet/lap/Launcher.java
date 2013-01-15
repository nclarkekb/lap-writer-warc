package dk.netarkivet.lap;

import java.io.File;

import org.apache.log4j.BasicConfigurator;

public class Launcher {

	public static void main(String[] args) throws Exception {
		launch(args);
	}

	public static void launch(String[] args) throws Exception {
        BasicConfigurator.configure();
        if (args.length < 2) usage();

        String[] hostPort = args[0].split(":", 2);
        String host = hostPort[0];
        if (hostPort.length < 2) usage();
        int port = Integer.parseInt(hostPort[1]);

        String dir = null;
        /*
        boolean compression = ArcWriter.DEFAULT_COMPRESSION;
        long maxFileSize = ArcWriter.DEFAULT_MAX_FILE_SIZE;
        String prefix = ArcWriter.DEFAULT_PREFIX;
        */
        boolean compression = false;
        long maxFileSize = 1024 * 1024;
        String prefix = "LAP-";
        int timeout = 10;
        boolean verbose = true;

        for (int i=1; i<args.length; i++) {
            String[] arg = args[i].split("=", 2);
            String key = arg[0];
            String val = arg.length < 2 ? null : arg[1];

            if ("--dir".equals(key)) dir = val;
            if ("--compression".equals(key)) compression = Boolean.parseBoolean(val);
            if ("--max-file-size".equals(key)) maxFileSize = Long.parseLong(val);
            if ("--prefix".equals(key)) prefix = val;
            if ("--timeout".equals(key)) timeout = Integer.parseInt(val);
            if ("--verbose".equals(key)) verbose = true;
        }

        if (dir == null) usage();

        if (verbose) {
            String msg =
                    "LAP: '" + host + ":" + port + "', " +
                    "dir: '" + dir + "', " +
                    "compress: '" + compression + "', " +
                    "max-file-size: '" + maxFileSize + "', " +
                    "prefix: '" + prefix + "', " +
                    "timeout: '" + timeout + "', " +
                    "verbose: '" + verbose + "', " +
                    "";
            System.out.println(msg);
        }

        LAPWarcWriter w;
        /*
        if (arc)
            aw = new ArcWriter(host, port, new File(dir), compression, maxFileSize, prefix);
        else
            aw = new WarcWriter(host, port, new File(dir), compression, maxFileSize, prefix);
        */
        w = new LAPWarcWriter(host, port, new File(dir), compression, maxFileSize, prefix);
        w.start(timeout);
    }

    private static void usage() {
        String usage =
                "\nUsage:\n"
                + "lap-writer-arc lap-host:lap-port"
                + " --dir=arcFileTargetDirectory "
                + "[--compress=true|false] "
                + "[--max-file-size=maxArcFileSize] "
                + "[--prefix=fileNamesPrefix] "
                + "[--timeout=connectionTimeout] "
                + "[--verbose] "
                ;
        System.out.println(usage);
        System.exit(1);
    }

}
