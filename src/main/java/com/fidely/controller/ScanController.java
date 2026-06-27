package com.fidely.controller;

import com.fidely.dto.*;
import com.fidely.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/info/{secureUuid}")
    public ResponseEntity<CardInfoResponse> getCardInfo(@PathVariable String secureUuid, @RequestParam Long businessId) {
        CardInfoResponse response = walletService.getCardInfo(secureUuid, businessId);
        return ResponseEntity.ok(response);
    }
}