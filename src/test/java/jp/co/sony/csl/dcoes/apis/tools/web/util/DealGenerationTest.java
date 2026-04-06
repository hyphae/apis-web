package jp.co.sony.csl.dcoes.apis.tools.web.api_handler;

import jp.co.sony.csl.dcoes.apis.tools.web.api_handler.DealGeneration;
import jp.co.sony.csl.dcoes.apis.common.ServiceAddress;
import io.vertx.core.logging.Logger;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

public class DealGenerationTest {

    @Mock
    private Vertx vertx;

    @Mock
    private HttpServerRequest req;  // fake request

    @Mock
    private HttpServerResponse res; // fake response

    @Mock
    private Logger log; 

    private DealGeneration dealGeneration; 

    @Mock
    private EventBus eventBus;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        dealGeneration = new DealGeneration();
    }

    //should accept deal path 
    @Test
    public void shouldAcceptDealPath(){
        // Arrange
        when(req.path()).thenReturn("/deal");
        // Act
        boolean result = dealGeneration.canHandleRequest(req);
        // Assert
        assertTrue(result);
    }

    // returning 405 for unsupported methods
    @ParameterizedTest
        @EnumSource(value = HttpMethod.class, names = {"GET", "POST"}, mode = EnumSource.Mode.EXCLUDE)
        public void unsupportedMethodReturn405(HttpMethod unsupportedMethod) {
            // Arrange
            when(req.method()).thenReturn(unsupportedMethod);
            when(req.response()).thenReturn(res);
            when(res.setStatusCode(anyInt())).thenReturn(res);
            // Act
            dealGeneration.handleRequest(vertx, req, log);
            // Assert
            verify(res).setStatusCode(405);
            verify(res).end();
        }

    // Should reject Non-deal Paths
    @ParameterizedTest
    @ValueSource(strings = {"/status", "/users", "/health"})
    public void shouldRejectNonDealPath(String path) {
        // Arrange
        when(req.path()).thenReturn(path);
        // Act
        boolean result = dealGeneration.canHandleRequest(req);
        // Assert
        assertFalse(result);
    }

    // get method should return HTML
    @Test
    public void getShouldRetutnHTML(){
        // Arrange
        when(req.method()).thenReturn(HttpMethod.GET);
        when(req.response()).thenReturn(res);
        when(res.setChunked(true)).thenReturn(res);
        when(res.putHeader("content-type", "text/html")).thenReturn(res);
        when(res.write(anyString())).thenReturn(res);
        // Act
        dealGeneration.handleRequest(vertx, req, log);
        // Assert
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(res).write(htmlCaptor.capture());
        String html = htmlCaptor.getValue();
        assertTrue(html.contains("<html>"));
    }

    // Should return 500 for invalid Json
    @Test
    public void postShouldReturn500ForInvalidJson() {
        // Arrange
        when(req.method()).thenReturn(HttpMethod.POST);
        when(req.response()).thenReturn(res);
        when(res.setChunked(true)).thenReturn(res);
        when(res.putHeader(anyString(), anyString())).thenReturn(res);
        when(res.setStatusCode(anyInt())).thenReturn(res);
        when(req.getFormAttribute("json")).thenReturn("NOT_VALID_JSON");
        // Act
        dealGeneration.handleRequest(vertx, req, log);
        // Assert
        ArgumentCaptor<Handler<Void>> endHandlerCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(req).endHandler(endHandlerCaptor.capture());
        endHandlerCaptor.getValue().handle(null);
        verify(res).setStatusCode(500);
    }

    // Should send to eventBus on valid json
    @Test
        public void postShouldSendToEventBusForValidJson() {
            when(req.method()).thenReturn(HttpMethod.POST);
            when(req.response()).thenReturn(res);
            when(res.setChunked(true)).thenReturn(res);
            when(res.putHeader(anyString(), anyString())).thenReturn(res);
            when(req.getFormAttribute("json")).thenReturn("{\"from\":\"nodeA\", \"to\":\"nodeB\"}");
            when(vertx.eventBus()).thenReturn(eventBus);
            // Act
            dealGeneration.handleRequest(vertx, req, log);
            // Assert
            ArgumentCaptor<Handler<Void>> endHandlerCaptor = ArgumentCaptor.forClass(Handler.class);
            verify(req).endHandler(endHandlerCaptor.capture());
            endHandlerCaptor.getValue().handle(null);
            verify(eventBus).send(eq(ServiceAddress.Mediator.dealCreation()), any(JsonObject.class));
        }

    // exceptions should throw 500
    @Test public void
    postException_ShouldReturn500(){
        // Arrange
        when(req.method()).thenReturn(HttpMethod.POST);
        when(req.response()).thenReturn(res);
        when(res.setChunked(true)).thenReturn(res);
        when(res.putHeader(anyString(), anyString())).thenReturn(res);	
        when(req.setExpectMultipart(true)).thenReturn(req);
        when(req.getFormAttribute(anyString())).thenThrow(new RuntimeException());
        // Act
        dealGeneration.handleRequest(vertx, req, log);
        verify(req).endHandler(endHandlerCaptor.capture());
        Handler capturedHandler = endHandlerCaptor.getValue();
        capturedHandler.handle(null);
        // Assert
        ArgumentCaptor<Handler> endHandlerCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(res).setStatusCode(500);
        verify(res).setChunked(true);
        verify(res).putHeader(anyString(),anyString());
        verify(res).end(contains("exception"));
        }
    }
