package dk.netarkivet.lap;

import java.io.IOException;

public interface SessionManagerInterface {

	public void setWriterAgent(String writerAgent);

	public WarcWriterWrapper getWarcWriter(String ip);

	public void close() throws IOException;

}
