package jp.co.sony.csl.dcoes.apis.tools.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jp.co.sony.csl.dcoes.apis.common.ServiceAddress;
import jp.co.sony.csl.dcoes.apis.common.util.vertx.JsonObjectUtil;
import jp.co.sony.csl.dcoes.apis.common.util.vertx.JsonObjectWrapper;
import jp.co.sony.csl.dcoes.apis.common.util.vertx.VertxConfig;

/**
 * This Verticle provides unit data to the outside.
 * Started from {@link jp.co.sony.csl.dcoes.apis.tools.web.util.Starter}
 * Verticle.
 * Emulates providing unit data to main_controller in DCDC emulator environment.
 * Provides the following API.
 * - /get/log : Gets unit data from all units
 * Removes the attributes below if {@code removeDeprecatedData=true} is
 * specified in environment variables; then returns the data.
 * - Under emu
 * - Under dcdc.powermeter
 * 
 * @author OES Project
 *         外部に対しユニットデータを提供する Verticle.
 *         {@link jp.co.sony.csl.dcoes.apis.tools.web.util.Starter} Verticle
 *         から起動される.
 *         DCDC emulator 環境での main_controller へのユニットデータ提供を模倣.
 *         以下の API を提供する.
 *         - /get/log : 全ユニットのユニットデータを取得する.
 *         環境変数に {@code removeDeprecatedData=true} が指定されていたら以下の属性を削除して返す.
 *         - emu 以下
 *         - dcdc.powermeter 以下
 * @author OES Project
 */
public class EmulatorEmulator extends AbstractVerticle {
	private static final Logger log = LoggerFactory.getLogger(EmulatorEmulator.class);

	/**
	 * This is the default value of the port that opens the service.
	 * The value is {@value}
	 * サービスを開くポートのデフォルト値.
	 * 値は {@value}
	 */
	private static final int DEFAULT_PORT = 43900;

	/**
	 * Creates cache of unit data.
	 * Periodically queries GridMaster and caches.
	 * ユニットデータのキャッシュ.
	 * 定期的に GridMaster に問合せキャッシュしておく.
	 */
	public static final JsonObjectWrapper cache = new JsonObjectWrapper();
	private static final JsonObject empty_ = new JsonObject();

	/**
	 * Called during startup.
	 * Opens HTTP service.
	 * 
	 * @param startFuture {@inheritDoc}
	 * @throws Exception {@inheritDoc}
	 *                   起動時に呼び出される.
	 *                   HTTP サービスを開く.
	 * @param startFuture {@inheritDoc}
	 * @throws Exception {@inheritDoc}
	 */
	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		startHttpService_(resHttp -> {
			if (resHttp.succeeded()) {
				if (log.isTraceEnabled())
					log.trace("started : " + deploymentID());
				startPromise.complete();
			} else {
				startPromise.fail(resHttp.cause());
			}
		});
	}

	/**
	 * Called when stopped.
	 * 
	 * @throws Exception {@inheritDoc}
	 *                   停止時に呼び出される.
	 * @throws Exception {@inheritDoc}
	 */
	@Override
	public void stop() throws Exception {
		if (log.isTraceEnabled())
			log.trace("stopped : " + deploymentID());
	}

	////

	/**
	 * Starts HTTP service.
	 * Gets settings from CONFIG and initializes.
	 * - CONFIG.emulatorEmulator.port : Port [{@link Integer}]
	 * 
	 * @param completionHandler The completion handler
	 *                          HTTP サービスを起動する.
	 *                          CONFIG から設定を取得し初期化する.
	 *                          - CONFIG.emulatorEmulator.port : ポート
	 *                          [{@link Integer}]
	 * @param completionHandler the completion handler
	 */
	private void startHttpService_(Handler<AsyncResult<Void>> completionHandler) {
		Integer port = VertxConfig.config.getInteger(DEFAULT_PORT, "emulatorEmulator", "port");
		vertx.createHttpServer().requestHandler(req -> {
			req.exceptionHandler(t -> {
				log.error("exceptionHandler", t);
				JsonObject result = new JsonObject().put("error", "exceptionHandler : " + t);
				req.response().setChunked(true).putHeader("content-type", "application/json").setStatusCode(400)
						.end(result.encode() + '\n');
			});
			try {
				if (req.path().equals("/get/log")) {
					vertx.eventBus().<JsonObject>request(ServiceAddress.GridMaster.unitDatas(), null)
							.onComplete(rep -> {
								JsonObject result = null;
								if (rep.succeeded()) {
									result = rep.result().body();
									if (result != null) {
										if (log.isDebugEnabled())
											log.debug("size of result : " + result.size());
										if (Boolean.valueOf(System.getenv("removeDeprecatedData")))
											removeDeprecatedData_(result);
										cache.setJsonObject(result);
									} else {
										if (log.isWarnEnabled())
											log.warn("result is null");
									}
								} else {
									log.error("Communication failed on EventBus ; ", rep.cause());
								}
								if (result == null) {
									if (!cache.isNull()) {
										if (log.isWarnEnabled())
											log.warn("size of cache : " + cache.jsonObject().size());
									} else {
										if (log.isWarnEnabled())
											log.warn("cache is null");
									}
									result = (!cache.isNull()) ? cache.jsonObject() : empty_;
								}
								req.response().setChunked(true).putHeader("content-type", "application/json")
										.end(result.encode() + '\n');
							});
				} else if (req.path().startsWith("/get/unit/")) {
					String unitId = req.path().substring("/get/unit/".length());
					JsonObject unitData = (cache.isNull()) ? null : cache.getJsonObject(unitId);
					if (unitData == null) {
						// Provide default data if not in cache to allow starting
						unitData = new JsonObject()
								.put("unitId", unitId)
								.put("emu", new JsonObject()
										.put("rsoc", 50.0)
										.put("battery_operation_status", 1))
								.put("dcdc", new JsonObject()
										.put("status", new JsonObject()
												.put("status", 1)));
					}
					req.response().setChunked(true).putHeader("content-type", "application/json")
							.end(unitData.encode() + '\n');
				} else if (req.path().startsWith("/get/dcdc/status/")) {
					String unitId = req.path().substring("/get/dcdc/status/".length());
					JsonObject unitData = (cache.isNull()) ? null : cache.getJsonObject(unitId);
					JsonObject dcdcStatus = (unitData != null) ? unitData.getJsonObject("dcdc")
							: new JsonObject().put("status", new JsonObject().put("status", 1));
					req.response().setChunked(true).putHeader("content-type", "application/json")
							.end(dcdcStatus.encode() + '\n');
				} else if (req.path().startsWith("/set/dcdc/")) {
					String mode = req.getParam("mode");
					String opMode = "Waiting"; // Default
					if ("0x0014".equals(mode)) {
						opMode = "Grid Autonomy";
					} else if ("0x0041".equals(mode) || "0x0002".equals(mode)) {
						opMode = "Heteronomy CV";
					}
					// Simulate success for setting DCDC parameters and return expected status
					JsonObject result = new JsonObject()
							.put("succeeded", true)
							.put("status", new JsonObject().put("operationMode", opMode));
					req.response().setChunked(true).putHeader("content-type", "application/json")
							.end(result.encode() + '\n');
				} else {
					if (log.isWarnEnabled())
						log.warn("not found : " + req.uri());
					JsonObject result = new JsonObject().put("error", "not found : " + req.uri());
					req.response().setChunked(true).putHeader("content-type", "application/json").setStatusCode(404)
							.end(result.encode() + '\n');
				}
			} catch (Exception e) {
				log.error("exception", e);
				JsonObject result = new JsonObject().put("error", "exception : " + e);
				req.response().setChunked(true).putHeader("content-type", "application/json").setStatusCode(400)
						.end(result.encode() + '\n');
			}
		}).listen(port, res -> {
			if (res.succeeded()) {
				if (log.isInfoEnabled())
					log.info("emulator emulation http service started on port : " + port);
				completionHandler.handle(Future.succeededFuture());
			} else {
				log.error("error", res.cause());
				completionHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}

	/**
	 * Called if {@code removeDeprecatedData=true} is specified in environment
	 * variables and converts unit data.
	 * Removes the attributes below and returns data.
	 * - Under emu
	 * - Under dcdc.powermeter
	 * 
	 * @param data Unit data to be converted
	 *             環境変数に {@code removeDeprecatedData=true}
	 *             が指定されていた場合に呼ばれユニットデータをコンバートする.
	 *             以下の属性を削除して返す.
	 *             - emu 以下
	 *             - dcdc.powermeter 以下
	 * @param data コンバート対象のユニットデータ
	 */
	private void removeDeprecatedData_(JsonObject data) {
		for (String aUnitId : data.fieldNames()) {
			JsonObject aUnitData = data.getJsonObject(aUnitId);
			aUnitData.remove("emu");
			JsonObjectUtil.remove(aUnitData, "dcdc", "powermeter");
		}
	}

}
