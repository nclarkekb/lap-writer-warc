package dk.netarkivet.lap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MultiSessionManager implements SessionManagerInterface {

    protected String writerAgent;

    protected Map<String, SessionConfig> ipSessionConfigMap = new HashMap<String, SessionConfig>();

    @Override
	public void setWriterAgent(String writerAgent) {
		this.writerAgent = writerAgent;
	}

    public synchronized void addSession(SessionConfig sessionConfig) {
    	for (int i=0; i<sessionConfig.ip.length; ++i) {
        	ipSessionConfigMap.put(sessionConfig.ip[i], sessionConfig);
    	}
    }

    @Override
	public synchronized WarcWriterWrapper getWarcWriter(String ip) {
		SessionConfig sessionConfig = ipSessionConfigMap.get(ip);
		WarcWriterWrapper w3 = null;
		if (sessionConfig != null) {
			w3 = sessionConfig.w3;
	        if (w3 == null) {
	        	w3 = WarcWriterWrapper.getWarcWriterInstance(sessionConfig);
	        	sessionConfig.w3 = w3;
	        }
		}
		return w3;
	}

	@Override
	public synchronized void close() throws IOException {
	}

}
