package io.vertx.handler.sse.impl;

import io.vertx.core.MultiMap;
import io.vertx.core.VertxException;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.handler.sse.SSEConnection;
import io.vertx.handler.sse.SSEHeaders;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class SSEConnectionImpl implements SSEConnection {

	private static final String MSG_SEPARATOR = "\n";
	private static final String PACKET_SEPARATOR = "\n\n";

	private final RoutingContext context;
	private boolean rejected;
	private List<MessageConsumer> consumers = new ArrayList<>();

	public SSEConnectionImpl(RoutingContext context) {
		this.context = context;
	}

	@Override
	public SSEConnection forward(String address) {
		consumers.add(context.vertx().eventBus().consumer(address, this::ebMsgHandler));
		return this;
	}

	@Override
	public SSEConnection forward(List<String> addresses) {
		consumers = addresses.stream().map(address ->
			context.vertx().eventBus().consumer(address, this::ebMsgHandler)
		).collect(toList());
		return this;
	}

	@Override
	public SSEConnection reject(int code) {
		return reject(code, null);
	}

	@Override
	public SSEConnection reject(int code, String reason) {
		rejected = true;
		HttpServerResponse response = context.response();
		response.setStatusCode(code);
		if (reason != null) {
			response.setStatusMessage(reason);
		}
		response.end();
		return this;
	}

	@Override
	public SSEConnection comment(String comment) {
		context.response().write("comment: " + comment + PACKET_SEPARATOR);
		return this;
	}

	@Override
	public SSEConnection retry(Long delay, List<String> data) {
		return withHeader(SSEHeaders.RETRY, delay.toString(), data);
	}

	@Override
	public SSEConnection retry(Long delay, String data) {
		return withHeader(SSEHeaders.RETRY, delay.toString(), data);
	}

	@Override
	public SSEConnection data(List<String> data) {
		return appendData(data);
	}

	@Override
	public SSEConnection data(String data) {
		return writeData(data);
	}

	@Override
	public SSEConnection event(String eventName, List<String> data) {
		return withHeader(SSEHeaders.EVENT, eventName, data);
	}

	@Override
	public SSEConnection event(String eventName, String data) {
		return withHeader(SSEHeaders.EVENT, eventName, data);
	}

	@Override
	public SSEConnection id(String id, List<String> data) {
		return withHeader(SSEHeaders.ID, id, data);
	}

	@Override
	public SSEConnection id(String id, String data) {
		return withHeader(SSEHeaders.ID, id, data);
	}

	@Override
	public SSEConnection close() {
		try {
			context.response().end(); // best effort
		} catch(VertxException | IllegalStateException e) {
			// connection has already been closed by the browser
			// do not log to avoid performance issues (ddos issue if client opening and closing alot of connections abruptly)
		}
		if (!consumers.isEmpty()) {
			consumers.forEach(MessageConsumer::unregister);
		}
		return this;
	}

	@Override
	public HttpServerRequest request() {
		return context.request();
	}

	@Override
	public String lastId() {
		return request().getHeader("Last-Event-ID");
	}

	@Override
	public boolean rejected() {
		return rejected;
	}

	private SSEConnection withHeader(String headerName, String headerValue, String data) {
		writeHeader(headerName, headerValue);
		writeData(data);
		return this;
	}

	private SSEConnection withHeader(String headerName, String headerValue, List<String> data) {
		writeHeader(headerName, headerValue);
		appendData(data);
		return this;
	}

	private SSEConnection writeHeader(String headerName, String headerValue) {
		context.response().write(headerName + ": " + headerValue + MSG_SEPARATOR);
		return this;
	}

	private SSEConnection writeData(String data) {
		context.response().write("data: " + data + PACKET_SEPARATOR);
		return this;
	}

	private SSEConnection appendData(List<String> data) {
		for (int i = 0; i < data.size(); i++) {
			String separator = i == data.size() - 1 ? PACKET_SEPARATOR : MSG_SEPARATOR;
			context.response().write("data: " + data.get(i) + separator);
		}
		return this;
	}

	private void ebMsgHandler(Message<?> msg) {
		MultiMap headers = msg.headers();
		String eventName = headers.get(SSEHeaders.EVENT);
		String id = headers.get(SSEHeaders.ID);
		String data = msg.body() == null ? "" : msg.body().toString();
		if (eventName != null) {
			this.event(eventName, data);
		}
		if (id != null) {
			this.id(id, data);
		}
		if (eventName == null && id == null) {
			this.data(data);
		}
	}
}
