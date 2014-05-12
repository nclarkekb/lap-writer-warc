package dk.netarkivet.lap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.List;

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
        long maxFileSize = 1073741824L;
        String prefix = "LAP";
        int timeout = 10;
        boolean deduplication = true;
        String isPartOf = "";
        String description = "";
        String operator = "";
        String httpheader = "";
        boolean verbose = false;
        String config = null;

        for (int i=1; i<args.length; i++) {
            String[] arg = args[i].split("=", 2);
            String key = arg[0];
            String val = arg.length < 2 ? null : arg[1];

            if ("--dir".equals(key)) dir = val;
            if ("--prefix".equals(key)) prefix = val;
            if ("--compression".equals(key)) compression = Boolean.parseBoolean(val);
            if ("--compress".equals(key)) compression = Boolean.parseBoolean(val);
            if ("--max-file-size".equals(key)) maxFileSize = Long.parseLong(val);
            if ("--timeout".equals(key)) timeout = Integer.parseInt(val);
            if ("--deduplication".equals(key)) deduplication = Boolean.parseBoolean(val);
            if ("--ispartof".equals(key)) isPartOf = val;
            if ("--description".equals(key)) description = val;
            if ("--operator".equals(key)) operator = val;
            if ("--httpheader".equals(key)) httpheader = val;
            if ("--verbose".equals(key)) verbose = true;
            if ("--config".equals(key)) config = val;
        }

        WriterConfig wc = null;

        if (config != null) {
        	try {
            	File configFile = new File(config);
            	if (configFile.exists() && configFile.isFile()) {
                	FileInputStream fin = new FileInputStream(configFile);
            		PushbackInputStream pbin = new PushbackInputStream(fin, 8192);
                	wc = WriterConfig.getWriterConfig(pbin);
            	}
        	} catch (FileNotFoundException e) {
        		System.out.println("File not found: " + config);
        	}
        	if (wc == null) {
        		System.exit(1);
        	}
        }

        if (wc == null && dir == null) usage();

        if (wc == null && verbose) {
            String msg =
                    "          LAP: '" + host + ":" + port + "'\r\n" +
                    "          dir: '" + dir + "'\r\n" +
                    "       prefix: '" + prefix + "'\r\n" +
                    "     compress: '" + compression + "'\r\n" +
                    "max-file-size: '" + maxFileSize + "'\r\n" +
                    "      timeout: '" + timeout + "'\r\n" +
                    "deduplication: '" + deduplication + "'\r\n" +
                    "     ispartof: '" + isPartOf + "'\r\n" +
                    "  description: '" + description + "'\r\n" +
                    "     operator: '" + operator + "'\r\n" +
                    "   httpheader: '" + httpheader + "'\r\n" +
                    "      verbose: '" + verbose + "'\r\n" +
                    "";
            System.out.println(msg);
        }

        SessionManagerInterface sessionManager = null;
    	SessionConfig sessionConfig;

        if (wc == null) {
            File targetDir = new File(dir);

            List<File> targetDirs = Arrays.asList(targetDir);
            checkWritableDirs(targetDirs);

            sessionConfig = new SessionConfig(dir, targetDir, prefix, compression, maxFileSize, deduplication, isPartOf, description, operator, httpheader);
            sessionManager = new SessionManager(sessionConfig);
        } else {
        	sessionManager = new MultiSessionManager();
        	for (int i=0; i<wc.sessions.length; ++i) {
        		sessionConfig = wc.sessions[i];
        		sessionConfig.targetDir = new File(sessionConfig.dir);
                List<File> targetDirs = Arrays.asList(sessionConfig.targetDir);
                checkWritableDirs(targetDirs);
                ((MultiSessionManager)sessionManager).addSession(sessionConfig);
        	}
        }

        LAPWarcWriter w;
        /*
        if (arc)
            aw = new ArcWriter(host, port, new File(dir), compression, maxFileSize, prefix);
        else
            aw = new WarcWriter(host, port, new File(dir), compression, maxFileSize, prefix);
        */

        w = new LAPWarcWriter(host, port, sessionManager, verbose);
        w.start(timeout);
    }

    protected static void checkWritableDirs(List<File> dirs) {
        String errors = "";
        for (File dir : dirs) {
            if (!dir.isDirectory()) {
                errors += "Target is not a directory: '" + dir + "'\n";            }
            else if (!dir.canWrite()) {
                errors += "Target directory is not writable: '" + dir + "'\n";
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors);
        }
    }

    private static void usage() {
        String usage =
                "\nUsage:\r\n"
                + "lap-writer-arc lap-host:lap-port\r\n"
                + "  --dir=warcFileTargetDirectory\r\n"
                + "  [--prefix=fileNamesPrefix]\r\n"
                + "  [--compress=true|false]\r\n"
                + "  [--max-file-size=maxArcFileSize]\r\n"
                + "  [--timeout=connectionTimeout]\r\n"
                + "  [--deduplication=true|false]\r\n"
                + "  [--ispartof=warcinfo ispartof]\r\n"
                + "  [--description=warcinfo description]\r\n"
                + "  [--operator=warcinfo operator]\r\n"
                + "  [--httpheader=warcinfo httpheader]\r\n"
                + "  [--verbose] "
                ;
        System.out.println(usage);
        System.exit(1);
    }

}
