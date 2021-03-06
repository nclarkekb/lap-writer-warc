package dk.netarkivet.lap;

import java.io.File;

import com.antiaction.common.json.annotation.JSONIgnore;
import com.antiaction.common.json.annotation.JSONName;
import com.antiaction.common.json.annotation.JSONNullable;

public class SessionConfig {

	@JSONIgnore
	protected WarcWriterWrapper w3;

	protected String dir;

	@JSONIgnore
    protected File targetDir;

	@JSONNullable
	protected String[] ip;

	@JSONName("prefix")
	@JSONNullable
    protected String filePrefix = "LAP";

	@JSONName("compression")
	@JSONNullable
    protected Boolean bCompression = false;

	@JSONName("max-file-size")
	@JSONNullable
    protected Long maxFileSize = 1073741824L;

	@JSONName("deduplication")
	@JSONNullable
    protected Boolean bDeduplication = true;

	@JSONIgnore
    protected String writerAgent;

	@JSONName("ispartof")
	@JSONNullable
    protected String isPartOf = "";

	@JSONNullable
    protected String description = "";

	@JSONNullable
    protected String operator = "";

	@JSONNullable
    protected String httpheader = "";

    public SessionConfig() {
    }

    public SessionConfig(String dir, File targetDir, String[] ip, String filePrefix, boolean bCompression, long maxFileSize, boolean bDeduplication,
			String isPartOf, String description, String operator, String httpheader) {
    	this.dir = dir;
        this.targetDir = targetDir;
    	this.ip = ip;
        this.filePrefix = filePrefix;
        this.bCompression = bCompression;
        this.maxFileSize = maxFileSize;
        this.bDeduplication = bDeduplication;
        this.isPartOf = isPartOf;
        this.description = description;
        this.operator = operator;
        this.httpheader = httpheader;
	}

}
