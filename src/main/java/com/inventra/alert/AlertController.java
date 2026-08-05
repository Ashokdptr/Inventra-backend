package com.inventra.alert;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<List<AlertResponse>> getAll() {
        return ResponseEntity.ok(alertService.getAll());
    }

    @GetMapping("/unread")
    public ResponseEntity<List<AlertResponse>> getUnread() {
        return ResponseEntity.ok(alertService.getUnread());
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countUnread() {
        return ResponseEntity.ok(alertService.countUnread());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<AlertResponse> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.markRead(id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        alertService.markAllRead();
        return ResponseEntity.noContent().build();
    }
}
