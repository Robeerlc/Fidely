package com.fidely.ui.controller;

import com.fidely.domain.dto.request.card.RedeemRequest;
import com.fidely.domain.dto.request.card.ScanRequest;
import com.fidely.domain.dto.response.card.CardInfoResponse;
import com.fidely.domain.dto.response.card.RedeemResponse;
import com.fidely.domain.dto.response.card.ScanResponse;
import com.fidely.domain.service.SseService;
import com.fidely.domain.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/scan")
@RequiredArgsConstructor
public class ScanController {

    private final WalletService walletService;
    private final SseService sseService;

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

    @GetMapping("/stream/{secureUuid}")
    public SseEmitter streamUpdates(@PathVariable String secureUuid) {
        return sseService.subscribe(secureUuid);
    }
}