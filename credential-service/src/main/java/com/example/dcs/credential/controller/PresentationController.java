package com.example.dcs.credential.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.dcs.credential.dto.waltid.verifier.*;
import com.example.dcs.credential.exception.ExternalServiceException;
import com.example.dcs.credential.service.VerifierService;

import java.util.List;

@RestController
@RequestMapping("/presentations/holder-initiated")
@RequiredArgsConstructor
public class PresentationController {

    private final VerifierService verifierService;

    @PostMapping("/start")
    public ResponseEntity<HolderInitiatedStartResponse> startHolderInitiated(
            @Valid @RequestBody HolderInitiatedStartRequest request
    ) throws ExternalServiceException {
        String format = (request.getFormat() == null || request.getFormat().isBlank())
                ? "jwt_vc_json" : request.getFormat();

        RequestedCredential credential = RequestedCredential.builder()
                .type(request.getCredentialType())
                .format(format)
                .build();

        VerificationRequest verificationRequest = VerificationRequest.builder()
                .requestCredentials(List.of(credential))
                .vpPolicies(request.getVpPolicies())
                .vcPolicies(request.getVcPolicies())
                .build();

        VerificationResponse verificationResponse = verifierService.initiateVerification(verificationRequest, null);

        HolderInitiatedStartResponse response = HolderInitiatedStartResponse.builder()
                .walletUrl(verificationResponse.getUrl())
                .state(verificationResponse.getState())
                .presentationId(verificationResponse.getPresentationId())
                .build();

        return ResponseEntity.ok(response);
    }
}
