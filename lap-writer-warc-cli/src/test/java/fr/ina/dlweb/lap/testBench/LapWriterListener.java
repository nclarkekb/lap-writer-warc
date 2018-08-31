package fr.ina.dlweb.lap.testBench;

import java.io.InputStream;

import fr.ina.dlweb.lap.writer.metadata.DefaultMetadata;
import fr.ina.dlweb.lap.writer.writerInfo.WriterInfoResponse;

/**
 * Date: 23/11/12
 * Time: 11:34
 *
 * @author drapin
 */
public interface LapWriterListener {
    void onStopped(boolean error);

    void onStarted(WriterInfoResponse response);

    void onContent(DefaultMetadata metadata, InputStream data, Long size);
}
