package com.fidely.controller;

import com.fidely.dto.RedeemRequest;
import com.fidely.dto.RedeemResponse;
import com.fidely.dto.ScanRequest;
import com.fidely.dto.ScanResponse;
import com.fidely.service.WalletService;
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

    @PostMapping("/redeem")
    public ResponseEntity<RedeemResponse> redeemReward(@Valid @RequestBody RedeemRequest request) {
        RedeemResponse response = walletService.redeemReward(request);
        return ResponseEntity.ok(response);
    }
}