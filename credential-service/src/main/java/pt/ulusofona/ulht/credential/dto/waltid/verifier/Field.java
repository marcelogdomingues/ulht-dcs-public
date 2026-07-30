package pt.ulusofona.ulht.credential.dto.waltid.verifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Field constraint in input descriptor.
 * Specifies which fields to check and their expected values.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Field {
    
    /**
     * Optional field identifier for relational constraints
     */
    private String id;
    
    /**
     * JSON paths to the field (e.g., ["$.vc.type"])
     */
    private List<String> path;
    
    /**
     * Filter to apply to the field value
     */
    private Map<String, Object> filter;
}


