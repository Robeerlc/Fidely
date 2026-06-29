package com.fidely.domain.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidely.dao.repository.WalletCardRepository;
import com.fidely.domain.dto.CampaignEvent;
import com.fidely.domain.service.EmailService;
import com.fidely.domain.service.GoogleWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final WalletCardRepository walletCardRepository;
    private final GoogleWalletService googleWalletService;

    @KafkaListener(topics = "campaign-notifications", groupId = "fidely-group")
    public void consume(String message) {
        try {
            CampaignEvent event = objectMapper.readValue(message, CampaignEvent.class);
            log.info("Procesando campaña para: {}", event.getEmail());

            emailService.sendMarketingEmail(
                    event.getEmail(),
                    event.getBrandName(),
                    event.getSubject(),
                    event.getMessage()
            );

            if (event.getWalletCardId() != null) {
                walletCardRepository.findById(event.getWalletCardId())
                        .ifPresent(card ->
                                googleWalletService.updateCardAndTriggerPush(card, event.getSubject())
                        );
            }
        } catch (Exception e) {
            log.error("Error procesando evento de campaña: {}", e.getMessage());
        }
    }
}
