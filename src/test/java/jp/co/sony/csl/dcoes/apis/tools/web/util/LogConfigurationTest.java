package jp.co.sony.csl.dcoes.apis.tools.web.api_handler;

import jp.co.sony.csl.dcoes.apis.tools.web.api_handler.LogConfiguration;
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

public class LogConfigurationTest{
    @Mock
    private Vertx vertx;

    @Mock
    private HttpServerRequest req;

    @Mock
    private HttpServerResponse res; // fake response

    @Mock
    private Logger log; 

    private LogConfiguration logConfiguration;

    @Mock
    private EventBus eventBus;

    @BeforeEach
    public void setUp() {
    MockitoAnnotations.openMocks(this);
    logConfiguration = new LogConfiguration();
    }

    //should accept log path 
    @Test
    public void shouldAcceptLogPath(){
    // Arrange
    when(req.path()).thenReturn("/log");
    // Act
    boolean result = logConfiguration.canHandleRequest(req);
    // Assert
    assertTrue(result);
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
            logConfiguration.handleRequest(vertx,req,log);
            // Assert
            verify(res).setStatusCode(405);
            verify(res).end();
        }

    // Should reject Non-log Paths
    @ParameterizedTest
    @ValueSource(strings = {"/status","/users","/health"})
    public void shouldRejectNonLogPath(String path){
        // Arrange
        when(req.path()).thenReturn(path);
        // Act
        boolean result = logConfiguration.canHandleRequest(req);
        // Assert
        assertFalse(result);
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
        logConfiguration.handleRequest(vertx, req, log);
        // Assert
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(res).write(htmlCaptor.capture());
        String html = htmlCaptor.getValue();
        assertTrue(html.contains("<html>"));
    }

    // post with multicast handler should publish to eventbus
    @Test public void postWithMulticastHandler_ShouldPublishToEventBus() {
        // Arrange
        when(vertx.eventBus()).thenReturn(eventBus); 
        when(req.method()).thenReturn(HttpMethod.POST);
        when(req.response()).thenReturn(res);
        when(res.setChunked(true)).thenReturn(res);
        when(res.putHeader(anyString(), anyString())).thenReturn(res);
        when(res.end(anyString())).thenReturn(res);
        when(req.getFormAttribute("handler")).thenReturn("Multicast");
        when(req.getFormAttribute("level")).thenReturn("DEBUG");
        ArgumentCaptor<Handler> endHandlerCaptor = ArgumentCaptor.forClass(Handler.class);
        // Act 
        logConfiguration.handleRequest(vertx, req, log);
        verify(req).endHandler(endHandlerCaptor.capture());
        Handler capturedHandler = endHandlerCaptor.getValue();
        capturedHandler.handle(null);
        // Assert
        verify(eventBus).publish(ServiceAddress.multicastLogHandlerLevel(), "DEBUG");
        verify(res).end(contains("publishing new multicast log level"));
    }

    // post without multicast handler should return 500
    @Test public void postWithoutMulticastHandler_ShouldReturn500() {
        // Arrange
        when(vertx.eventBus()).thenReturn(eventBus); 
        when(req.method()).thenReturn(HttpMethod.POST);
        when(req.response()).thenReturn(res);
        when(res.setChunked(true)).thenReturn(res);
        when(res.putHeader(anyString(), anyString())).thenReturn(res);
        when(res.end(anyString())).thenReturn(res);
        when(req.getFormAttribute("handler")).thenReturn("Unknown");
        when(req.getFormAttribute("level")).thenReturn("DEBUG");
        ArgumentCaptor<Handler> endHandlerCaptor = ArgumentCaptor.forClass(Handler.class);
        // Act
        logConfiguration.handleRequest(vertx, req, log);
        verify(req).endHandler(endHandlerCaptor.capture());
        Handler capturedHandler = endHandlerCaptor.getValue();
        capturedHandler.handle(null);
        // Assert
        verify(res).setStatusCode(500);
        verify(res).end(contains("exception"));
    }

    // exception should throw 500
    @Test public void
    postException_ShouldReturn500(){
        // Arrange
        when(req.method()).thenReturn(HttpMethod.POST);
        when(req.response()).thenReturn(res);
        when(res.setChunked(true)).thenReturn(res);
        when(res.putHeader(anyString(), anyString())).thenReturn(res);	
        when(req.setExpectMultipart(true)).thenReturn(req);
        when(req.getFormAttribute(anyString())).thenThrow(new RuntimeException());
        ArgumentCaptor<Handler> endHandlerCaptor = ArgumentCaptor.forClass(Handler.class);
        // Act
        logConfiguration.handleRequest(vertx, req, log);
        verify(req).endHandler(endHandlerCaptor.capture());
        Handler capturedHandler = endHandlerCaptor.getValue();
        capturedHandler.handle(null);
        // Assert
        verify(res).setStatusCode(500);
        verify(res).end(contains("unknown log handler"));
        }
    }


