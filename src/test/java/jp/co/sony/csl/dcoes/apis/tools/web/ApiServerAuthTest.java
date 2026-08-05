package jp.co.sony.csl.dcoes.apis.tools.web;

import io.vertx.core.http.HttpServerRequest;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ApiServerAuthTest {

    @Test
    public void nullApiKey_returns500() {
        HttpServerRequest req = mock(HttpServerRequest.class);
        assertEquals(Integer.valueOf(500), ApiServer.checkAuth(req, null));
    }

    @Test
    public void emptyApiKey_returns500() {
        HttpServerRequest req = mock(HttpServerRequest.class);
        assertEquals(Integer.valueOf(500), ApiServer.checkAuth(req, ""));
    }

    @Test
    public void missingHeader_returns401() {
        HttpServerRequest req = mock(HttpServerRequest.class);
        when(req.getHeader("X-API-Key")).thenReturn(null);
        assertEquals(Integer.valueOf(401), ApiServer.checkAuth(req, "secret"));
    }

    @Test
    public void wrongKey_returns401() {
        HttpServerRequest req = mock(HttpServerRequest.class);
        when(req.getHeader("X-API-Key")).thenReturn("wrong");
        assertEquals(Integer.valueOf(401), ApiServer.checkAuth(req, "secret"));
    }

    @Test
    public void correctKey_returnsNull() {
        HttpServerRequest req = mock(HttpServerRequest.class);
        when(req.getHeader("X-API-Key")).thenReturn("secret");
        assertNull(ApiServer.checkAuth(req, "secret"));
    }
}
