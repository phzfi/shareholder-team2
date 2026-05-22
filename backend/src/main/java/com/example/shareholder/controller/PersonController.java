package com.example.shareholder.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.example.shareholder.model.Person;
import com.example.shareholder.service.PersonService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/persons")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping
    public ResponseEntity<List<Person>> getPersons() {
        List<Person> persons = personService.getPersons();
        return ResponseEntity.ok().body(persons);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Person> getPersonById(@PathVariable Long id) {
        Person person = personService.getPersonById(id);
        return ResponseEntity.ok().body(person);
    }

    @GetMapping("/top5")
    public List<Object[]> getTop5Shareholders() {
        return personService.getTop5ShareholdersAndRest();
    }

    @PostMapping("/add")
    public ResponseEntity<Person> addPerson(@RequestBody Person person) {
        Person newPerson = personService.addPerson(person);
        return ResponseEntity.ok().body(newPerson);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Person> updatePerson(@PathVariable Long id, @RequestBody Person person) {
        Person updatedPerson = personService.updatePerson(id, person);
        return ResponseEntity.ok().body(updatedPerson);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePerson(@PathVariable Long id) {
        personService.deletePerson(id);
        return ResponseEntity.ok().body("Henkilö poistettu onnistuneesti");
    }
}
