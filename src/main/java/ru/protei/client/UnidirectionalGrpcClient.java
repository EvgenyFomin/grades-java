package ru.protei.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import ru.protei.grpc.PersonRequest;
import ru.protei.grpc.PersonResponse;
import ru.protei.grpc.SingleCommentServiceGrpc;
import ru.protei.grpc.UnidirectionalCommentServiceGrpc;

import java.util.Iterator;

public class UnidirectionalGrpcClient {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:8080").usePlaintext().build();

        UnidirectionalCommentServiceGrpc.UnidirectionalCommentServiceBlockingStub stub =
                UnidirectionalCommentServiceGrpc.newBlockingStub(channel);

        Iterator<PersonResponse> responses = stub.getPerson(PersonRequest.newBuilder().setId(1L).build());

        while (responses.hasNext()) {
            System.out.println(responses.next().getPerson());
        }

        channel.shutdown();
    }
}
