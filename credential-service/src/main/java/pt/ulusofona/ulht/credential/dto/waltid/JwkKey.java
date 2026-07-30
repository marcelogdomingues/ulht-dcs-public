package pt.ulusofona.ulht.credential.dto.waltid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON Web Key (JWK) representation
 * Field names must match JWK standard (RFC 7517)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwkKey {
    @JsonProperty("kty")
    private String kty;  // "OKP", "EC", "RSA"
    
    @JsonProperty("d")
    private String d;  // Private key component
    
    @JsonProperty("crv")
    private String crv;  // Curve name: "Ed25519", "secp256k1", "P-256", "P-384"
    
    @JsonProperty("kid")
    private String kid;  // Key ID
    
    @JsonProperty("x")
    private String x;  // Public key x coordinate
    
    @JsonProperty("y")
    private String y;  // Public key y coordinate (for EC keys)
    
    @JsonProperty("n")
    private String n;  // RSA modulus
    
    @JsonProperty("e")
    private String e;  // RSA exponent
}

