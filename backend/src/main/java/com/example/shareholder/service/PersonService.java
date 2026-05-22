package com.example.shareholder.service;

import org.springframework.stereotype.Service;

import java.util.List;

import com.example.shareholder.model.Person;
import com.example.shareholder.repository.PersonRepository;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final ShareOwnershipService shareOwnershipService;
    private final ShareCountTotalService shareCountTotalService;
    private final OwnerPercentageCalculator ownerPercentageCalculator;

    public PersonService(PersonRepository personRepository,
                         ShareOwnershipService shareOwnershipService,
                         ShareCountTotalService shareCountTotalService,
                         OwnerPercentageCalculator ownerPercentageCalculator) {
        this.personRepository = personRepository;
        this.shareOwnershipService = shareOwnershipService;
        this.shareCountTotalService = shareCountTotalService;
        this.ownerPercentageCalculator = ownerPercentageCalculator;
    }

    public List<Person> getPersons() {
        return personRepository.findAll();
    }

    public Person getPersonById(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Henkilöä ei löytynyt id:llä " + id));
    }

    public List<Object[]> getTop5ShareholdersAndRest() {
        return personRepository.findTop5ShareholdersAndRest();
    }

    public Person addPerson(Person person) {
        if (person.getFirstname() == null || person.getLastname() == null || person.getEmail() == null
                || person.getPhone() == null || person.getAddress() == null
                || person.getPostalCode() == null || person.getCity() == null || person.getBankAccount() == null) {
            throw new IllegalArgumentException("Kentät ovat pakollisia.");
        }
        if (personRepository.findBySsn(person.getSsn()) != null) {
            throw new IllegalArgumentException("Palvelusta löytyy jo samalla henkilötunnuksella oleva henkilö.");
        }
        if (person.getNumberOfShares() == null || person.getNumberOfShares() < 0) {
            throw new IllegalArgumentException("Osakemäärän on oltava nolla tai suurempi.");
        }
        Person newPerson = personRepository.save(person);
        shareCountTotalService.addTotalShareCount(person.getNumberOfShares());

        if (person.getNumberOfShares() > 0) {
            shareOwnershipService.addShareOwnership(person);
            ownerPercentageCalculator.updateAllOwnershipPercentages();
        }
        return newPerson;
    }

    public Person updatePerson(Long id, Person person) {
        Person existingPerson = personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Henkilöä ei löytynyt " + id));

        Integer previousShares = existingPerson.getNumberOfShares();

        existingPerson.setFirstname(person.getFirstname());
        existingPerson.setLastname(person.getLastname());
        existingPerson.setEmail(person.getEmail());
        existingPerson.setPhone(person.getPhone());
        existingPerson.setAddress(person.getAddress());
        existingPerson.setPostalCode(person.getPostalCode());
        existingPerson.setCity(person.getCity());
        existingPerson.setNumberOfShares(person.getNumberOfShares());
        existingPerson.setBankAccount(person.getBankAccount());

        if (personRepository.findBySsn(person.getSsn()) != null) {
            throw new IllegalArgumentException("Palvelusta löytyy jo samalla henkilötunnuksella oleva henkilö.");
        } else {
            existingPerson.setSsn(person.getSsn());
        }

        Person newPerson = personRepository.save(existingPerson);

        if (!previousShares.equals(person.getNumberOfShares())) {
            ownerPercentageCalculator.updateAllOwnershipPercentages();
        }
        shareCountTotalService.addTotalShareCount(person.getNumberOfShares());
        return newPerson;
    }

    public void deletePerson(Long id) {
        Person personToDelete = personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Henkilöä ei löytynyt id:llä " + id));
        personRepository.deleteById(personToDelete.getId());
    }
}