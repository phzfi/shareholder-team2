package com.example.shareholder.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import com.example.shareholder.model.ShareOwnership;
import com.example.shareholder.service.ShareOwnershipService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/shareownership")
public class ShareOwnershipController {

  private final ShareOwnershipService shareOwnershipService;

  public ShareOwnershipController(ShareOwnershipService shareOwnershipService) {
    this.shareOwnershipService = shareOwnershipService;
  }

  @GetMapping("/all")
  public ResponseEntity<List<ShareOwnership>> getAllShareOwnerships() {
    List<ShareOwnership> shareOwnerships = shareOwnershipService.getAllShareOwnerships();
    return ResponseEntity.ok().body(shareOwnerships);
  }

  @GetMapping("/{id}")
  public ResponseEntity<ShareOwnership> getShareOwnershipById(@PathVariable Long id) {
    ShareOwnership shareOwnership = shareOwnershipService.getShareOwnershipById(id);
    return ResponseEntity.ok().body(shareOwnership);
  }

  @GetMapping("/count")
  public long getTotalShareOwnership() {
      return shareOwnershipService.getTotalShareOwnership();
  }
}
