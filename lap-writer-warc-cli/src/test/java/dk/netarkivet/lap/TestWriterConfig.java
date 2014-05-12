package dk.netarkivet.lap;

import java.io.ByteArrayInputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestWriterConfig {

	public static String jsonStr1 = ""
			+ "{"
			+ "\t\"timeout\": 20,"
			+ "\t\"verbose\": true,"
			+ "\t\"sessions\": ["
			+ "\t\t{"
			+ "\t\t\t\"dir\": \".\","
			+ "\t\t\t\"prefix\": \"LAP-KB-SB-TEST\","
			+ "\t\t\t\"compression\": true,"
			+ "\t\t\t\"max-file-size\": 12345678,"
			+ "\t\t\t\"deduplication\": true,"
			+ "\t\t\t\"ispartof\": \"LAP Test\","
			+ "\t\t\t\"description\": \"Files archive as part of testing INA's LAP.\","
			+ "\t\t\t\"operator\": \"KB/SB\","
			+ "\t\t\t\"httpheader\": \"LAP Test samling\""
			+ "\t\t}"
			+ "\t]"
			+ "}";

	public static String jsonStr2 = ""
			+ "{"
			+ "\t\"sessions\": ["
			+ "\t\t{"
			+ "\t\t\t\"dir\": \".\""
			+ "\t\t}"
			+ "\t]"
			+ "}";

	@Test
	public void test_writerconfig() {
		byte[] jsonBytes;
		PushbackInputStream pbin;
		WriterConfig wc;
		SessionConfig session;
		try {
			/*
			 * 1.
			 */
			jsonBytes = jsonStr1.getBytes("UTF-8");
    		pbin = new PushbackInputStream(new ByteArrayInputStream(jsonBytes), 32);

    		wc = WriterConfig.getWriterConfig(pbin);
    		Assert.assertNotNull(wc);
    		Assert.assertEquals(new Integer(20), wc.timeout);
    		Assert.assertEquals(new Boolean(true), wc.verbose);

    		Assert.assertNotNull(wc.sessions);
    		Assert.assertEquals(1, wc.sessions.length);

    		session = wc.sessions[0];

    		Assert.assertEquals(".", session.dir);
    		Assert.assertEquals("LAP-KB-SB-TEST", session.filePrefix);
    		Assert.assertEquals(new Boolean(true), session.bCompression);
    		Assert.assertEquals(new Long(12345678L), session.maxFileSize);
    		Assert.assertEquals(new Boolean(true), session.bDeduplication);
    		Assert.assertEquals("LAP Test", session.isPartOf);
    		Assert.assertEquals("Files archive as part of testing INA's LAP.", session.description);
    		Assert.assertEquals("KB/SB", session.operator);
    		Assert.assertEquals("LAP Test samling", session.httpheader);
			/*
			 * 2.
			 */
			jsonBytes = jsonStr2.getBytes("UTF-8");
    		pbin = new PushbackInputStream(new ByteArrayInputStream(jsonBytes), 32);

    		wc = WriterConfig.getWriterConfig(pbin);
    		Assert.assertNotNull(wc);
    		Assert.assertEquals(new Integer(10), wc.timeout);
    		Assert.assertEquals(new Boolean(false), wc.verbose);

    		Assert.assertNotNull(wc.sessions);
    		Assert.assertEquals(1, wc.sessions.length);

    		session = wc.sessions[0];

    		Assert.assertEquals(".", session.dir);
    		Assert.assertEquals("LAP", session.filePrefix);
    		Assert.assertEquals(new Boolean(false), session.bCompression);
    		Assert.assertEquals(new Long(1073741824L), session.maxFileSize);
    		Assert.assertEquals(new Boolean(true), session.bDeduplication);
    		Assert.assertEquals("", session.isPartOf);
    		Assert.assertEquals("", session.description);
    		Assert.assertEquals("", session.operator);
    		Assert.assertEquals("", session.httpheader);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		}
	}

}
