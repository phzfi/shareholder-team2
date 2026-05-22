package com.example.shareholder.service;

import org.springframework.stereotype.Service;
import java.util.*;

import com.example.shareholder.model.ShareCountTotal;
import com.example.shareholder.repository.ShareCountTotalRepository;

@Service
public class ShareCountTotalService {

  private final ShareCountTotalRepository shareCountTotalRepository;

  public ShareCountTotalService(ShareCountTotalRepository shareCountTotalRepository) {
    this.shareCountTotalRepository = shareCountTotalRepository;
  }

  public List<ShareCountTotal> getAllTotalCounts() {
    return shareCountTotalRepository.findAll();
  }

  public ShareCountTotal getLatestTotalCount() {
    return shareCountTotalRepository.findFirstByOrderByIdDesc()
      .orElseThrow(() -> new RuntimeException("Osakkeiden kokonaismäärää ei löydy"));
  }
  
  public Integer addTotalShareCount(Integer shareCount) {
    Optional<ShareCountTotal> oldCountOptional = shareCountTotalRepository.findFirstByOrderByIdDesc();
    if (oldCountOptional.isPresent()) {
      ShareCountTotal oldTotal = oldCountOptional.get();
      ShareCountTotal newTotal = new ShareCountTotal(oldTotal.getTotalShares() + shareCount);
      shareCountTotalRepository.save(newTotal);
      return newTotal.getTotalShares();
    } else {
      ShareCountTotal newTotal = new ShareCountTotal(0);
      shareCountTotalRepository.save(newTotal);
      return newTotal.getTotalShares();
    }
  }
}
