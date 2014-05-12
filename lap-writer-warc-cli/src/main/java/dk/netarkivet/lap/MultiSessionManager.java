package dk.netarkivet.lap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MultiSessionManager implements SessionManagerInterface {

    protected String writerAgent;

    protected Map<String, SessionConfig> sessions = new HashMap<String, SessionConfig>();

    @Override
	public void setWriterAgent(String writerAgent) {
		this.writerAgent = writerAgent;
	}

    public synchronized void addSession(SessionConfig session) {
    	for (int i=0; i<session.ip.length; ++i) {
        	sessions.put(session.ip[i], session);
    	}
    }

    @Override
	public synchronized WarcWriterWrapper getWarcWriter(String ip) {
		SessionConfig session = sessions.get(ip);
		WarcWriterWrapper w3 = null;
		if (session != null) {
			w3 = session.w3;
	        if (w3 == null) {
	        	w3 = WarcWriterWrapper.getWarcWriterInstance(session.targetDir, session.filePrefix, session.bCompression, session.maxFileSize, session.bDeduplication,
	        			writerAgent, session.isPartOf, session.description, session.operator, session.httpheader);
	        	session.w3 = w3;
	        }
		}
		return w3;
	}

	@Override
	public synchronized void close() throws IOException {
	}

}
