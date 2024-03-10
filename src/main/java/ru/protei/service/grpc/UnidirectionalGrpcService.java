package ru.protei.service.grpc;

import io.grpc.stub.StreamObserver;
import ru.protei.grpc.PersonRequest;
import ru.protei.grpc.PersonResponse;
import ru.protei.grpc.UnidirectionalCommentServiceGrpc;
import ru.protei.service.PersonService;

public class UnidirectionalGrpcService extends UnidirectionalCommentServiceGrpc.UnidirectionalCommentServiceImplBase {
    private final PersonService personService = PersonService.getInstance();

    @Override
    public void getPerson(PersonRequest request, StreamObserver<PersonResponse> responseObserver) {
        for (int i = 0; i < 1000; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            responseObserver.onNext(ru.protei.grpc.PersonResponse.newBuilder().setPerson(personService.getPerson(request.getId())).build());
        }

        responseObserver.onCompleted();
    }
}
