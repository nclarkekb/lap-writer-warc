package fr.ina.dlweb.lap.testBench;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.mortbay.component.LifeCycle;

import fr.ina.dlweb.lap.writer.metadata.DefaultMetadata;
import fr.ina.dlweb.lap.writer.writerInfo.WriterInfoResponse;

/**
 * Date: 22/11/12
 * Time: 17:09
 *
 * @author drapin
 */
public class Bench {
    private static final Logger log = Logger.getLogger(Bench.class);
    private final String prefix = "";

    private final String lapBinary;
    private final String digest;
    private final int webServerPort;
    private final int lapPort;
    private final int writerPort;
    private final String host;

    private final Lap lap;
    private final WebServer server;
    private final LapWriter writer;
    private final HttpClient client;

    private int tests = 0;
    private final int testsCount = 4;

    public static void main(String[] args) {
        try {
            PropertyConfigurator.configure(Bench.class.getResource("log4j.properties"));
            Bench bench = new Bench("./lap", "SHA-256", 8181, 8282, 8383);
            bench.start();
            //Thread.sleep(30 * 1000);
        } catch (Exception e) {
            log.error(null, e);
        }
    }

    public Bench(String lapBinary, String digest, int webServerPort, int lapPort, int writerPort) {
        this.lapBinary = lapBinary;
        this.digest = digest;
        this.webServerPort = webServerPort;
        this.lapPort = lapPort;
        this.writerPort = writerPort;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        lap = new Lap();
        server = new WebServer();
        writer = new LapWriter();
        client = new HttpClient();
        client.getHostConfiguration().setProxy(host, lapPort);
    }

    public void start() {
        lap.start(host, lapBinary, lapPort, writerPort, digest, new LapListener() {
            @Override
            public void onLapStarted() {

                log.info(prefix + "LAP started");
                server.start(webServerPort, new WebServerListener() {

                    @Override
                    public void onStarted(LifeCycle lifeCycle) {

                        log.info(prefix + "WebServer started (" + lifeCycle.getClass() + ")");
                        writer.start(host, writerPort, digest, new LapWriterListener() {

                            @Override
                            public void onStarted(WriterInfoResponse response) {
                                log.info(prefix + "Writer started (" + response.getVersion() + ")");

                                httpGet(host, webServerPort, "/index.html");
                                httpGet(host, webServerPort, "/favicon.ico");
                            }

                            @Override
                            public void onStopped(boolean error) {
                                log.info(prefix + "Writer stopped");
                                server.stop();
                            }

                            @Override
                            public void onContent(DefaultMetadata metadata, InputStream data, Long size) {
                                ++tests;
                                String url = metadata.getInfo("url") + "";
                                String status = metadata.getInfo("status") + "";
                                String digest = metadata.getInfo("digest") + "";
                                log.info("LAP Writer: got content for '" + url +
                                        "', status:" + status + ", size:" + size +
                                        " (Tests:" + tests + "/" + testsCount + ")"
                                );
                                if (tests == testsCount) {
                                    writer.stop();
                                }
                            }
                        });
                    }

                    @Override
                    public void onStopped(LifeCycle lifeCycle) {
                        log.info(prefix + "WebServer stopped (" + lifeCycle.getClass() + ")");
                        lap.stop();
                    }
                });
            }

            @Override
            public void onLapStopped() {
                log.info(prefix + "LAP stopped");
            }
        });
    }

    private void httpGet(String host, int webServerPort, String path) {
        GetMethod request = new GetMethod("http://" + host + ":" + webServerPort + path);
        try {
            int code = client.executeMethod(request);
            ++tests;
            log.info("http client: got response for (http " + code +
                    "') for '" + path +
                    "', size:" + request.getResponseBody().length +
                    " (Tests:" + tests + "/" + testsCount + ")"
            );
            if (tests == testsCount) {
                writer.stop();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
