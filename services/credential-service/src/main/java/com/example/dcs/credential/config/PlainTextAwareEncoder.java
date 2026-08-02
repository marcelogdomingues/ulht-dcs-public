package com.example.dcs.credential.config;

import feign.RequestTemplate;
import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * Custom Feign encoder that handles plain text request bodies.
 * 
 * When Content-Type is "text/plain" and the body is a String,
 * sends it as raw text without JSON encoding (no quotes).
 * Otherwise, delegates to the default Jackson encoder.
 */
@Slf4j
public class PlainTextAwareEncoder implements Encoder {
    
    private final Encoder defaultEncoder;
    
    public PlainTextAwareEncoder(Encoder defaultEncoder) {
        this.defaultEncoder = defaultEncoder;
    }
    
    @Override
    public void encode(Object object, Type bodyType, RequestTemplate template) {
        // Check if Content-Type is text/plain
        String contentType = template.headers().get("Content-Type") != null ?
            template.headers().get("Content-Type").iterator().next() : null;
        
        // If content type is text/plain and body is a String, send as raw text
        if ("text/plain".equals(contentType) && object instanceof String) {
            String text = (String) object;
            log.debug("Encoding plain text body (length: {}): {}...", 
                    text.length(), 
                    text.length() > 100 ? text.substring(0, 100) : text);
            
            // Set body as raw bytes (no JSON encoding, no quotes)
            template.body(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            return;
        }
        
        // Otherwise, use default encoder (JSON for objects, etc.)
        defaultEncoder.encode(object, bodyType, template);
    }
}

