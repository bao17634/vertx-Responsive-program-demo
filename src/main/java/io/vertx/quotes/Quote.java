package io.vertx.quotes;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;

public class Quote {

	private static final MathContext MATH_CONTEXT = new MathContext(2);

	private String ticker;
	private BigDecimal price;
	private Instant instant;

	public Quote(String ticker, BigDecimal price,Instant instant) {
		this.ticker = ticker;
		this.price = price;
		this.instant=instant;
	}

	public Quote(String ticker, Double price,Instant instant) {
		this(ticker, new BigDecimal(price, MATH_CONTEXT),instant);
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Instant getInstant() {
		return instant;
	}

	public void setInstant(Instant instant) {
		this.instant = instant;
	}

	@Override
	public String toString() {
		return "Quote{" +
				"ticker='" + ticker + '\'' +
				", price=" + price +
				", instant=" + instant +
				'}';
	}
}
