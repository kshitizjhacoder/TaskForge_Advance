package com.kshitiz.taskforge.auth.adapter.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kshitiz.taskforge.common.adapter.rest.ProblemDetailWriter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Limits how often one client IP can call the authentication endpoints
 * ({@code /api/auth/**}), to slow down password-guessing and sign-up spam.
 *
 * <p>Uses the token-bucket algorithm (Bucket4j): each IP gets a bucket of N tokens that
 * refills to N every minute; each request takes one token; an empty bucket means 429 Too
 * Many Requests. Buckets are kept in a Caffeine cache that forgets idle IPs after 10 minutes
 * and holds at most 100,000 entries, so memory use stays bounded.
 *
 * <p>Limits are per application instance. With several instances behind a load balancer, a
 * shared store (e.g. Redis) would be needed.
 */
@Slf4j
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/auth/";

    private final ProblemDetailWriter problemDetailWriter;
    private final int requestsPerMinute;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    /**
     * Creates the filter.
     *
     * @param problemDetailWriter writes the 429 error response
     * @param requestsPerMinute   allowed requests per IP per minute
     */
    public AuthRateLimitFilter(ProblemDetailWriter problemDetailWriter, int requestsPerMinute) {
        this.problemDetailWriter = problemDetailWriter;
        this.requestsPerMinute = requestsPerMinute;
    }

    /**
     * Only authentication endpoints are rate-limited; every other request skips this filter.
     *
     * @param request incoming request
     * @return {@code true} to skip the filter
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith(AUTH_PATH_PREFIX);
    }

    /**
     * Takes one token from the caller's bucket, or answers 429 if none are left.
     *
     * @param request     incoming request
     * @param response    outgoing response
     * @param filterChain remaining filters
     * @throws ServletException if a later filter fails
     * @throws IOException      if a later filter fails or the response cannot be written
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Bucket bucket = buckets.get(request.getRemoteAddr(), ip -> newBucket());
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }
        log.warn("Rate limit exceeded on {} from {}", request.getRequestURI(), request.getRemoteAddr());
        response.setHeader(HttpHeaders.RETRY_AFTER, "60");
        problemDetailWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS,
                "Too many attempts. Please wait a minute and try again.");
    }

    /**
     * Creates a full bucket for a new client IP.
     *
     * @return bucket with {@code requestsPerMinute} tokens, refilled every minute
     */
    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(requestsPerMinute)
                        .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                        .build())
                .build();
    }
}
