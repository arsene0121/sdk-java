import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class TestOTP implements PILoggerBridge {

    private ClientAndServer mockServer;
    private PrivacyIDEA privacyIDEA;

    private final String username = "testuser";
    private final String otp = "123456";

    private Throwable lastError;

    @Before
    public void setup() {
        mockServer = ClientAndServer.startClientAndServer(1080);

        privacyIDEA = new PrivacyIDEA.Builder("https://127.0.0.1:1080")
                .setSSLVerify(false)
                .setLogger(this)
                .build();
    }

    @Test
    public void testOTPSuccess() {
        String responseBody = "{\n" +
                "  \"detail\": {\n" +
                "    \"message\": \"matching 1 tokens\",\n" +
                "    \"otplen\": 6,\n" +
                "    \"serial\": \"PISP0001C673\",\n" +
                "    \"threadid\": 140536383567616,\n" +
                "    \"type\": \"totp\"\n" +
                "  },\n" +
                "  \"id\": 1,\n" +
                "  \"jsonrpc\": \"2.0\",\n" +
                "  \"result\": {\n" +
                "    \"status\": true,\n" +
                "    \"value\": true\n" +
                "  },\n" +
                "  \"time\": 1589276995.4397042,\n" +
                "  \"version\": \"privacyIDEA 3.2.1\",\n" +
                "  \"versionnumber\": \"3.2.1\",\n" +
                "  \"signature\": \"rsa_sha256_pss:AAAAAAAAAAA\"\n" +
                "}";
        setResponseBody(responseBody);

        PIResponse response = privacyIDEA.validateCheck(username, otp);

        // Assert everything
        assertEquals("matching 1 tokens", response.getMessage());
        assertEquals(6, response.getOTPlength());
        assertEquals("PISP0001C673", response.getSerial());
        //assertEquals("140536383567616", response.getThreadID());
        //assertEquals("1589276995.4397042", response.getTime());
        assertEquals("totp", response.getType());
        assertEquals("1", response.getID());
        assertEquals("2.0", response.getJSONRPCVersion());
        assertEquals("privacyIDEA 3.2.1", response.getPrivacyIDEAVersion());
        assertEquals("3.2.1", response.getPrivacyIDEAVersionNumber());
        assertEquals("rsa_sha256_pss:AAAAAAAAAAA", response.getSignature());
        // Trim all whitespaces, newlines
        assertEquals(responseBody.replaceAll("[\n\r]", ""), response.getRawMessage().replaceAll("[\n\r]", ""));
        assertEquals(responseBody.replaceAll("[\n\r]", ""), response.toString().replaceAll("[\n\r]", ""));
        // result
        assertTrue(response.getStatus());
        assertTrue(response.getValue());
    }

    @Test
    public void testEmptyResponse() {
        setResponseBody("");

        PIResponse response = privacyIDEA.validateCheck(username, otp);

        // An empty response returns null
        assertNull(response);
    }

    @Test
    public void testNoResponse() {
        // No server setup - server might be offline/unreachable etc
        PIResponse response = privacyIDEA.validateCheck(username, otp);

        // No response also returns null - the exception is forwarded to the ILoggerBridge if set
        assertNull(response);
        assertTrue(lastError instanceof FileNotFoundException);
    }

    private void setResponseBody(String s) {
        mockServer.when(
                HttpRequest.request()
                        .withMethod("POST")
                        .withPath("/validate/check")
                        .withBody("user=" + username + "&pass=" + otp))
                .respond(HttpResponse.response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(s)
                        .withDelay(TimeUnit.MILLISECONDS, 50));
    }

    @After
    public void tearDown() {
        mockServer.stop();
    }

    @Override
    public void error(String message) {
        System.err.println(message);
    }

    @Override
    public void log(String message) {
        System.out.println(message);
    }

    @Override
    public void error(Throwable t) {
        lastError = t;
        t.printStackTrace();
    }

    @Override
    public void log(Throwable t) {
        lastError = t;
        t.printStackTrace();
    }
}
