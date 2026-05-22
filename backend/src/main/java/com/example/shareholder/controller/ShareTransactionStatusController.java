package com.example.shareholder.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.List;
import com.example.shareholder.model.ShareTransaction;
import com.example.shareholder.service.ShareTransactionService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/sharetransactionstatus")
public class ShareTransactionStatusController {

  private final ShareTransactionService shareTransactionService;

  public ShareTransactionStatusController(ShareTransactionService shareTransactionService) {
    this.shareTransactionService = shareTransactionService;
  }

  // 'pending', 'approved', 'rejected'
  @GetMapping("/{status}")
  public ResponseEntity<List<ShareTransaction>> getPendingTransactions(@PathVariable String status) {
    List<ShareTransaction> transactions = shareTransactionService.getTransactionsByStatus(status);
    return ResponseEntity.ok().body(transactions);
  }
}
