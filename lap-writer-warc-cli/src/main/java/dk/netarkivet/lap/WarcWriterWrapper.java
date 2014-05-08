package dk.netarkivet.lap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

import org.jwat.common.RandomAccessFileOutputStream;
import org.jwat.common.Uri;
import org.jwat.warc.WarcConstants;
import org.jwat.warc.WarcHeader;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcWriter;
import org.jwat.warc.WarcWriterFactory;

public class WarcWriterWrapper {

    private static final Logger logger = Logger.getLogger(WarcWriterWrapper.class.getName());

    protected static final String ACTIVE_SUFFIX = ".open";

    /*
     * Configuration.
     */

    protected File targetDir;

    protected String filePrefix;

    protected boolean bCompression;

    protected long maxFileSize;

    protected boolean bDeduplication;

    /*
     * Filename.
     */
    protected String date;

    protected int sequenceNr;

    protected String hostname;

    protected String extension;

    /*
     * File.
     */

    protected File writerFile;

    protected RandomAccessFile writer_raf;

    protected RandomAccessFileOutputStream writer_rafout;

    public WarcWriter writer;

    /*
     * Metadata.
     */

    protected String warcFields;

    public Uri warcinfoRecordId;

    private WarcWriterWrapper(File targetDir, String filePrefix, boolean bCompression, long maxFileSize, boolean bDeduplication) {
        this.targetDir = targetDir;
        this.filePrefix = filePrefix;
        this.bCompression = bCompression;
        this.maxFileSize = maxFileSize;
        this.bDeduplication = bDeduplication;
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
    }

    public static WarcWriterWrapper getWarcWriterInstance(File targetDir, String filePrefix, boolean bCompression, long maxFileSize, boolean bDeduplication,
    		String writerAgent, String isPartOf, String description,  String operator, String httpheader) {
		WarcWriterWrapper w3 = new WarcWriterWrapper(targetDir, filePrefix, bCompression, maxFileSize, bDeduplication);
        StringBuilder sb = new StringBuilder();
        sb.append("software");
        sb.append(": ");
        sb.append(writerAgent);
        sb.append("\r\n");
        sb.append("host");
        sb.append(": ");
        sb.append(w3.hostname);
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
        w3.warcFields = sb.toString();
		return w3;
	}

    public boolean deduplicate() {
    	return bDeduplication;
    }

    public void nextWriter() throws Exception {
    	boolean bNewWriter = false;
    	if (writer_raf == null) {
    		bNewWriter = true;
    	} else if (writer_raf.length() > maxFileSize) {
        	closeWriter();
    		bNewWriter = true;
    	}
    	if (bNewWriter) {
            String finishedFilename = filePrefix + "-" + date + "-" + String.format("%05d", sequenceNr++) + "-" + hostname + extension;
            String activeFilename = finishedFilename + ACTIVE_SUFFIX;
            File finishedFile = new File(targetDir, finishedFilename);
            writerFile = new File(targetDir, activeFilename);
            if (writerFile.exists()) {
                throw new IOException(writerFile + " already exists, will not overwrite");
            }
            if (finishedFile.exists()) {
                throw new IOException(finishedFile + " already exists, will not overwrite");
            }

            writer_raf = new RandomAccessFile(writerFile, "rw");
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
            header.warcFilename = finishedFilename;
            header.warcRecordIdUri = warcinfoRecordId;
            header.contentTypeStr = WarcConstants.CT_APP_WARC_FIELDS;
            header.contentLength = new Long(warcFieldsBytes.length);
            writer.writeHeader(record);
            writer.streamPayload(bin);
            writer.closeRecord();
            
            logger.info("created new warc for writing: " + writerFile.getAbsolutePath());
    	}
    }

    public void closeWriter() throws IOException {
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
        
        if (writerFile != null && writerFile.getName().endsWith(ACTIVE_SUFFIX)) {
            String finishedName = writerFile.getName().substring(0, writerFile.getName().length() - ACTIVE_SUFFIX.length());
            File finishedFile = new File(writerFile.getParent(), finishedName);
            if (finishedFile.exists()) {
                throw new IOException("unable to rename " + writerFile + " to " + finishedFile + " - destination file already exists");
            }
            boolean success = writerFile.renameTo(finishedFile);
            if (!success) {
                throw new IOException("unable to rename " + writerFile + " to " + finishedFile + " - unknown problem");
            }
            logger.info("closed " + finishedFile);
        }
        writerFile = null;
    }

}
