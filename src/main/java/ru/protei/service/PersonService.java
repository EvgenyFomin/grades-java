package ru.protei.service;

import ru.protei.grpc.Person;

import java.util.*;
import java.util.stream.Collectors;

public class PersonService {
    private final Map<Long, ru.protei.grpc.Person> personIdToPerson = new HashMap<>();
    private long commentId = 1;

    private PersonService() {}

    public static PersonService getInstance() {
        return new PersonService();
    }

    public ru.protei.grpc.Person getPerson(long personId) {
        return generatePerson(personId);
    }

    public List<ru.protei.grpc.Person> getPeople(List<Long> personIds) {
        return personIds.stream().map(this::generatePerson).collect(Collectors.toList());
    }

    private ru.protei.grpc.Person generatePerson(long personId) {
        if (!personIdToPerson.containsKey(personId)) {
            Person person = Person.getDefaultInstance().newBuilderForType()
                    .setId(personId)
                    .setName(UUID.randomUUID().toString())
                    .addAllComments(getComments())
                    .build();

            personIdToPerson.put(personId, person);

            return person;
        }

        ru.protei.grpc.Person person = personIdToPerson.get(personId).toBuilder().addComments(generateComment()).build();

        personIdToPerson.put(personId, person);

        return personIdToPerson.get(personId);
    }

    private List<ru.protei.grpc.Comment> getComments() {
        List<ru.protei.grpc.Comment> comments = new ArrayList<>();

        for (int i = 0; i < new Random().nextInt(10); i++) {
            comments.add(generateComment());
        }

        return comments;
    }

    private ru.protei.grpc.Comment generateComment() {
        return ru.protei.grpc.Comment.getDefaultInstance().newBuilderForType()
                .setId(commentId++)
                .setComment(UUID.randomUUID().toString())
                .build();
    }
}
