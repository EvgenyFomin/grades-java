package ru.protei.service.grpc;

import io.grpc.stub.StreamObserver;
import ru.protei.grpc.Person;
import ru.protei.grpc.PersonRequest;
import ru.protei.grpc.PersonResponse;
import ru.protei.grpc.SingleCommentServiceGrpc;
import ru.protei.service.PersonService;

public class SingleGrpcService extends SingleCommentServiceGrpc.SingleCommentServiceImplBase {
    private final PersonService personService = PersonService.getInstance();

    @Override
    public void getPerson(PersonRequest request, StreamObserver<PersonResponse> responseObserver) {
        Person person = personService.getPerson(request.getId());
        responseObserver.onNext(ru.protei.grpc.PersonResponse.newBuilder().setPerson(person).build());
        responseObserver.onCompleted();
    }
}
