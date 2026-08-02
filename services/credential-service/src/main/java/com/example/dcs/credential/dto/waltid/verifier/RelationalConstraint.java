package com.example.dcs.credential.dto.waltid.verifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Relational constraint for verification.
 * Used with is_holder and same_subject constraints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RelationalConstraint {
    
    /**
     * Field IDs to apply the constraint to
     */
    @JsonProperty("field_id")
    private List<String> fieldId;
    
    /**
     * Directive: "required" or "preferred"
     */
    private String directive;
}


