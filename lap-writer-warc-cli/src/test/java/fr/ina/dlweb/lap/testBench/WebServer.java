package fr.ina.dlweb.lap.testBench;

import org.mortbay.component.LifeCycle;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;

/**
 * Date: 23/11/12
 * Time: 11:36
 *
 * @author drapin
 */
public class WebServer {

	private Server webServer;

    public void stop() {
        try {
            webServer.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void start(final int port, final WebServerListener listener) {
        Runnable serverThread = new Runnable() {
            @Override
            public void run() {
                webServer = new Server(port);
                ServletHandler h = new ServletHandler();
                h.addServletWithMapping(WebServerServlet.class, "/*");
                webServer.addHandler(h);
                webServer.addLifeCycleListener(new LifeCycle.Listener() {
                    @Override
                    public void lifeCycleStarting(LifeCycle lifeCycle) {

                    }

                    @Override
                    public void lifeCycleStarted(LifeCycle lifeCycle) {
                        listener.onStarted(lifeCycle);
                    }

                    @Override
                    public void lifeCycleFailure(LifeCycle lifeCycle, Throwable throwable) {
                    }

                    @Override
                    public void lifeCycleStopping(LifeCycle lifeCycle) {
                    }

                    @Override
                    public void lifeCycleStopped(LifeCycle lifeCycle) {
                        listener.onStopped(lifeCycle);
                    }
                });
                try {

                    webServer.start();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        new Thread(serverThread, "web-server").start();
    }
}
