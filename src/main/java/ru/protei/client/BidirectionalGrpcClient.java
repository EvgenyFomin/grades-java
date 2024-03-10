package ru.protei.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BidirectionalGrpcClient {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:8080").usePlaintext().build();

        ru.protei.grpc.BidirectionalCommentServiceGrpc.BidirectionalCommentServiceStub stub =
                ru.protei.grpc.BidirectionalCommentServiceGrpc.newStub(channel);

        StreamObserver<ru.protei.grpc.PersonSubscribeRequest> observer = stub.subscribe(new StreamObserver<ru.protei.grpc.PersonSubscribeResponse>() {
            @Override
            public void onNext(ru.protei.grpc.PersonSubscribeResponse personSubscribeResponse) {
                System.out.println(personSubscribeResponse.getPeopleList());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("ERROR CLIENT");
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Client COMPLETED");
            }
        });

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);

        executorService.schedule(() -> observer.onNext(ru.protei.grpc.PersonSubscribeRequest.newBuilder().addPersonIds(1L).build()), 1L, TimeUnit.SECONDS);
        executorService.schedule(() -> observer.onNext(ru.protei.grpc.PersonSubscribeRequest.newBuilder().addAllPersonIds(Arrays.asList(1L, 2L)).build()), 5L, TimeUnit.SECONDS);
        executorService.schedule(() -> observer.onNext(ru.protei.grpc.PersonSubscribeRequest.newBuilder().addPersonIds(2L).build()), 10L, TimeUnit.SECONDS);
        executorService.schedule(() -> {
            observer.onCompleted();
            channel.shutdown();
            executorService.shutdown();
        }, 15L, TimeUnit.SECONDS);
    }
}
