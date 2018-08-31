package fr.ina.dlweb.lap.testBench;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

/**
 * Date: 23/11/12
 * Time: 11:29
 *
 * @author drapin
 */
public class Lap {
    private static final Logger log = Logger.getLogger(Lap.class);

    private Process lapProcess;
    private int lapPort;
    private String host;

    public void stop() {
        HttpClient client = new HttpClient();
        client.getHostConfiguration().setProxy(host, lapPort);
        GetMethod request = new GetMethod("http://ppc.vortex.vortex/exit");
        try {
            int code = client.executeMethod(request);
            log.debug("lap http exit : " + code);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        log.debug("calling lapProcess.destroy()");
//        lapProcess.destroy();
    }

    public void start(String host, String lapBinary, int lapPort, int writerPort, String digest, final LapListener listener) {
        File lap = new File(lapBinary);
        if (!lap.exists()) throw new RuntimeException("LAP binary '" + lapBinary + "' not found");
        final String[] startArgs = new String[]{
                lap.getAbsolutePath(),
                "--web-port=" + lapPort,
                "--writer-port=" + writerPort,
                "--digest=" + (digest == null ? "none" : digest)
        };

        try {
            this.host = host;
            this.lapPort = lapPort;
            lapProcess = Runtime.getRuntime().exec(startArgs);

            Runnable lapReader = new Runnable() {

                @Override
                public void run() {
                    InputStream os = lapProcess.getInputStream();
                    while (true) {
                        try {
                            String s = readLine((BufferedInputStream) os);
                            if (s == null || s.isEmpty()) continue;

                            s = s.replaceAll("[\r\n]+", "");
                            log.debug("LAP>" + s);

                            if (s.equals("Loading Harbor 'Lap'... Ok")) {
                                listener.onLapStarted();
                            }

                            if (s.equals("EXIT")) {
                                listener.onLapStopped();
                                return;
                            }

                        } catch (IOException e) {
                            log.warn(null, e);
                            listener.onLapStopped();
                            return;
                        }
                    }
                }
            };
            new Thread(lapReader, "lap-output").start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static String readLine(BufferedInputStream input) throws IOException {
        // read data from the stream util we reach a '\n'
        byte[] buffer = new byte[1000];
        int read = 0, found;
        while (true) {
            found = input.read(buffer, read, 1);
            if (found == -1) break;
            read += found;

            if (buffer[read - 1] == '\n') break;

            if (read == buffer.length) {
                byte[] prev = buffer;
                buffer = new byte[buffer.length * 2];
                System.arraycopy(prev, 0, buffer, 0, read);
            }
        }
        return new String(buffer, 0, read);
    }


}
