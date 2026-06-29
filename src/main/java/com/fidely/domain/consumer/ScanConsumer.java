package com.fidely.domain.consumer;

import com.fidely.dao.repository.WalletCardRepository;
import com.fidely.domain.dto.ScanUpdateEvent;
import com.fidely.domain.service.GoogleWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScanConsumer {
    private final WalletCardRepository walletCardRepository;
    private final GoogleWalletService googleWalletService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "scan-updates", groupId = "fidely-group")
    public void consumeScanUpdate(String message) {
        try {
            ScanUpdateEvent event = objectMapper.readValue(message, ScanUpdateEvent.class);
            walletCardRepository.findById(event.walletCardId()).ifPresent(card -> googleWalletService.updateCardAndTriggerPush(card, event.pushMessage()));
        } catch (Exception e) {
            log.error("Error procesando scan-update en Kafka: {}", e.getMessage());
        }
    }
}