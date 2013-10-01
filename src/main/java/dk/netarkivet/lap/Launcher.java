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
        long maxFileSize = 1024 * 1024 * 1024;
        String prefix = "LAP";
        int timeout = 10;
        boolean deduplication = true;
        String isPartOf = "";
        String description = "";
        String operator = "";
        String httpheader = "";
        boolean verbose = false;

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
        }

        if (dir == null) usage();

        if (verbose) {
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

        LAPWarcWriter w;
        /*
        if (arc)
            aw = new ArcWriter(host, port, new File(dir), compression, maxFileSize, prefix);
        else
            aw = new WarcWriter(host, port, new File(dir), compression, maxFileSize, prefix);
        */
        w = new LAPWarcWriter(host, port, new File(dir), prefix, compression, maxFileSize, deduplication, verbose,
                isPartOf, description, operator, httpheader);
        w.start(timeout);
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
