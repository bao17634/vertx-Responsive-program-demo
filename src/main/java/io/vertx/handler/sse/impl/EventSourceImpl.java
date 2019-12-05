package io.vertx.handler.sse.impl;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.handler.sse.EventSource;
import io.vertx.handler.sse.SSEHeaders;
import io.vertx.handler.sse.exceptions.ConnectionRefusedException;


import java.util.HashMap;
import java.util.Map;

public class EventSourceImpl implements EventSource {

	private HttpClient client;
	private boolean connected;
	private String lastId;
	private Handler<String> messageHandler;
	private final Map<String, Handler<String>> eventHandlers;
	private SSEPacket currentPacket;
	private final Vertx vertx;
	private final HttpClientOptions options;
	private Handler<Void> closeHandler;

	public EventSourceImpl(Vertx vertx, HttpClientOptions options) {
		options.setKeepAlive(true);
		this.vertx = vertx;
		this.options = options;
		eventHandlers = new HashMap<>();
	}

	@Override
	public EventSource connect(String path, Handler<AsyncResult<Void>> handler) {
		return connect(path, null, handler);
	}

	@Override
	public EventSource connect(String path, String lastEventId, Handler<AsyncResult<Void>> handler) {
		if (connected) {
			throw new VertxException("SSEConnection already connected");
		}
		if (client == null) {
			client = vertx.createHttpClient(options);
		}
		HttpClientRequest request = client.get(path, response -> {
			if (response.statusCode() != 200) {
				ConnectionRefusedException ex = new ConnectionRefusedException(response);
				handler.handle(Future.failedFuture(ex));
			} else {
				connected = true;
				response.handler(this::handleMessage);
				if (closeHandler != null) {
					response.endHandler(closeHandler);
				}
				handler.handle(Future.succeededFuture());
			}
		});
		if (lastEventId != null) {
			request.headers().add("Last-Event-ID", lastEventId);
		}
		request.setChunked(true);
		request.headers().add(HttpHeaders.ACCEPT, "text/event-stream");
		request.end();
		return this;
	}

	@Override
	public EventSource close() {
		client.close();
		client = null;
		connected = false;
		return this;
	}

	@Override
	public EventSource onMessage(Handler<String> messageHandler) {
		this.messageHandler = messageHandler;
		return this;
	}

	@Override
	public EventSource onEvent(String eventName, Handler<String> handler) {
		eventHandlers.put(eventName, handler);
		return this;
	}

	@Override
	public EventSource onClose(Handler<Void> closeHandler) {
		this.closeHandler = closeHandler;
		return this;
	}

	@Override
	public String lastId() {
		return lastId;
	}

	private void handleMessage(Buffer buffer) {
		if (currentPacket == null) {
			currentPacket = new SSEPacket();
		}
		boolean terminated = currentPacket.append(buffer);
		if (terminated) {
			// choose the right handler and call it
			Handler<String> handler = messageHandler;
			String header = currentPacket.headerName;
			if (header == null) {
				messageHandler.handle(currentPacket.toString());
			} else {
				switch (currentPacket.headerName) {
					case SSEHeaders.EVENT:
						handler = eventHandlers.get(currentPacket.headerValue);
						break;
					case SSEHeaders.ID:
						lastId = currentPacket.headerValue;
						break;
					case SSEHeaders.RETRY:
						// FIXME : we should automatically handle this ?
				}
				if (handler != null) {
					handler.handle(currentPacket.toString());
				}
			}
			currentPacket = null;
		}
	}
}
