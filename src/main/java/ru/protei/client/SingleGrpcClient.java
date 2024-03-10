package ru.protei.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import ru.protei.grpc.PersonRequest;
import ru.protei.grpc.PersonResponse;
import ru.protei.grpc.SingleCommentServiceGrpc;

public class SingleGrpcClient {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:8080").usePlaintext().build();

        SingleCommentServiceGrpc.SingleCommentServiceBlockingStub stub =
                SingleCommentServiceGrpc.newBlockingStub(channel);

        PersonResponse person = stub.getPerson(PersonRequest.newBuilder().setId(1L).build());

        System.out.println(person);

        channel.shutdown();
    }
}
