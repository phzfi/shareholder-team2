package com.example.shareholder.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import java.util.Optional;
import com.example.shareholder.model.SharePrice;
import com.example.shareholder.repository.SharePriceRepository;

@Service
public class SharePriceService {

  private final SharePriceRepository sharePriceRepository;
  private final SharePriceDifferenceCalculator sharePriceDifferenceCalculator;

  public SharePriceService(SharePriceRepository sharePriceRepository,
                            SharePriceDifferenceCalculator sharePriceDifferenceCalculator) {
    this.sharePriceRepository = sharePriceRepository;
    this.sharePriceDifferenceCalculator = sharePriceDifferenceCalculator;
  }

  public Iterable<SharePrice> getAllSharePrices() {
    return sharePriceRepository.findAll();
  }

  public SharePrice getLatestPrice() {
    Optional<SharePrice> sharePriceOptional = sharePriceRepository.findFirstByOrderByIdDesc();
    if (sharePriceOptional.isPresent()) {
      SharePrice shareprice = sharePriceOptional.get();
      return shareprice;
    } else {
      return new SharePrice();
    }
  }

  public Map<Integer, BigDecimal> getAveragePricePerYear() {
    List<Object[]> results = sharePriceRepository.findAveragePricePerYear();
    Map<Integer, BigDecimal> averagePrices = new HashMap<>();

    for (Object[] result : results) {
      Integer year = (Integer) result[0];
      Double avgPriceAsDouble = (Double) result[1]; // Cast to Double
      
      // Convert Double to BigDecimal
      BigDecimal avgPrice = BigDecimal.valueOf(avgPriceAsDouble);

      averagePrices.put(year, avgPrice);
    }

    return averagePrices;
  }

  public SharePrice getSharePrice(Long id) {
    return sharePriceRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Osakkeen hintaa ei löytynyt id:llä " + id));
  }

  public SharePrice addSharePrice(SharePrice sharePrice) {
    BigDecimal difference = sharePriceDifferenceCalculator.calculateDifference(sharePrice);
    sharePrice.setDifference(difference);
    return sharePriceRepository.save(sharePrice);
  }

  public SharePrice updateSharePrice(Long id, SharePrice newSharePrice) {
    SharePrice existingSharePrice = sharePriceRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Osakkeen hintaa ei löytynyt id:llä " + id));

    existingSharePrice.setPrice(newSharePrice.getPrice());

    // Calculate difference
    BigDecimal difference = sharePriceDifferenceCalculator.calculateDifference(newSharePrice);
    existingSharePrice.setDifference(difference);

    // Update date
    existingSharePrice.setDate(LocalDate.now());

    return sharePriceRepository.save(existingSharePrice);
  }

  public void deleteSharePrice(Long id) {
    sharePriceRepository.deleteById(id);
  }
}
