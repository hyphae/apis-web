package jp.co.sony.csl.dcoes.apis.tools.web.api_handler;

import jp.co.sony.csl.dcoes.apis.tools.web.api_handler.ErrorGeneration;
import io.vertx.core.logging.Logger;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
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

public class ErrorGenerationTest{
    @Mock
    private Vertx vertx;

    @Mock
    private HttpServerRequest req;

    @Mock
    private HttpServerResponse res; // fake response

    @Mock
    private Logger log; 

    private ErrorGeneration errorGeneration;

    @Mock
    private EventBus eventBus;

    @BeforeEach
    public void setUp() {
    MockitoAnnotations.openMocks(this);
    errorGeneration = new ErrorGeneration();
    }

    //should accept error path 
    @Test
    public void shouldAcceptErrorPath(){
    // Arrange
    when(req.path()).thenReturn("/error");
    // Act
    boolean result = errorGeneration.canHandleRequest(req);
    // Assert
    assertTrue(result);
}

    // Should reject non-error Paths
    @ParameterizedTest
    @ValueSource(strings = {"/status","/users","/health"})
    public void shouldRejectNonLogPath(String path){
        // Arrange
        when(req.path()).thenReturn(path);
        // Act
        boolean result = errorGeneration.canHandleRequest(req);
        // Assert
        assertFalse(result);
    }

    // returning 405 for unsupported methods
    @ParameterizedTest
        @EnumSource(value = HttpMethod.class, names ={"GET","POST"},mode = EnumSource.Mode.Exclude)
        public void unsupportedMethodReturn405(HttpMethod unsupportedMethod){
            // Arrange
            when(req.method()).thenReturn(unsupportedMethod);
            when(req.response()).thenReturn(res);
            when(res.setStatusCode(anyInt())).thenReturn(res);
            // Act
            errorGeneration.handleRequest(vertx,req,log);
            // Assert
            verify(res).setStatusCode(405);
            verify(res).end();
        }

    // GET method should return HTML
    @Test
    public void getShouldRetutnHTML(){
        // Arrange
        when(req.method()).thenReturn(HttpMethod.GET);
        when(req.response()).thenReturn(res);
        when(res.setChunked(true)).thenReturn(res);
        when(res.putHeader("content-type","text/html")).thenReturn(res);
        when(res.write(anyString())).thenReturn(res);
        //Act
        errorGeneration.handleRequest(vertx, req, log);
        // Assert
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(res).write(htmlCaptor.capture());
        String html = htmlCaptor.getValue();
        assertTrue(html.contains("<html>"));
    }

    // POST method, with valid error-data.
    @Test 
    public void postWithValidErrorData_ShouldReportError() {
        // Arrange
        when(req.method()).thenReturn(HttpMethod.POST);
        when(req.response()).thenReturn(res);
        when(res.setChunked(true)).thenReturn(res);
        when(res.putHeader(anyString(), anyString())).thenReturn(res);
        when(res.end(anyString())).thenReturn(res);
        when(req.getFormAttribute("unitId")).thenReturn("unit-1");
        when(req.getFormAttribute("category")).thenReturn("USER");
        when(req.getFormAttribute("extent")).thenReturn("LOCAL");
        when(req.getFormAttribute("level")).thenReturn("ERROR");
        when(req.getFormAttribute("message")).thenReturn("Test error");
        ArgumentCaptor<Handler> endHandlerCaptor = ArgumentCaptor.forClass(Handler.class);
        // Act
        errorGeneration.handleRequest(vertx, req, log);
        verify(req).endHandler(endHandlerCaptor.capture());
        Handler capturedHandler = endHandlerCaptor.getValue();
        capturedHandler.handle(null);
        // Assert
        verify(res).setChunked(true);
        verify(res).putHeader("content-type", "text/plain");
        verify(res).end(contains("publishing error"));
    }

    // exception should throw 500
    @Test public void
    postException_ShouldReturn500(){
        // Arrange
        when(req.method()).thenReturn(HttpMethod.POST);
        when(req.response()).thenReturn(res);
        when(req.setExpectMultipart(true)).thenReturn(req);
        when(res.setChunked(true)).thenReturn(res);
        when(res.putHeader(anyString(), anyString())).thenReturn(res);	
        when(res.setStatusCode(anyInt())).thenReturn(res);
        when(res.end(anyString())).thenReturn(res);
        when(req.getFormAttribute(anyString())).thenThrow(new RuntimeException());
        ArgumentCaptor<Handler> endHandlerCaptor = ArgumentCaptor.forClass(Handler.class);
        // Act
        errorGeneration.handleRequest(vertx, req, log);
        verify(req).endHandler(endHandlerCaptor.capture());
        Handler capturedHandler = endHandlerCaptor.getValue();
        capturedHandler.handle(null);
        // Assert
        verify(res).setStatusCode(500);
        verify(res).end(contains("exception"));
        }
    }