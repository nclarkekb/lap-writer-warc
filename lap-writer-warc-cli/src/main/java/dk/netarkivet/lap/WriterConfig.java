package dk.netarkivet.lap;

import java.io.IOException;
import java.io.PushbackInputStream;

import com.antiaction.common.json.JSONDecoder;
import com.antiaction.common.json.JSONEncoding;
import com.antiaction.common.json.JSONException;
import com.antiaction.common.json.JSONObjectMappings;
import com.antiaction.common.json.JSONStructure;
import com.antiaction.common.json.JSONText;
import com.antiaction.common.json.annotation.JSONNullable;

public class WriterConfig {

	@JSONNullable
	public Integer timeout = 10;

	@JSONNullable
	public Boolean verbose = false;

	public SessionConfig[] sessions;

    /** JSON encoding encoder/decoder dispatcher. */
    protected static JSONEncoding json_encoding = JSONEncoding.getJSONEncoding();

    /** JSON object mapping worker. */
    protected static JSONObjectMappings json_om = new JSONObjectMappings();

    /** JSON decoder/encoder. */
    protected static JSONText json_text;

    static {
        json_text = new JSONText();
    	try {
			json_om.register(WriterConfig.class);
		} catch (JSONException e) {
			e.printStackTrace();
			System.exit(1);
		}
    }

    public WriterConfig() {
    }

    public static WriterConfig getWriterConfig(PushbackInputStream pbin) {
    	WriterConfig wc = null;
    	try {
    		//JSONStreamUnmarshaller unmarshaller = json_om.getStreamUnmarshaller();
    		int encoding = JSONEncoding.encoding(pbin);
    		JSONDecoder json_decoder = json_encoding.getJSONDecoder(encoding);
    		//wc = unmarshaller.toObject(pbin, json_decoder, WriterConfig.class);
            JSONStructure json_object = json_text.decodeJSONtext(pbin, json_decoder);
            wc = json_om.getStructureUnmarshaller().toObject(json_object, WriterConfig.class);
    	} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
    	return wc;
    }

}
