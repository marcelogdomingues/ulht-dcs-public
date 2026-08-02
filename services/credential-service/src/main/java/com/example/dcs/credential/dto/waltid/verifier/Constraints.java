package com.example.dcs.credential.dto.waltid.verifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Constraints for credential verification in input descriptor.
 * Supports relational constraints like subject_is_issuer, is_holder, same_subject.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Constraints {
    
    /**
     * Fields to check in the credential
     */
    private List<Field> fields;
    
    /**
     * Relational constraint: credential must be self-issued
     * Values: "required", "preferred"
     */
    @JsonProperty("subject_is_issuer")
    private String subjectIsIssuer;
    
    /**
     * Relational constraint: bind specific fields to holder's DID
     */
    @JsonProperty("is_holder")
    private List<RelationalConstraint> isHolder;
    
    /**
     * Relational constraint: multiple credentials must have same subject
     */
    @JsonProperty("same_subject")
    private List<RelationalConstraint> sameSubject;
}


