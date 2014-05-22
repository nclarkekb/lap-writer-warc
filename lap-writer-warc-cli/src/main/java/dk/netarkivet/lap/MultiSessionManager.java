package dk.netarkivet.lap;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MultiSessionManager implements SessionManagerInterface {

    protected String writerAgent;

    protected List<SessionConfig> sessionConfigList = new LinkedList<SessionConfig>();

    protected Map<String, SessionConfig> ipSessionConfigMap = new HashMap<String, SessionConfig>();

    @Override
	public void setWriterAgent(String writerAgent) {
		this.writerAgent = writerAgent;
	}

    public synchronized void addSession(SessionConfig sessionConfig) {
    	sessionConfigList.add(sessionConfig);
    	for (int i=0; i<sessionConfig.ip.length; ++i) {
        	ipSessionConfigMap.put(sessionConfig.ip[i], sessionConfig);
    	}
    }

    public List<SessionConfig> getSessionConfigs() {
    	return sessionConfigList;
    }

    @Override
	public synchronized WarcWriterWrapper getWarcWriter(String ip) {
		SessionConfig sessionConfig = ipSessionConfigMap.get(ip);
		WarcWriterWrapper w3 = null;
		if (sessionConfig != null) {
			w3 = sessionConfig.w3;
	        if (w3 == null) {
	        	sessionConfig.writerAgent = writerAgent;
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
