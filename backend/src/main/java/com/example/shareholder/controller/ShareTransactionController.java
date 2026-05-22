package com.example.shareholder.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

import com.example.shareholder.model.ShareTransaction;
import com.example.shareholder.service.ShareTransactionService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/transactions")
public class ShareTransactionController {

  private final ShareTransactionService shareTransactionService;

  public ShareTransactionController(ShareTransactionService shareTransactionService) {
    this.shareTransactionService = shareTransactionService;
  }


  @GetMapping
  public ResponseEntity<List<ShareTransaction>> getShareTransactions() {
    List<ShareTransaction> shareholders = shareTransactionService.getShareTransactions();
    return ResponseEntity.ok().body(shareholders);
  }

  @GetMapping("/{id}")
  public ResponseEntity<ShareTransaction> getShareTransactionById(@PathVariable Long id) {
    ShareTransaction shareholder = shareTransactionService.getShareTransactionById(id);
    return ResponseEntity.ok().body(shareholder);
  }

  @PostMapping("/add")
  public ResponseEntity<ShareTransaction> addTransaction(@RequestBody ShareTransaction shareTransaction) {
    ShareTransaction newShareholder = shareTransactionService.addShareTransaction(shareTransaction);
    return ResponseEntity.ok().body(newShareholder);
  }

  @PutMapping("/{id}")
  public ResponseEntity<ShareTransaction> updateTransaction(@PathVariable Long id, @RequestBody ShareTransaction shareTransaction) {
    ShareTransaction updatedShareholder = shareTransactionService.updateShareTransaction(id, shareTransaction);
    return ResponseEntity.ok().body(updatedShareholder);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteTransaction(@PathVariable Long id) {
    shareTransactionService.deleteShareTransaction(id);
    return ResponseEntity.ok().body("Transaktio poistettu onnistuneesti");
  }

  @GetMapping("/search")
  public ResponseEntity<List<ShareTransaction>> searchShareTransactions(@RequestParam(required = false) String search) {
    List<ShareTransaction> shareholders = shareTransactionService.searchShareTransactions(search);
    return ResponseEntity.ok().body(shareholders);
  }
}
