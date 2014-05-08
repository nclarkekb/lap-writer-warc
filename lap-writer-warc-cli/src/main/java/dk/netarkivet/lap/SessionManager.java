package dk.netarkivet.lap;

import java.io.File;
import java.io.IOException;

public class SessionManager implements SessionManagerInterface {

	protected WarcWriterWrapper w3;

    protected File targetDir;

    protected String filePrefix;

    protected boolean bCompression;

    protected long maxFileSize;

    protected boolean bDeduplication;

    protected String writerAgent;

    protected String isPartOf;

    protected String description;

    protected String operator;

    protected String httpheader;

	public SessionManager(File targetDir, String filePrefix, boolean bCompression, long maxFileSize, boolean bDeduplication,
			String isPartOf, String description, String operator, String httpheader) {
        this.targetDir = targetDir;
        this.filePrefix = filePrefix;
        this.bCompression = bCompression;
        this.maxFileSize = maxFileSize;
        this.bDeduplication = bDeduplication;
        this.isPartOf = isPartOf;
        this.description = description;
        this.operator = operator;
        this.httpheader = httpheader;
	}

	@Override
	public void setWriterAgent(String writerAgent) {
		this.writerAgent = writerAgent;
	}

	@Override
	public WarcWriterWrapper getWarcWriter(String ip) {
        if (w3 == null) {
        	w3 = WarcWriterWrapper.getWarcWriterInstance(targetDir, filePrefix, bCompression, maxFileSize, bDeduplication, writerAgent, isPartOf, description, operator, httpheader);
        }
		return w3;
	}

	@Override
	public void close() throws IOException {
		if (w3 != null) {
            w3.closeWriter();
            w3.writer = null;
            w3 = null;
		}
	}

}
