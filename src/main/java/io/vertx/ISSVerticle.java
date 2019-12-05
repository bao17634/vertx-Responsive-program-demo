package io.vertx;


import com.alibaba.fastjson.JSONObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.handler.sse.SSEHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.quotes.*;

import lombok.var;

import java.util.ArrayList;
import java.util.List;


public class ISSVerticle extends AbstractVerticle {

    public static void main(String[] args) {
        ISSVerticle issVerticle = new ISSVerticle();
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(issVerticle.getClass().getName());
    }

    public final static int PORT = 9002;
    public static Integer INDEX = 0;
    private final static Logger LOG = LoggerFactory.getLogger(ISSVerticle.class.getName());
    private final static String EB_ADDRESS = "iss-position";
    private HttpServer server;
    private StaticHandler staticFiles = StaticHandler.create();
    private SSEHandler sse = SSEHandler.create();
    private Long timerId;
    private HttpClient client;


    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        client = vertx.createHttpClient(new HttpClientOptions()
                //如果在发出请求时没有提供主机名设置此客户端在请求中使用的默认主机名。
                .setDefaultHost("api.open-notify.org"));
        LOG.info("服务启动");
    }

    @Override
    public void start(Future<Void> future) {
        server = vertx.createHttpServer();
        var router = Router.router(vertx);
        router.get("/").handler(
                rc -> rc.reroute("/index.html")
        );
        //解决vertx跨域问题
        router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET));
        router.route("/*").handler(StaticHandler.create());
        router.get("/static/*").handler(staticFiles);
        router.get("/iss/position").handler(sse);
        sse.connectHandler(connection -> {
            //注册消息使用地址者
            connection.forward(EB_ADDRESS);
        });
        //设置一个定期计时器，定时触发一次，此时将使用计时器的ID调用函数
        vertx.setPeriodic(500, this::fetchISSPosition);
        vertx.setPeriodic(500, this::fetchISSPosition1);
        server.requestHandler(router);
        server.listen(PORT, future.<HttpServer>map(v -> null).completer());
    }

    /**
     * quotes.html页面数据
     * @param timerId
     */
    private void fetchISSPosition(Long timerId) {
        QuoteGenerator quoteGenerator = new QuoteGenerator();
        List<Quote> data = new ArrayList<Quote>();
        MessageInfo messageInfo = new MessageInfo();
        this.timerId = timerId;
        messageInfo.setErrorCode("200");
        messageInfo.setErrorMsg("success");
        Quote quote = quoteGenerator.fetchQuoteStream(timerId, INDEX++);
        if (INDEX > 6) {
            INDEX = 0;
        }
        data.add(quote);
        messageInfo.setData(data);
        String json = JSONObject.toJSONString(quote);
        vertx.eventBus().publish(EB_ADDRESS, json);
    }

    /**
     * index.html页面数据
     * @param timerId
     */
    private void fetchISSPosition1(Long timerId) {
        this.timerId = timerId;
        client.getNow("/iss-now.json", resp -> {
            if (resp.statusCode() != 200) {
                LOG.error("Could not fetch ISS position {}", resp.statusCode());
                return;
            }
            resp.bodyHandler(buff -> {
                var json = buff.toJsonObject();
                if (!"success".equals(json.getString("message"))) {
                    LOG.error("Could not fetch ISS position {}", json.toString());
                    return;
                }
                var position = json.getJsonObject("iss_position");
                vertx.eventBus().publish(EB_ADDRESS, position);
            });
        });
    }
    @Override
    public void stop(Future<Void> future) {
        if (timerId != null) {
            vertx.cancelTimer(timerId);
        }
        if (server == null) {
            future.complete();
            return;
        }
        server.close(future.completer());
    }

}
