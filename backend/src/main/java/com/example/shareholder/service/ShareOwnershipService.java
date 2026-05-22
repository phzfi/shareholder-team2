package com.example.shareholder.service;

import org.springframework.stereotype.Service;

import com.example.shareholder.model.Person;
import com.example.shareholder.model.ShareOwnership;
import com.example.shareholder.model.ShareTransaction;
import com.example.shareholder.repository.PersonRepository;
import com.example.shareholder.repository.ShareOwnershipRepository;

import java.util.List;

@Service
public class ShareOwnershipService {

  private final ShareOwnershipRepository shareOwnershipRepository;
  private final PersonRepository personRepository;

  public ShareOwnershipService(ShareOwnershipRepository shareOwnershipRepository,
                                PersonRepository personRepository) {
    this.shareOwnershipRepository = shareOwnershipRepository;
    this.personRepository = personRepository;
  }

  public List<ShareOwnership> getAllShareOwnerships() {
    return shareOwnershipRepository.findAll();
  }
  
  public ShareOwnership getShareOwnershipById(Long id) {
    return shareOwnershipRepository.findById(id).orElse(null);
  }

  public long getTotalShareOwnership() {
    return shareOwnershipRepository.count();
  }


  public ShareOwnership addShareOwnership(Person person) {
    if (person.getNumberOfShares() != 0) {
      ShareOwnership shareOwnership = new ShareOwnership();
      shareOwnership.setNumberOfShares(person.getNumberOfShares());
      shareOwnership.setOwner(person);
      shareOwnership.setShareClass("A");

      int startingShareNumber = shareOwnershipRepository.findMaxEndingShareNumber().orElse(0) + 1;
      int endingShareNumber = startingShareNumber + person.getNumberOfShares() - 1;

      shareOwnership.setStartingShareNumber(startingShareNumber);
      shareOwnership.setEndingShareNumber(endingShareNumber);

      return shareOwnershipRepository.save(shareOwnership);
    } else {
      return null;
    }
  }

  public ShareOwnership updateShareOwnership(ShareTransaction shareTransaction) {

    if (shareTransaction.getNumberOfShares() != 0) {
      Person buyer = shareTransaction.getBuyer();
      Person seller = shareTransaction.getSeller();

      if (!personRepository.findById(buyer.getId()).isPresent()) {
        throw new IllegalArgumentException("Ostajaa ei löydy annetulla ID:llä");
      }
      if (!personRepository.findById(seller.getId()).isPresent()) {
        throw new IllegalArgumentException("Myyjää ei löydy annetulla ID:llä");
      }

      List<ShareOwnership> sellerOwnerships = shareOwnershipRepository.findByOwnerId(seller.getId());

      if (sellerOwnerships.isEmpty()) {
        throw new IllegalArgumentException("Myyjällä ei ole osakkeita");
      }
      int totalSellerShares = sellerOwnerships.stream().mapToInt(ShareOwnership::getNumberOfShares).sum();

      if (totalSellerShares < shareTransaction.getNumberOfShares()) {
        throw new IllegalArgumentException("Myyjällä ei ole tarpeeksi osakkeita");
      }

      // Sort all the seller ownerships
      sellerOwnerships.sort((o1, o2) -> Integer.compare(o1.getNumberOfShares(), o2.getNumberOfShares()));

      ShareOwnership ownershipToSell = new ShareOwnership();
      int remainingSharesToSell = shareTransaction.getNumberOfShares();

      // Iterate through all the seller ownerships to find suitable ownerships to sell
      for (ShareOwnership ownership : sellerOwnerships) {
        if (remainingSharesToSell == 0) {
          break;
        }

        int availableShares = ownership.getNumberOfShares();

        if (availableShares <= remainingSharesToSell) {
          ownership.setOwner(buyer);
          shareOwnershipRepository.save(ownership);
          remainingSharesToSell -= availableShares;

        } else {
          ownershipToSell.setNumberOfShares(remainingSharesToSell);
          ownershipToSell.setOwner(buyer);
          ownershipToSell.setShareClass(ownership.getShareClass());
          ownershipToSell.setEndingShareNumber(ownership.getEndingShareNumber());
          ownershipToSell.setStartingShareNumber(ownership.getEndingShareNumber() - remainingSharesToSell + 1);

          ownership.setNumberOfShares(availableShares - remainingSharesToSell);
          ownership.setEndingShareNumber(ownership.getEndingShareNumber() - remainingSharesToSell);

          shareOwnershipRepository.save(ownership);
          shareOwnershipRepository.save(ownershipToSell);

          remainingSharesToSell = 0;
        }
      }

      if (remainingSharesToSell > 0) {
        throw new IllegalArgumentException("Myyjällä ei ollut tarpeeksi osakkeita myyntiin.");
      }

      // Finally update person shares
      seller.setNumberOfShares(totalSellerShares - shareTransaction.getNumberOfShares());
      buyer.setNumberOfShares(buyer.getNumberOfShares() + shareTransaction.getNumberOfShares());

      personRepository.save(seller);
      personRepository.save(buyer);

      return ownershipToSell;
    } else {
      return null;
    }
  }

}
