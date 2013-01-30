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

public class LAPWarcWriter extends DefaultLapWriter {

	protected String version = "LAP WARC writer v0.1";

    protected File targetDir;

    protected boolean compression;

    protected long maxFileSize;

    protected String filePrefix;

    protected String extension;

    protected String warcFields;

	protected String date;

	protected int sequenceNr;

	protected String hostname;

	protected Deduplication deduplication;

	protected RandomAccessFile raf;

	protected RandomAccessFileOutputStream rafout;

	protected WarcWriter writer;

	protected Uri warcinfoRecordId;

	protected boolean writerClosed = false;

    public LAPWarcWriter(String lapHost, int lapPort, File targetDir, boolean compression, long maxFileSize, String filePrefix) {
        super(lapHost, lapPort);
        List<File> targetDirs = Arrays.asList(targetDir);

        this.targetDir = targetDir;
        this.compression = compression;
        this.maxFileSize = maxFileSize;
        this.filePrefix = filePrefix;

        checkWritableDirs(targetDirs);

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        date = dateFormat.format(new Date());
        sequenceNr = 0;
        try {
			hostname = InetAddress.getLocalHost().getHostName().toLowerCase();
		} catch (UnknownHostException e) {
            throw new RuntimeException(e);
		}

        List<String> metadata = new ArrayList<String>();
        metadata.add("Created by INA's Live Archiving Proxy Writer (" + getInfo().getWriterAgent() + ")");

        warcFields =
        		"software: Internet Archive Heritrix\r\n"
        		+ "host: archive.org\r\n"
        		+ "isPartOf: Internet Archive world harvest 2000-2004\r\n"
        		+ "description: Retrieved from Internet Archive November 2009 as arc-files, converted to warc-files december 2010.\r\n"
        		+ "operator: Internet Archive\r\n"
        		+ "httpheader: Retrospektiv indsamling 2000-2004\r\n"
        		+ "format: WARC file version 1.0\r\n"
        		+ "conformsTo: http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf\r\n";

        if (compression) {
        	extension = ".warc.gz";
        } else {
        	extension = ".warc";
        }

        deduplication = new Deduplication();

        try {
        	nextWriter();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void closeWriter() throws Exception {
    	if (writer != null) {
    		writer.close();
    		writer = null;
    	}
    	if (rafout != null) {
    		rafout.close();
    		rafout = null;
    	}
    	if (raf != null) {
    		raf.close();
    		raf = null;
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

    	raf = new RandomAccessFile(file, "rw");
    	raf.seek(0L);
    	raf.setLength(0L);
    	rafout = new RandomAccessFileOutputStream(raf);
    	writer = WarcWriterFactory.getWriter(rafout, 8192, compression);

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
    protected void onStarted(final WriterInfoResponse response) {
        super.onStarted(response);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    stopWriter(false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }));

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

    protected synchronized void stopWriter(boolean error) throws IOException {
        if (writer != null && !writerClosed) {
        	// TODO
            //stopWriter(writer, error);
            System.out.println("");
            log.info("closed writer");
            writerClosed = true;
        }
    }

	public WriterInfo getInfo() {
        return new DefaultWriterInfo(version);
	}

    protected ByteArrayOutputStream out = new ByteArrayOutputStream(1024*1024);

    protected byte[] tmpBuf = new byte[8192];

    @Override
	public synchronized void onContent(DefaultMetadata metadata, InputStream data, String id,
			Long size, PersistenceListener listener) throws Exception {
    	//System.out.print(".");

        if (size != null) {
            // content type
            String contentType = "unknown";
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
            	RandomAccessFile raf = new RandomAccessFile("temp-content.tmp", "rw");
            	raf.seek(0L);
            	raf.setLength(0L);
            	while ((read = data.read(tmpBuf)) > 0) {
            		blockDigestObj.update(tmpBuf, 0, read);
            		payloadDigestObj.update(tmpBuf, 0, read);
            		raf.write(tmpBuf, 0, read);
            	}
            	raf.seek(0L);
            	contentStream = new RandomAccessFileInputStream(raf);
            }
        	data.close();

            blockDigestBytes = blockDigestObj.digest();
            payloadDigestBytes = payloadDigestObj.digest();

            String dedupKey = Base64.encodeArray(payloadDigestBytes) + ":" + size + ":" + uri;

            SizeDigest sizeDigest = null;
            if (deduplication != null) {
                sizeDigest = deduplication.lookup(dedupKey);
            }

            if (sizeDigest == null || sizeDigest.urls.size() == 0) {
        		System.out.println("Archiving: " + uri + " (" + dedupKey + ")");

        		/*
                 * Response content.
                 */

                ByteArrayInputStream headersStream = new ByteArrayInputStream(responseHeaderBytes);
                SequenceInputStream fullResponseStream = new SequenceInputStream(headersStream, contentStream);

                /*
                 * Response record.
                 */

                if (raf.length() > maxFileSize) {
                	nextWriter();
                }

                //createRecord(writer, uri, contentType, ip, requestTimestamp, fullResponseSize, fullResponseStream);

                WarcRecord record;
                WarcHeader header;
                WarcDigest warcBlockDigest;
                WarcDigest warcPayloadDigest;

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

                sizeDigest.urls.add(uri);
                deduplication.persistSizeDigest(sizeDigest);
            } else {
            	if (sizeDigest.urls.contains(uri)) {
            	}
            	// "http://netpreserve.org/warc/1.0/revisit/uri-agnostic-identical-payload-digest". 
            	System.out.println("Duplicate: " + uri + " (" + dedupKey + ")");
            }
        }
        listener.onDataPersisted(id);
	}

}
