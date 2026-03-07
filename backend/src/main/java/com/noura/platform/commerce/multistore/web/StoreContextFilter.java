package com.noura.platform.commerce.multistore.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.noura.platform.commerce.multistore.application.StoreContext;

import java.io.IOException;

/**
 * Filter that sets the store context based on request headers.
 * Looks for X-Store-ID header or store query parameter.
 */
@Component
@Order(1)
public class StoreContextFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(StoreContextFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                // Check header first
                String storeIdHeader = httpRequest.getHeader("X-Store-ID");
                if (storeIdHeader != null && !storeIdHeader.isBlank()) {
                    try {
                        Long storeId = Long.parseLong(storeIdHeader);
                        StoreContext.setStoreId(storeId);
                        log.debug("Store context set from header: {}", storeId);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid X-Store-ID header: {}", storeIdHeader);
                    }
                }

                // Fallback to query parameter
                if (!StoreContext.hasStore()) {
                    String storeParam = httpRequest.getParameter("storeId");
                    if (storeParam != null && !storeParam.isBlank()) {
                        try {
                            Long storeId = Long.parseLong(storeParam);
                            StoreContext.setStoreId(storeId);
                            log.debug("Store context set from parameter: {}", storeId);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid storeId parameter: {}", storeParam);
                        }
                    }
                }
            }

            chain.doFilter(request, response);
        } finally {
            StoreContext.clear();
        }
    }
}
