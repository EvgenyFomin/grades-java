package ru.protei.service.grpc;

import io.grpc.stub.StreamObserver;
import ru.protei.grpc.BidirectionalCommentServiceGrpc;
import ru.protei.grpc.PersonSubscribeRequest;
import ru.protei.service.PersonService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class BidirectionalGrpcService extends BidirectionalCommentServiceGrpc.BidirectionalCommentServiceImplBase {
    private final PersonService personService = PersonService.getInstance();
    private final Map<StreamObserver<ru.protei.grpc.PersonSubscribeResponse>, ScheduledFuture<?>> obsToTask = new HashMap<>();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);

    @Override
    public StreamObserver<PersonSubscribeRequest> subscribe(StreamObserver<ru.protei.grpc.PersonSubscribeResponse> responseObserver) {
        return new StreamObserver<PersonSubscribeRequest>() {
            @Override
            public void onNext(PersonSubscribeRequest personSubscribeRequest) {
                cleanTask(responseObserver);
                obsToTask.put(responseObserver, executorService.scheduleWithFixedDelay(() -> {
                    responseObserver.onNext(
                            ru.protei.grpc.PersonSubscribeResponse.newBuilder()
                                    .addAllPeople(personService.getPeople(personSubscribeRequest.getPersonIdsList()))
                                    .build()
                    );
                }, 0, 1, TimeUnit.SECONDS));
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("ERROR SERVER");
                cleanTask(responseObserver);
            }

            @Override
            public void onCompleted() {
                System.out.println("Service COMPLETED");
                responseObserver.onCompleted();
                cleanTask(responseObserver);
            }
        };
    }

    private void cleanTask(StreamObserver<ru.protei.grpc.PersonSubscribeResponse> responseObserver) {
        ScheduledFuture<?> scheduledFuture = obsToTask.get(responseObserver);

        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }

        obsToTask.remove(responseObserver);
    }
}
