package fr.ina.dlweb.lap.testBench;

import java.io.InputStream;

import fr.ina.dlweb.lap.writer.DefaultLapWriter;
import fr.ina.dlweb.lap.writer.PersistenceListener;
import fr.ina.dlweb.lap.writer.metadata.DefaultMetadata;
import fr.ina.dlweb.lap.writer.writerInfo.DefaultWriterInfo;
import fr.ina.dlweb.lap.writer.writerInfo.WriterInfo;
import fr.ina.dlweb.lap.writer.writerInfo.WriterInfoResponse;

/**
 * Date: 23/11/12
 * Time: 11:34
 *
 * @author drapin
 */
public class LapWriter {
    private DefaultLapWriter lapWriter;

    public void stop() {
        lapWriter.stop();
    }

    public void start(String host, int lapWriterPort, final String digest, final LapWriterListener writerListener) {
        lapWriter = new DefaultLapWriter(host, lapWriterPort) {

            @Override
            protected void onStopped(boolean error) {
                super.onStopped(error);
                writerListener.onStopped(error);
            }

            @Override
            protected void onStarted(WriterInfoResponse response) {
                super.onStarted(response);
                writerListener.onStarted(response);
            }

            @Override
            public WriterInfo getInfo() {
                return new DefaultWriterInfo(digest, "Lap-Test-Writer");
            }

            @Override
            public void onContent(DefaultMetadata metadata, InputStream data, String contentId, Long size, PersistenceListener listener) throws Exception {
                writerListener.onContent(metadata, data, size);
                listener.onDataPersisted(contentId);
            }
        };

        Runnable writerRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    lapWriter.start(5);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        new Thread(writerRunnable, "lap-writer").start();
    }
}
