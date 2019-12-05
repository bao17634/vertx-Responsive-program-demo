package io.vertx.quotes;


import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @ClassName: QuoteGenerator
 * @Description: TODO
 * @Author: yanrong
 * @Date: 2019/12/4 18:22
 */

public class QuoteGenerator {
    private MathContext mathContext = new MathContext(2);

    private Random random = new Random();

    private List<Quote> prices = new ArrayList();

    public QuoteGenerator() {
        this.prices.add(new Quote("CTXS", 82.26, Instant.now()));
        this.prices.add(new Quote("DELL", 63.74, Instant.now()));
        this.prices.add(new Quote("GOOG", 847.24, Instant.now()));
        this.prices.add(new Quote("MSFT", 65.11, Instant.now()));
        this.prices.add(new Quote("ORCL", 45.71, Instant.now()));
        this.prices.add(new Quote("RHT", 84.29, Instant.now()));
        this.prices.add(new Quote("VMW", 92.21, Instant.now()));

    }

    public Quote fetchQuoteStream(Long timerId,Integer index) {
        Quote updatedQuote = updateQuote(prices.get(index));
        return updatedQuote;
    }

    private Quote updateQuote(Quote quote) {
        BigDecimal priceChange = quote.getPrice()
                .multiply(new BigDecimal(0.05 * this.random.nextDouble()), this.mathContext);
        return new Quote(quote.getTicker(), quote.getPrice().add(priceChange), Instant.now());
    }
}
