package pt.ulusofona.ulht.credential.dto.waltid;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for cryptographic key creation in walt.id
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeyConfig {
    /**
     * Storage backend for the key
     * Options: "jwk" (managed by you), "TSE" (Hashicorp Vault), "oci" (Oracle KMS)
     */
    private String backend;
    
    /**
     * Algorithm/type of key to generate
     * Options: "ed25519", "secp256k1", "secp256r1", "RSA"
     */
    private String keyType;
}

