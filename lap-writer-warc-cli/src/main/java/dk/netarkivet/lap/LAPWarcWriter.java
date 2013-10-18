package dk.netarkivet.lap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.SequenceInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.logging.Logger;

import org.jwat.common.Base32;
import org.jwat.common.Base64;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.HeaderLine;
import org.jwat.common.HttpHeader;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.common.Uri;
import org.jwat.warc.WarcConstants;
import org.jwat.warc.WarcDigest;
import org.jwat.warc.WarcHeader;
import org.jwat.warc.WarcRecord;

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

    private static final Logger logger = Logger.getLogger(LAPWarcWriter.class.getName());

    /** Writer name and version. */
    protected static final String version = "LAP WARC writer v0.5";

    protected final String writerAgent;

    protected File targetDir;

    protected String filePrefix;

    protected boolean bCompression;

    protected long maxFileSize;

    protected boolean bDeduplication;

    protected boolean bVerbose;

    protected String isPartOf;

    protected String description;

    protected String operator;

    protected String httpheader;

    protected File tmpdir;

    protected Deduplication deduplication;

    protected boolean writerClosed = false;

    protected WarcWriterWrapper w3;

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

        this.isPartOf = isPartOf;
        this.description = description;
        this.operator = operator;
        this.httpheader = httpheader;

        writerAgent = "Created by INA's Live Archiving Proxy Writer (" + getInfo().getWriterAgent() + ")";

        checkWritableDirs(targetDirs);

        //List<String> metadata = new ArrayList<String>();
        //metadata.add("Created by INA's Live Archiving Proxy Writer (" + getInfo().getWriterAgent() + ")");

        String sys_tmpdir = System.getProperty("java.io.tmpdir");

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String ts = dateFormat.format(new Date());

        tmpdir = new File(sys_tmpdir, "LAP-" + ts);
        tmpdir.mkdirs();

        if (bDeduplication) {
            deduplication = new Deduplication(tmpdir);
        }
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

        logger.info("started !");
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
        if (w3 != null && !writerClosed) {
            w3.closeWriter();
            logger.info("closed writer");
            if (deduplication != null) {
                deduplication.close();
                deduplication = null;
            }
            System.out.println("Bye...");
            w3.writer = null;
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

    protected static final byte[] zeroArr = new byte[0];

    protected byte[] tmpBuf = new byte[8192];

    @Override
    public void onContent(DefaultMetadata metadata, InputStream data, String id,
            Long size, PersistenceListener listener) throws Exception {
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

    	/*
    	 * Handle empty payload.
    	 */
        if (size == null) {
        	size = 0L;
        }
        if (data == null) {
        	data = new ByteArrayInputStream(zeroArr);
        }
        /*
         * Timestamp.
         */
        long requestTimestamp = Long.parseLong(metadata.getInfo("request_time") + "");
        /*
         * Client IP.
         */
        String ip = new String(metadata.getInfo("request_ip") + "");
        if (ip == null || ip.length() == 0) {
        	ip = "0.0.0.0";
        }

    	/*
        // content type
        String contentType = "application/binary";
        List<String> contentTypes = metadata.getResponseHeader("Content-Type");
        if (contentTypes != null) {
            contentType = contentTypes.get(0);
        }
        */

        if (w3 == null) {
        	w3 = WarcWriterWrapper.getWarcWriterInstance(targetDir, filePrefix, bCompression, maxFileSize, writerAgent, isPartOf, description, operator, httpheader);
        }

        /*
         * Filter HTTP Response headers + WARC Content-Length.
         */

        byte[] responseHeaderBytes = filter(metadata.getResponseHeaders().getBytes(), size);

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

        w3.nextWriter();

        Uri responseOrRevisitRecordId;
        if (sizeDigest == null || sizeDigest.originalUrl == null || size == 0) {
            /*
             * Response content.
             */

            ByteArrayInputStream headersStream = new ByteArrayInputStream(responseHeaderBytes);
            SequenceInputStream fullResponseStream = new SequenceInputStream(headersStream, contentStream);

            /*
             * Response record.
             */

            warcBlockDigest = WarcDigest.createWarcDigest("SHA1", blockDigestBytes, "base32", Base32.encodeArray(blockDigestBytes));
            warcPayloadDigest = WarcDigest.createWarcDigest("SHA1", payloadDigestBytes, "base32", Base32.encodeArray(payloadDigestBytes));

            responseOrRevisitRecordId = new Uri("urn:uuid:" + UUID.randomUUID());

            record = WarcRecord.createRecord(w3.writer);
            header = record.header;
            header.warcTypeIdx = WarcConstants.RT_IDX_RESPONSE;
            header.warcDate = new Date(requestTimestamp);
            //header.warcIpAddress = "1.2.3.4";
            header.warcRecordIdUri = responseOrRevisitRecordId;
            header.warcWarcinfoIdUri = w3.warcinfoRecordId;
            header.warcTargetUriStr = uri;
            header.warcBlockDigest = warcBlockDigest;
            header.warcPayloadDigest = warcPayloadDigest;
            header.contentTypeStr = "application/http; msgtype=response";
            header.contentLength = fullResponseSize;
            w3.writer.writeHeader(record);
            w3.writer.streamPayload(fullResponseStream);
            w3.writer.closeRecord();

            if (sizeDigest != null) {
                sizeDigest.recordId = responseOrRevisitRecordId.toString();
                sizeDigest.payloadDigest = warcPayloadDigest.toString();
                sizeDigest.originalUrl = uri;
                sizeDigest.originalDate = header.warcDate;
                deduplication.persistSizeDigest(sizeDigest);
            }

            if (bVerbose) {
                System.out.println("Archiving: " + dedupKey + ":" + uri);
            }
        } else {
            record = WarcRecord.createRecord(w3.writer);
            header = record.header;
            header.warcTypeIdx = WarcConstants.RT_IDX_REVISIT;
            header.warcDate = new Date(requestTimestamp);
            //header.warcIpAddress = "1.2.3.4";
            responseOrRevisitRecordId = new Uri("urn:uuid:" + UUID.randomUUID());
            header.warcRecordIdUri = responseOrRevisitRecordId;
            header.warcWarcinfoIdUri = w3.warcinfoRecordId;
            header.warcTargetUriStr = uri;
            header.warcRefersToUri = new Uri(sizeDigest.recordId);
            header.warcPayloadDigest = WarcDigest.parseWarcDigest(sizeDigest.payloadDigest);
            header.warcProfileUri = new Uri("http://netpreserve.org/warc/0.18/revisit/identical-payload-digest");
            header.contentLength = (long) responseHeaderBytes.length;
            header.warcRefersToTargetUriStr = sizeDigest.originalUrl;
            header.warcRefersToDate = sizeDigest.originalDate;

            w3.writer.writeHeader(record);
            w3.writer.writePayload(responseHeaderBytes);
            w3.writer.closeRecord();

            if (bVerbose) {
                System.out.println("Duplicate: " + dedupKey + ":" + uri);
            }
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

            record = WarcRecord.createRecord(w3.writer);
            header = record.header;
            header.warcTypeIdx = WarcConstants.RT_IDX_REQUEST;
            header.warcDate = new Date(requestTimestamp);
            //header.warcIpAddress = "1.2.3.4";
            header.warcRecordIdUri = new Uri("urn:uuid:" + UUID.randomUUID());
            header.addHeader(WarcConstants.FN_WARC_CONCURRENT_TO, responseOrRevisitRecordId, null);
            header.warcWarcinfoIdUri = w3.warcinfoRecordId;
            header.warcTargetUriStr = uri;
            header.warcBlockDigest = warcBlockDigest;
            header.contentTypeStr = "application/http; msgtype=request";
            header.contentLength = new Long(requestHeaderBytes.length);
            w3.writer.writeHeader(record);
            ByteArrayInputStream bin = new ByteArrayInputStream(requestHeaderBytes);
            w3.writer.streamPayload(bin);
            w3.writer.closeRecord();
        }

        if (tmpfile_raf != null) {
            tmpfile_raf.seek(0L);
            tmpfile_raf.setLength(0L);
            tmpfile_raf.close();
        }

        listener.onDataPersisted(id);
    }

    public static byte[] filter(byte[] responseHeaderBytes, long contentLength) throws IOException {
    	HttpHeader httpHeader = HttpHeader.processPayload(HttpHeader.HT_RESPONSE,
        		new ByteCountingPushBackInputStream(new ByteArrayInputStream(responseHeaderBytes), 8192),
        		responseHeaderBytes.length,
        		null);
    	httpHeader.close();
        if (httpHeader != null && httpHeader.isValid()) {
        	List<HeaderLine> headerLines = httpHeader.getHeaderList();
        	HeaderLine headerLine;

        	ByteArrayOutputStream out = new ByteArrayOutputStream();
        	out.write((httpHeader.httpVersion + " " + httpHeader.statusCodeStr + " " + httpHeader.reasonPhrase + "\r\n").getBytes());

        	boolean bContentLengthPresent = false;
        	for (int i=0; i<headerLines.size(); ++i) {
        		headerLine = headerLines.get(i);
        		if ("content-length".equalsIgnoreCase(headerLine.name)) {
        			out.write((headerLine.name + ": " + Long.toString(contentLength) + "\r\n").getBytes());
        			bContentLengthPresent = true;
        		} else if ("content-encoding".equalsIgnoreCase(headerLine.name)) {
        		} else if ("transfer-encoding".equalsIgnoreCase(headerLine.name)) {
        		} else {
        			out.write((headerLine.name + ": " + headerLine.value + "\r\n").getBytes());
        		}
        	}
        	if (!bContentLengthPresent) {
    			out.write(("Content-Length: " + Long.toString(contentLength) + "\r\n").getBytes());
        	}

        	out.write("\r\n".getBytes());

        	responseHeaderBytes = out.toByteArray();
        }
        return responseHeaderBytes;
    }

}
