package com.closiq.inventory.service;

import com.closiq.inventory.domain.InventoryHistory;
import com.closiq.inventory.domain.InventoryItem;
import com.closiq.inventory.repository.InventoryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryHistoryService {

    private final InventoryHistoryRepository historyRepository;

    @Transactional
    public void recordItemCreated(InventoryItem item, UUID actorId) {
        historyRepository.save(InventoryHistory.builder()
                .inventoryItem(item)
                .actorId(actorId)
                .eventType("ITEM_CREATED")
                .toStatus(item.getStatus())
                .occurredAt(Instant.now())
                .build());
    }

    @Transactional
    public void recordStatusChange(
            InventoryItem item, String fromStatus, String toStatus, UUID actorId, String eventType) {

        historyRepository.save(InventoryHistory.builder()
                .inventoryItem(item)
                .actorId(actorId)
                .eventType(eventType)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .occurredAt(Instant.now())
                .build());
    }

    @Transactional
    public void recordEvent(InventoryItem item, String eventType, UUID actorId, Map<String, Object> payload) {
        historyRepository.save(InventoryHistory.builder()
                .inventoryItem(item)
                .actorId(actorId)
                .eventType(eventType)
                .payload(payload)
                .occurredAt(Instant.now())
                .build());
    }
}
