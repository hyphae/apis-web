package jp.co.sony.csl.dcoes.apis.tools.web.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import jp.co.sony.csl.dcoes.apis.tools.web.ApiServer;
import jp.co.sony.csl.dcoes.apis.tools.web.BudoEmulator;
import jp.co.sony.csl.dcoes.apis.tools.web.EmulatorEmulator;
import jp.co.sony.csl.dcoes.apis.tools.web.deprecated.DealGenerator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class StarterTest {

    @Mock
    private Vertx vertx;

    @Mock
    private EventBus eventBus;

    @Captor
    private ArgumentCaptor<Handler<AsyncResult<Void>>> completionHandlerCaptor;

    @InjectMocks
    private Starter starter;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testStart_ShouldDeployVerticlesInSequence() {

        Future<Void> futureEmulatorEmulator = Future.succeededFuture();
        Future<Void> futureBudoEmulator = Future.succeededFuture();
        Future<Void> futureApiServer = Future.succeededFuture();
        Future<Void> futureDealGenerator = Future.succeededFuture();

        doAnswer(invocation -> {
            Handler<AsyncResult<String>> handler = invocation.getArgument(1);
            handler.handle(Future.succeededFuture("verticle_id"));
            return null;
        }).when(vertx).deployVerticle(Mockito.any(Verticle.class), any(Handler.class));

        starter.doStart(result -> {
            completionHandlerCaptor.capture();
            completionHandlerCaptor.getValue().handle(Future.succeededFuture());
        });

        verify(vertx).deployVerticle(Mockito.any(EmulatorEmulator.class), any(Handler.class));
        verify(vertx).deployVerticle(Mockito.any(BudoEmulator.class), any(Handler.class));
        verify(vertx).deployVerticle(Mockito.any(ApiServer.class), any(Handler.class));
        verify(vertx).deployVerticle(Mockito.any(DealGenerator.class), any(Handler.class));

        verify(completionHandlerCaptor.getValue()).handle(Future.succeededFuture());
    }

    @Test
    public void testStart_ShouldFailOnVerticleDeployment() {
        doAnswer(invocation -> {
            Handler<AsyncResult<String>> handler = invocation.getArgument(1);
            handler.handle(Future.failedFuture("Deployment failed"));
            return null;
        }).when(vertx).deployVerticle(Mockito.any(Verticle.class), any(Handler.class));

        starter.doStart(result -> {
            completionHandlerCaptor.capture();
            completionHandlerCaptor.getValue().handle(Future.failedFuture("Deployment failed"));
        });

        verify(completionHandlerCaptor.getValue()).handle(Future.failedFuture("Deployment failed"));
    }

    @Test
    public void testStart_ShouldCallCompletionHandlerOnSuccess() {
        doAnswer(invocation -> {
            Handler<AsyncResult<String>> handler = invocation.getArgument(1);
            handler.handle(Future.succeededFuture("verticle_id"));
            return null;
        }).when(vertx).deployVerticle(Mockito.any(Verticle.class), any(Handler.class));

        starter.doStart(result -> {
            completionHandlerCaptor.capture();
            completionHandlerCaptor.getValue().handle(Future.succeededFuture());
        });

        verify(completionHandlerCaptor.getValue()).handle(Future.succeededFuture());
    }
}
