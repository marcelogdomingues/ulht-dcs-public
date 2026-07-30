package pt.ulusofona.ulht.credential.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * User model for Lusofona.
 * Independent model for the credential service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LusofonaUser {
    
    private String id;
    private String name;
    private String email;
    private String studentId;
    private String department;
    private String status;
}
