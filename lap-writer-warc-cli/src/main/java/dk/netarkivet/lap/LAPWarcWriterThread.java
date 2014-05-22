package dk.netarkivet.lap;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LAPWarcWriterThread implements Runnable {

    /** Logging mechanism. */
    private static Logger logger = Logger.getLogger(LAPWarcWriterThread.class.getName());

    public int lapTimeout;

    public Thread t;

    public LAPWarcWriter w;

    public LAPWarcWriterThread(String lapHost, int lapPort, int lapTimeout, SessionManagerInterface sessionManager, boolean bVerbose) {
        w = new LAPWarcWriter(lapHost, lapPort, sessionManager, false);
        t = new Thread(this);
        t.start();
    }

    @Override
	public void run() {
		logger.log(Level.INFO, "Starting LAP Warc writer.");
        try {
            w.start(lapTimeout);
        } catch (Exception e) {
        	logger.log(Level.SEVERE, e.toString(), e);
        }
		logger.log(Level.INFO, "LAP Warc writer stopped.");
	}

}
