package com.example.shareholder.service;

import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

import com.example.shareholder.model.Person;
import com.example.shareholder.repository.PersonRepository;

@Service
public class OwnerPercentageCalculator {

  private final PersonRepository personRepository;

  public OwnerPercentageCalculator(PersonRepository personRepository) {
    this.personRepository = personRepository;
  }

  public void updateAllOwnershipPercentages() {
    List<Person> allPersons = personRepository.findAll();
    int totalShares = allPersons.stream().mapToInt(Person::getNumberOfShares).sum();

    for (Person person : allPersons) {
      if (totalShares > 0) {
        BigDecimal newPercentage = BigDecimal.valueOf(person.getNumberOfShares())
            .divide(BigDecimal.valueOf(totalShares), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        person.setOwnershipPercentage(newPercentage);
        personRepository.save(person);
      } else {
        person.setOwnershipPercentage(BigDecimal.ZERO);
        personRepository.save(person);
      }
    }
  }

}
