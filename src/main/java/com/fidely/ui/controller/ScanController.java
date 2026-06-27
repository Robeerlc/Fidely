package com.fidely.ui.controller;

import com.fidely.ui.dto.Scan.ScanRequest;
import com.fidely.ui.dto.Scan.ScanResponse;
import com.fidely.domain.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scan")
@RequiredArgsConstructor
public class ScanController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<ScanResponse> scanCard(@Valid @RequestBody ScanRequest request) {
        ScanResponse response = walletService.processScan(request);
        if (response.isSuccess()) return ResponseEntity.ok(response);
        else return ResponseEntity.badRequest().body(response);
    }
}