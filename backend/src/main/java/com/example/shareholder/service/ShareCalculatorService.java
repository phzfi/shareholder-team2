package com.example.shareholder.service;

import org.springframework.stereotype.Service;

import com.example.shareholder.repository.ShareTransactionRepository;

@Service
public class ShareCalculatorService {

  private final ShareTransactionRepository shareTransactionRepository;

  public ShareCalculatorService(ShareTransactionRepository shareTransactionRepository) {
    this.shareTransactionRepository = shareTransactionRepository;
  }

  public Integer calculateTotalShares() {
    return shareTransactionRepository.findAll().stream().mapToInt(shareholder -> shareholder.getNumberOfShares()).sum();
  }
}
