package ru.denisov.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_FORWARDED_PREFIX = "X-Forwarded-Prefix";
    private static final int FILTER_ORDER = -1;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = getOrGenerateRequestId(exchange);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> headers.remove(HEADER_FORWARDED_PREFIX))
                .header(HEADER_REQUEST_ID, requestId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        log.info("Incoming request: id={}, method={}, path={}",
                requestId,
                mutatedRequest.getMethod(),
                mutatedRequest.getURI().getPath());

        return chain.filter(mutatedExchange)
                .doOnSuccess(v -> log.info(
                        "Request completed: id={}, status={}",
                        requestId,
                        mutatedExchange.getResponse().getStatusCode()))
                .doOnError(e -> log.error(
                        "Request failed: id={}, error={}",
                        requestId,
                        e.getMessage()));
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    private String getOrGenerateRequestId(ServerWebExchange exchange) {
        String existingId = exchange.getRequest().getHeaders().getFirst(HEADER_REQUEST_ID);
        return existingId != null ? existingId : UUID.randomUUID().toString();
    }
}
