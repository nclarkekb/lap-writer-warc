package dk.netarkivet.lap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.SequenceInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

import org.jwat.common.Base32;
import org.jwat.common.Base64;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.common.RandomAccessFileOutputStream;
import org.jwat.common.Uri;
import org.jwat.warc.WarcConstants;
import org.jwat.warc.WarcDigest;
import org.jwat.warc.WarcHeader;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcWriter;
import org.jwat.warc.WarcWriterFactory;

import dk.netarkivet.lap.Deduplication.SizeDigest;
import fr.ina.dlweb.lap.writer.DefaultLapWriter;
import fr.ina.dlweb.lap.writer.PersistenceListener;
import fr.ina.dlweb.lap.writer.metadata.DefaultMetadata;
import fr.ina.dlweb.lap.writer.writerInfo.DefaultWriterInfo;
import fr.ina.dlweb.lap.writer.writerInfo.WriterInfo;
import fr.ina.dlweb.lap.writer.writerInfo.WriterInfoResponse;

/**
 * A WARC writer for INA's Live Archiving Proxy (LAP).
 * 
 * @author nicl
 */
public class LAPWarcWriter extends DefaultLapWriter {

    /** Writer name and version. */
    protected String version = "LAP WARC writer v0.5";

    protected File targetDir;

    protected String filePrefix;

    protected boolean bCompression;

    protected long maxFileSize;

    protected boolean bDeduplication;

    protected boolean bVerbose;

    protected String date;

    protected int sequenceNr;

    protected String hostname;

    protected String extension;

    protected String warcFields;

    protected File tmpdir;

    protected Deduplication deduplication;

    protected RandomAccessFile writer_raf;

    protected RandomAccessFileOutputStream writer_rafout;

    protected WarcWriter writer;

    protected Uri warcinfoRecordId;

    protected boolean writerClosed = false;

    public LAPWarcWriter(String lapHost, int lapPort, File targetDir, String filePrefix, boolean bCompression, long maxFileSize, boolean bDeduplication, boolean bVerbose,
            String isPartOf, String description,  String operator, String httpheader) {
        super(lapHost, lapPort);
        List<File> targetDirs = Arrays.asList(targetDir);

        this.targetDir = targetDir;
        this.filePrefix = filePrefix;
        this.bCompression = bCompression;
        this.maxFileSize = maxFileSize;
        this.bDeduplication = bDeduplication;
        this.bVerbose = bVerbose;

        checkWritableDirs(targetDirs);

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        date = dateFormat.format(new Date());
        sequenceNr = 0;
        try {
            hostname = InetAddress.getLocalHost().getHostName().toLowerCase();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        if (bCompression) {
            extension = ".warc.gz";
        } else {
            extension = ".warc";
        }

        List<String> metadata = new ArrayList<String>();
        metadata.add("Created by INA's Live Archiving Proxy Writer (" + getInfo().getWriterAgent() + ")");

        StringBuilder sb = new StringBuilder();
        sb.append("software");
        sb.append(": ");
        sb.append(version);
        sb.append("\r\n");
        sb.append("host");
        sb.append(": ");
        sb.append(hostname);
        sb.append("\r\n");
        if (isPartOf != null && isPartOf.length() > 0) {
            sb.append("isPartOf");
            sb.append(": ");
            sb.append(isPartOf);
            sb.append("\r\n");
        }
        if (description != null && description.length() > 0) {
            sb.append("description");
            sb.append(": ");
            sb.append(description);
            sb.append("\r\n");
        }
        if (operator != null && operator.length() > 0) {
            sb.append("operator");
            sb.append(": ");
            sb.append(operator);
            sb.append("\r\n");
        }
        if (httpheader != null && httpheader.length() > 0) {
            sb.append("httpheader");
            sb.append(": ");
            sb.append(httpheader);
            sb.append("\r\n");
        }
        sb.append("format");
        sb.append(": ");
        sb.append("WARC file version 1.0");
        sb.append("\r\n");
        sb.append("conformsTo");
        sb.append(": ");
        sb.append("http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf");
        sb.append("\r\n");
        warcFields = sb.toString();

        String sys_tmpdir = System.getProperty("java.io.tmpdir");

        dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String ts = dateFormat.format(new Date());

        tmpdir = new File(sys_tmpdir, "LAP-" + ts);
        tmpdir.mkdirs();

        if (bDeduplication) {
            deduplication = new Deduplication(tmpdir);
        }

        try {
            nextWriter();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void closeWriter() throws IOException {
        if (writer != null) {
            writer.close();
            writer = null;
        }
        if (writer_rafout != null) {
            writer_rafout.close();
            writer_rafout = null;
        }
        if (writer_raf != null) {
            writer_raf.close();
            writer_raf = null;
        }
        warcinfoRecordId = null;
    }

    protected void nextWriter() throws Exception {
        closeWriter();

        String filename = filePrefix + "-" + date + "-" + String.format("%05d", sequenceNr++) + "-" + hostname + extension;
        File file = new File(targetDir, filename);
        if (file.exists()) {
            if (!file.delete()) {
                System.out.println("Could not delete old file!");
            }
        }

        writer_raf = new RandomAccessFile(file, "rw");
        writer_raf.seek(0L);
        writer_raf.setLength(0L);
        writer_rafout = new RandomAccessFileOutputStream(writer_raf);
        writer = WarcWriterFactory.getWriter(writer_rafout, 8192, bCompression);

        byte[] warcFieldsBytes = warcFields.getBytes("ISO-8859-1");
        ByteArrayInputStream bin = new ByteArrayInputStream(warcFieldsBytes);

        warcinfoRecordId = new Uri("urn:uuid:" + UUID.randomUUID());

        WarcRecord record;
        WarcHeader header;

        record = WarcRecord.createRecord(writer);
        header = record.header;
        header.warcTypeIdx = WarcConstants.RT_IDX_WARCINFO;
        header.warcDate = new Date();
        header.warcFilename = filename;
        header.warcRecordIdUri = warcinfoRecordId;
        header.contentTypeStr = WarcConstants.CT_APP_WARC_FIELDS;
        header.contentLength = new Long(warcFieldsBytes.length);
        writer.writeHeader(record);
        writer.streamPayload(bin);
        writer.closeRecord();
    }

    protected void checkWritableDirs(List<File> dirs) {
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

    @Override
    public WriterInfo getInfo() {
        return new DefaultWriterInfo(version);
    }

    @Override
    protected void onStarted(final WriterInfoResponse response) {
        super.onStarted(response);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    stopWriter(false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                System.err.println("Uncaught exception in thread '" + t + "'");
                e.printStackTrace();
                try {
                    stopWriter(true);
                } catch (IOException e1) {
                    throw new RuntimeException(e);
                }
            }
        });

        log.info("started !");
    }

    @Override
    protected void onStopped(boolean error) {
        super.onStopped(error);
        try {
            stopWriter(error);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void stopWriter(boolean error) throws IOException {
        if (writer != null && !writerClosed) {
            log.info("closed writer");
            closeWriter();
            if (deduplication != null) {
                deduplication.close();
                deduplication = null;
            }
            System.out.println("Bye...");
            writer = null;
            writerClosed = true;
        }
        if (tmpdir.exists()) {
            deleteDirectory(tmpdir.getPath());
        }
    }

    protected void deleteDirectory(String root) {
        Stack<String> dirStack = new Stack<String>();
        dirStack.push(root);
        while(!dirStack.empty()) {
            String dir = dirStack.pop();
            File f = new File(dir);
            if(f.listFiles().length==0) {
                f.delete();
            }
            else {
                dirStack.push(dir);
                for(File ff: f.listFiles()) {
                    if(ff.isFile()) {
                        ff.delete();
                    }
                    else if(ff.isDirectory()) {
                        dirStack.push(ff.getPath());
                    }
                }
            }
        }
    }

    protected ByteArrayOutputStream out = new ByteArrayOutputStream(1024*1024);

    protected byte[] tmpBuf = new byte[8192];

    @Override
    public void onContent(DefaultMetadata metadata, InputStream data, String id,
            Long size, PersistenceListener listener) throws Exception {
        //System.out.print(".");

        if (size != null) {
            // content type
            String contentType = "application/binary";
            List<String> contentTypes = metadata.getResponseHeader("Content-Type");
            if (contentTypes != null) {
                contentType = contentTypes.get(0);
            }

            // todo: add IP to the metadata (LAP)
            String ip = null;

            // timestamp
            long requestTimestamp = Long.parseLong(metadata.getInfo("request_time") + "");

            // content
            byte[] responseHeaderBytes = metadata.getResponseHeaders().getBytes();
            long fullResponseSize = responseHeaderBytes.length + size;

            // uri
            String uri = metadata.getInfo("url") + "";

            /*
             * Response digest.
             */

            InputStream contentStream;

            byte[] blockDigestBytes;
            byte[] payloadDigestBytes;

            MessageDigest blockDigestObj = null;
            MessageDigest payloadDigestObj = null;
            try {
                blockDigestObj = MessageDigest.getInstance("SHA1");
                payloadDigestObj = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            blockDigestObj.reset();
            blockDigestObj.update(responseHeaderBytes);
            payloadDigestObj.reset();

            RandomAccessFile tmpfile_raf = null; 

            int read;
            if (size <= (1024*1024)) {
                out.reset();
                while ((read = data.read(tmpBuf)) > 0) {
                    blockDigestObj.update(tmpBuf, 0, read);
                    payloadDigestObj.update(tmpBuf, 0, read);
                    out.write(tmpBuf, 0, read);
                }
                out.close();
                contentStream = new ByteArrayInputStream(out.toByteArray());
                out.reset();
            } else {
                tmpfile_raf = new RandomAccessFile(new File(tmpdir, "temp-content.dat"), "rw");
                tmpfile_raf.seek(0L);
                tmpfile_raf.setLength(0L);
                while ((read = data.read(tmpBuf)) > 0) {
                    blockDigestObj.update(tmpBuf, 0, read);
                    payloadDigestObj.update(tmpBuf, 0, read);
                    tmpfile_raf.write(tmpBuf, 0, read);
                }
                tmpfile_raf.seek(0L);
                contentStream = new RandomAccessFileInputStream(tmpfile_raf);
            }
            data.close();

            blockDigestBytes = blockDigestObj.digest();
            payloadDigestBytes = payloadDigestObj.digest();
            WarcDigest warcBlockDigest;
            WarcDigest warcPayloadDigest;
            WarcRecord record;
            WarcHeader header;

            String dedupKey = Base64.encodeArray(payloadDigestBytes) + ":" + size;

            SizeDigest sizeDigest = null;
            if (deduplication != null) {
                sizeDigest = deduplication.lookup(dedupKey);
            }

            if (sizeDigest == null || sizeDigest.urls.size() == 0) {
                /*
                 * Response content.
                 */

                ByteArrayInputStream headersStream = new ByteArrayInputStream(responseHeaderBytes);
                SequenceInputStream fullResponseStream = new SequenceInputStream(headersStream, contentStream);

                /*
                 * Response record.
                 */

                if (writer_raf.length() > maxFileSize) {
                    nextWriter();
                }

                warcBlockDigest = WarcDigest.createWarcDigest("SHA1", blockDigestBytes, "base32", Base32.encodeArray(blockDigestBytes));
                warcPayloadDigest = WarcDigest.createWarcDigest("SHA1", payloadDigestBytes, "base32", Base32.encodeArray(payloadDigestBytes));

                Uri responseRecordId = new Uri("urn:uuid:" + UUID.randomUUID());

                record = WarcRecord.createRecord(writer);
                header = record.header;
                header.warcTypeIdx = WarcConstants.RT_IDX_RESPONSE;
                header.warcDate = new Date(requestTimestamp);
                //header.warcIpAddress = "1.2.3.4";
                header.warcRecordIdUri = responseRecordId;
                header.warcWarcinfoIdUri = warcinfoRecordId;
                header.warcTargetUriStr = uri;
                header.warcBlockDigest = warcBlockDigest;
                header.warcPayloadDigest = warcPayloadDigest;
                header.contentTypeStr = "application/http; msgtype=response";
                header.contentLength = fullResponseSize;
                writer.writeHeader(record);
                writer.streamPayload(fullResponseStream);
                writer.closeRecord();

                if (sizeDigest != null) {
                    sizeDigest.recordId = responseRecordId.toString();
                    sizeDigest.payloadDigest = warcPayloadDigest.toString();
                }

                /*
                 * Request.
                 */

                String requestHeader = metadata.getRequestHeaders();
                if (requestHeader != null) {
                    /*
                     * Digest.
                     */

                    byte[] requestHeaderBytes = requestHeader.getBytes("ISO-8859-1");

                    blockDigestObj.reset();
                    blockDigestObj.update(requestHeaderBytes);

                    blockDigestBytes = blockDigestObj.digest();
                    warcBlockDigest = WarcDigest.createWarcDigest("SHA1", blockDigestBytes, "base32", Base32.encodeArray(blockDigestBytes));

                    /*
                     * Content.
                     */

                    record = WarcRecord.createRecord(writer);
                    header = record.header;
                    header.warcTypeIdx = WarcConstants.RT_IDX_REQUEST;
                    header.warcDate = new Date(requestTimestamp);
                    //header.warcIpAddress = "1.2.3.4";
                    header.warcRecordIdUri = new Uri("urn:uuid:" + UUID.randomUUID());
                    header.addHeader(WarcConstants.FN_WARC_CONCURRENT_TO, responseRecordId, null);
                    header.warcWarcinfoIdUri = warcinfoRecordId;
                    header.warcTargetUriStr = uri;
                    header.warcBlockDigest = warcBlockDigest;
                    header.contentTypeStr = "application/http; msgtype=request";
                    header.contentLength = new Long(requestHeaderBytes.length);
                    writer.writeHeader(record);
                    ByteArrayInputStream bin = new ByteArrayInputStream(requestHeaderBytes);
                    writer.streamPayload(bin);
                    writer.closeRecord();
                }

                /*
                 * debug
                 */

                /*
                System.out.println(metadata.getId());
                System.out.println(metadata.getRequestHeaders());
                System.out.println(metadata.getResponseHeaders());
                Map<String, ?> map;
                Iterator<?> iter;
                Map.Entry entry;
                map = metadata.getInfo();
                iter = map.entrySet().iterator();
                while (iter.hasNext()) {
                    entry = (Map.Entry)iter.next();
                    System.out.println(entry.getKey());
                    System.out.println(entry.getValue());
                }
                map = metadata.getRequestInfo();
                iter = map.entrySet().iterator();
                while (iter.hasNext()) {
                    entry = (Map.Entry)iter.next();
                    System.out.println(entry.getKey());
                    System.out.println(entry.getValue());
                }
                */

                if (deduplication != null) {
                    sizeDigest.urls.add(uri);
                    deduplication.persistSizeDigest(sizeDigest);
                }

                if (bVerbose) {
                    System.out.println("Archiving: " + dedupKey + ":" + uri);
                }
            } else {
                if (!sizeDigest.urls.contains(uri)) {
                    if (writer_raf.length() > maxFileSize) {
                        nextWriter();
                    }

                    record = WarcRecord.createRecord(writer);
                    header = record.header;
                    header.warcTypeIdx = WarcConstants.RT_IDX_REVISIT;
                    header.warcDate = new Date(requestTimestamp);
                    //header.warcIpAddress = "1.2.3.4";
                    header.warcRecordIdUri = new Uri("urn:uuid:" + UUID.randomUUID());
                    header.warcWarcinfoIdUri = warcinfoRecordId;
                    header.warcTargetUriStr = uri;
                    header.warcRefersToUri = new Uri(sizeDigest.recordId);
                    header.warcPayloadDigest = WarcDigest.parseWarcDigest(sizeDigest.payloadDigest);
                    header.warcProfileUri = new Uri("http://netpreserve.org/warc/1.0/revisit/uri-agnostic-identical-payload-digest");
                    header.contentLength = 0L;
                    writer.writeHeader(record);
                    writer.closeRecord();

                    if (deduplication != null) {
                        sizeDigest.urls.add(uri);
                        deduplication.persistSizeDigest(sizeDigest);
                    }

                    if (bVerbose) {
                        System.out.println("Duplicate: " + dedupKey + ":" + uri);
                    }
                } else {
                    if (bVerbose) {
                        System.out.println("Identical: " + dedupKey + ":" + uri);
                    }
                }
            }
            if (tmpfile_raf != null) {
                tmpfile_raf.seek(0L);
                tmpfile_raf.setLength(0L);
                tmpfile_raf.close();
            }
        }
        listener.onDataPersisted(id);
    }

}
