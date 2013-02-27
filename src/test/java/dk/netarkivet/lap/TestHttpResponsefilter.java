package dk.netarkivet.lap;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.HttpHeader;

@RunWith(JUnit4.class)
public class TestHttpResponsefilter {

	@Test
	public void test_httpresponse_filter() {
		Object[][] cases = new Object[][] {
				{
					// No change.
					(
							"poop"
					).getBytes(),
					42, false,
					(
							"poop"
					).getBytes()
				},
				// No change.
				{
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:42:04 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Content-Length: 229575\r\n"
							+ "Set-Cookie: ASPSESSIONIDCRDTQSBA=GCHCPEBABIPDOKDGACFPBKDL; path=/\r\n"
							+ "Cache-control: private\r\n"
							+ "\r\n"
					).getBytes(),
					229575, true,
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:42:04 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Content-Length: 229575\r\n"
							+ "Set-Cookie: ASPSESSIONIDCRDTQSBA=GCHCPEBABIPDOKDGACFPBKDL; path=/\r\n"
							+ "Cache-control: private\r\n"
							+ "\r\n"
					).getBytes()
				},
				// Change content-length.
				{
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:42:04 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Content-Length: 229575\r\n"
							+ "Set-Cookie: ASPSESSIONIDCRDTQSBA=GCHCPEBABIPDOKDGACFPBKDL; path=/\r\n"
							+ "Cache-control: private\r\n"
							+ "\r\n"
					).getBytes(),
					123456, true,
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:42:04 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Content-Length: 123456\r\n"
							+ "Set-Cookie: ASPSESSIONIDCRDTQSBA=GCHCPEBABIPDOKDGACFPBKDL; path=/\r\n"
							+ "Cache-control: private\r\n"
							+ "\r\n"
					).getBytes()
				},
				// Add content-length.
				{
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:42:04 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Set-Cookie: ASPSESSIONIDCRDTQSBA=GCHCPEBABIPDOKDGACFPBKDL; path=/\r\n"
							+ "Cache-control: private\r\n"
							+ "\r\n"
					).getBytes(),
					123456, true,
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:42:04 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Set-Cookie: ASPSESSIONIDCRDTQSBA=GCHCPEBABIPDOKDGACFPBKDL; path=/\r\n"
							+ "Cache-control: private\r\n"
							+ "Content-Length: 123456\r\n"
							+ "\r\n"
					).getBytes()
				},
				// No change.
				{
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:53:00 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Accept-Ranges: bytes\r\n"
							+ "Last-Modified: Thu, 01 Jul 1999 22:49:36 GMT\r\n"
							+ "ETag: \"0c021fe13c4be1:267001\"\r\n"
							+ "Content-Length: 229575\r\n"
							+ "\r\n"
					).getBytes(),
					229575, true,
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:53:00 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Accept-Ranges: bytes\r\n"
							+ "Last-Modified: Thu, 01 Jul 1999 22:49:36 GMT\r\n"
							+ "ETag: \"0c021fe13c4be1:267001\"\r\n"
							+ "Content-Length: 229575\r\n"
							+ "\r\n"
					).getBytes()
				},
				// Change content-length.
				{
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:53:00 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Accept-Ranges: bytes\r\n"
							+ "Last-Modified: Thu, 01 Jul 1999 22:49:36 GMT\r\n"
							+ "ETag: \"0c021fe13c4be1:267001\"\r\n"
							+ "Content-Length: 229575\r\n"
							+ "\r\n"
					).getBytes(),
					123456, true,
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:53:00 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Accept-Ranges: bytes\r\n"
							+ "Last-Modified: Thu, 01 Jul 1999 22:49:36 GMT\r\n"
							+ "ETag: \"0c021fe13c4be1:267001\"\r\n"
							+ "Content-Length: 123456\r\n"
							+ "\r\n"
					).getBytes()
				},
				// Add content-length.
				{
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:53:00 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Accept-Ranges: bytes\r\n"
							+ "Last-Modified: Thu, 01 Jul 1999 22:49:36 GMT\r\n"
							+ "ETag: \"0c021fe13c4be1:267001\"\r\n"
							+ "\r\n"
					).getBytes(),
					123456, true,
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:53:00 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Accept-Ranges: bytes\r\n"
							+ "Last-Modified: Thu, 01 Jul 1999 22:49:36 GMT\r\n"
							+ "ETag: \"0c021fe13c4be1:267001\"\r\n"
							+ "Content-Length: 123456\r\n"
							+ "\r\n"
					).getBytes()
				},










				// No change.
				{
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:42:04 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Content-Length: 229575\r\n"
							+ "Set-Cookie: ASPSESSIONIDCRDTQSBA=GCHCPEBABIPDOKDGACFPBKDL; path=/\r\n"
							+ "Cache-control: private\r\n"
							+ "Transfer-Encoding: chunked\r\n"
							+ "Content-Encoding: gzip\r\n"
							+ "\r\n"
					).getBytes(),
					229575, true,
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:42:04 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Content-Length: 229575\r\n"
							+ "Set-Cookie: ASPSESSIONIDCRDTQSBA=GCHCPEBABIPDOKDGACFPBKDL; path=/\r\n"
							+ "Cache-control: private\r\n"
							+ "\r\n"
					).getBytes()
				},
				// Change content-length.
				{
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:42:04 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Content-Length: 229575\r\n"
							+ "Set-Cookie: ASPSESSIONIDCRDTQSBA=GCHCPEBABIPDOKDGACFPBKDL; path=/\r\n"
							+ "Cache-control: private\r\n"
							+ "Transfer-Encoding: chunked\r\n"
							+ "Content-Encoding: gzip\r\n"
							+ "\r\n"
					).getBytes(),
					123456, true,
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:42:04 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Content-Length: 123456\r\n"
							+ "Set-Cookie: ASPSESSIONIDCRDTQSBA=GCHCPEBABIPDOKDGACFPBKDL; path=/\r\n"
							+ "Cache-control: private\r\n"
							+ "\r\n"
					).getBytes()
				},
				// Add content-length.
				{
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:42:04 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Set-Cookie: ASPSESSIONIDCRDTQSBA=GCHCPEBABIPDOKDGACFPBKDL; path=/\r\n"
							+ "Cache-control: private\r\n"
							+ "Transfer-Encoding: chunked\r\n"
							+ "Content-Encoding: gzip\r\n"
							+ "\r\n"
					).getBytes(),
					123456, true,
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:42:04 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Set-Cookie: ASPSESSIONIDCRDTQSBA=GCHCPEBABIPDOKDGACFPBKDL; path=/\r\n"
							+ "Cache-control: private\r\n"
							+ "Content-Length: 123456\r\n"
							+ "\r\n"
					).getBytes()
				},
				// No change.
				{
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:53:00 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Accept-Ranges: bytes\r\n"
							+ "Last-Modified: Thu, 01 Jul 1999 22:49:36 GMT\r\n"
							+ "ETag: \"0c021fe13c4be1:267001\"\r\n"
							+ "Transfer-Encoding: chunked\r\n"
							+ "Content-Encoding: gzip\r\n"
							+ "Content-Length: 229575\r\n"
							+ "\r\n"
					).getBytes(),
					229575, true,
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:53:00 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Accept-Ranges: bytes\r\n"
							+ "Last-Modified: Thu, 01 Jul 1999 22:49:36 GMT\r\n"
							+ "ETag: \"0c021fe13c4be1:267001\"\r\n"
							+ "Content-Length: 229575\r\n"
							+ "\r\n"
					).getBytes()
				},
				// Change content-length.
				{
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:53:00 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Accept-Ranges: bytes\r\n"
							+ "Last-Modified: Thu, 01 Jul 1999 22:49:36 GMT\r\n"
							+ "ETag: \"0c021fe13c4be1:267001\"\r\n"
							+ "Transfer-Encoding: chunked\r\n"
							+ "Content-Encoding: gzip\r\n"
							+ "Content-Length: 229575\r\n"
							+ "\r\n"
					).getBytes(),
					123456, true,
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:53:00 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Accept-Ranges: bytes\r\n"
							+ "Last-Modified: Thu, 01 Jul 1999 22:49:36 GMT\r\n"
							+ "ETag: \"0c021fe13c4be1:267001\"\r\n"
							+ "Content-Length: 123456\r\n"
							+ "\r\n"
					).getBytes()
				},
				// Add content-length.
				{
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:53:00 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Transfer-Encoding: chunked\r\n"
							+ "Content-Encoding: gzip\r\n"
							+ "Accept-Ranges: bytes\r\n"
							+ "Last-Modified: Thu, 01 Jul 1999 22:49:36 GMT\r\n"
							+ "ETag: \"0c021fe13c4be1:267001\"\r\n"
							+ "\r\n"
					).getBytes(),
					123456, true,
					(
							"HTTP/1.1 200 OK\r\n"
							+ "Server: Microsoft-IIS/4.0\r\n"
							+ "Date: Fri, 19 Dec 2003 12:53:00 GMT\r\n"
							+ "Content-Type: text/html\r\n"
							+ "Accept-Ranges: bytes\r\n"
							+ "Last-Modified: Thu, 01 Jul 1999 22:49:36 GMT\r\n"
							+ "ETag: \"0c021fe13c4be1:267001\"\r\n"
							+ "Content-Length: 123456\r\n"
							+ "\r\n"
					).getBytes()
				}
		};

		HttpHeader httpHeader;
		try {
			for (int i=0; i<cases.length; ++i) {
				byte[] httpHeaderBytesSrc = (byte[])cases[i][0];
				long contentLength = (Integer)cases[i][1];
				boolean expectedValidity = (Boolean)cases[i][2];
				byte[] httpHeaderBytesExpected = (byte[])cases[i][3];

				byte[] httpHeaderBytes = LAPWarcWriter.filter(httpHeaderBytesSrc, contentLength);

				httpHeader = HttpHeader.processPayload(HttpHeader.HT_RESPONSE,
						new ByteCountingPushBackInputStream(new ByteArrayInputStream(httpHeaderBytes), 8192),
						httpHeaderBytes.length,
						null);
				httpHeader.close();

				Assert.assertNotNull(httpHeader);
				Assert.assertEquals(expectedValidity, httpHeader.isValid());

				Assert.assertArrayEquals(httpHeaderBytesExpected, httpHeader.getHeader());

				//System.out.println(new String(httpHeader.getHeader()));
			}
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		}
	}

}
/*
HTTP/1.1 200 OK
Server: Microsoft-IIS/4.0
Date: Fri, 19 Dec 2003 12:42:04 GMT
Content-Type: text/html
Set-Cookie: ASPSESSIONIDCRDTQSBA=GCHCPEBABIPDOKDGACFPBKDL; path=/
Cache-control: private
*/

/*
HTTP/1.1 200 OK
Server: Microsoft-IIS/4.0
Date: Fri, 19 Dec 2003 12:53:00 GMT
Content-Type: text/html
Accept-Ranges: bytes
Last-Modified: Thu, 01 Jul 1999 22:49:36 GMT
ETag: "0c021fe13c4be1:267001"
Content-Length: 229575
*/

/*
"PUT /<api version>/<account>/<container>/<object> HTTP/1.1\r\n"
+ "Host: storage.clouddrive.com\r\n"
+ "X-Auth-Token: eaaafd18-0fed-4b3a-81b4-663c99ec1cbb\r\n"
+ "Content-Type: video/mp4\r\n"
+ "Content-Encoding: gzip\r\n"
+ "\r\n"
*/