package com.example.dcs.credential.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * User model for Sis.
 * Independent model for the credential service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SisUser {
    
    private String id;
    private String name;
    private String email;
    private String studentId;
    private String department;
    private String status;
}
