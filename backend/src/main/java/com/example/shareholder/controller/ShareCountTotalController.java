package com.example.shareholder.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.example.shareholder.model.ShareCountTotal;
import com.example.shareholder.service.ShareCountTotalService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/totalshares")
public class ShareCountTotalController {

  private final ShareCountTotalService shareCountTotalService;

  public ShareCountTotalController(ShareCountTotalService shareCountTotalService) {
    this.shareCountTotalService = shareCountTotalService;
  }

  @GetMapping("/all")
  public ResponseEntity<List<ShareCountTotal>> getAllTotalCounts() {
    List<ShareCountTotal> shareCountTotals = shareCountTotalService.getAllTotalCounts();
    return ResponseEntity.ok().body(shareCountTotals);
  }

  @GetMapping("/latest")
  public ResponseEntity<ShareCountTotal> getLatestTotalCount() {
    ShareCountTotal shareCountTotal = shareCountTotalService.getLatestTotalCount();
    return ResponseEntity.ok().body(shareCountTotal);
  }
}
