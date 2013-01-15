package dk.netarkivet.lap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jwat.common.RandomAccessFileOutputStream;
import org.jwat.common.Uri;
import org.jwat.warc.WarcConcurrentTo;
import org.jwat.warc.WarcConstants;
import org.jwat.warc.WarcHeader;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcWriter;
import org.jwat.warc.WarcWriterFactory;

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

    protected RandomAccessFile raf;

	protected RandomAccessFileOutputStream rafout;

	protected WarcWriter writer;

    protected boolean writerClosed = false;

    public LAPWarcWriter(String lapHost, int lapPort, File targetDir, boolean compression, long maxFileSize, String filePrefix) {
        super(lapHost, lapPort);
        List<File> targetDirs = Arrays.asList(targetDir);

        this.targetDir = targetDir;
        this.compression = compression;
        this.maxFileSize = maxFileSize;
        this.filePrefix = filePrefix;

        checkWritableDirs(targetDirs);

        List<String> metadata = new ArrayList<String>();
        metadata.add("Created by INA's Live Archiving Proxy Writer (" + getInfo().getWriterAgent() + ")");

        if (compression) {
        	extension = ".warc.gz";
        } else {
        	extension = ".warc";
        }

        WarcRecord record;
        WarcHeader header;

        String warcFields =
        		"software: Internet Archive Heritrix"
        		+ "host: archive.org"
        		+ "isPartOf: Internet Archive world harvest 2000-2004"
        		+ "description: Retrieved from Internet Archive November 2009 as arc-files, converted to warc-files december 2010."
        		+ "operator: Internet Archive"
        		+ "httpheader: Retrospektiv indsamling 2000-2004"
        		+ "format: WARC file version 1.0"
        		+ "conformsTo: http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf";

        try {
        	raf = new RandomAccessFile("lap" + extension, "rw");
        	rafout = new RandomAccessFileOutputStream(raf);
        	writer = WarcWriterFactory.getWriter(rafout, 8192, compression);

            byte[] warcFieldsBytes = warcFields.getBytes("ISO-8859-1");
            ByteArrayInputStream bin = new ByteArrayInputStream(warcFieldsBytes);

            Uri warcinfoRecordId = new Uri("urn:uuid:" + UUID.randomUUID());

            record = WarcRecord.createRecord(writer);
            header = record.header;
            header.warcTypeIdx = WarcConstants.RT_IDX_RESPONSE;
            header.warcDate = new Date();
            header.warcFilename = "lap" + extension;
            header.warcRecordIdUri = warcinfoRecordId;
            header.contentTypeStr = WarcConstants.CT_APP_WARC_FIELDS;
            header.contentLength = new Long(warcFieldsBytes.length);
            writer.writeHeader(record);
            writer.streamPayload(bin);
            writer.closeRecord();
        } catch (Exception e) {
            throw new RuntimeException(e);
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

	@Override
	public void onContent(DefaultMetadata metadata, InputStream data, String id,
			Long size, PersistenceListener listener) throws Exception {
        System.out.print(".");

        if (size != null) {

            // content type
            String contentType = "unknown";
            List<String> contentTypes = metadata.getResponseHeader("Content-Type");
            if (contentTypes != null) contentType = contentTypes.get(0);

            // uri
            String uri = metadata.getInfo("url") + "";

            // todo: add IP to the metadata (LAP)
            String ip = null;

            // timestamp
            long requestTimestamp = Long.parseLong(metadata.getInfo("request_time") + "");

            // content
            ByteArrayInputStream headers = new ByteArrayInputStream(metadata.getResponseHeaders().getBytes());
            SequenceInputStream fullResponseStream = new SequenceInputStream(headers, data);

            // content size
            long fullResponseSize = headers.available() + size;

            //createRecord(writer, uri, contentType, ip, requestTimestamp, fullResponseSize, fullResponseStream);

            WarcRecord record;
            WarcHeader header;

            record = WarcRecord.createRecord(writer);
            header = record.header;
            header.warcTypeIdx = WarcConstants.RT_IDX_RESPONSE;
            header.warcDate = new Date(requestTimestamp);
            //header.warcIpAddress = "1.2.3.4";
            Uri responseRecordId = new Uri("urn:uuid:" + UUID.randomUUID());
            header.warcRecordIdUri = responseRecordId;
            header.warcTargetUriStr = uri;
            header.contentTypeStr = "application/http; msgtype=response";
            header.contentLength = fullResponseSize;
            writer.writeHeader(record);
            writer.streamPayload(fullResponseStream);
            writer.closeRecord();

            String request = metadata.getRequestHeaders();
            if (request != null) {
                record = WarcRecord.createRecord(writer);
                header = record.header;
                header.warcTypeIdx = WarcConstants.RT_IDX_REQUEST;
                header.warcDate = new Date(requestTimestamp);
                //header.warcIpAddress = "1.2.3.4";
                header.warcRecordIdUri = new Uri("urn:uuid:" + UUID.randomUUID());
                header.addHeader(WarcConstants.FN_WARC_CONCURRENT_TO, responseRecordId, null);
                header.warcTargetUriStr = uri;
                header.contentTypeStr = "application/http; msgtype=request";
                byte[] requestBytes = request.getBytes("ISO-8859-1");
                header.contentLength = new Long(requestBytes.length);
                writer.writeHeader(record);
                ByteArrayInputStream bin = new ByteArrayInputStream(requestBytes);
                writer.streamPayload(bin);
                writer.closeRecord();
            }

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
        }
        listener.onDataPersisted(id);
	}

}
