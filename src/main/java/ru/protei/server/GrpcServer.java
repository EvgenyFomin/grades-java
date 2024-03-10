package ru.protei.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import ru.protei.service.grpc.BidirectionalGrpcService;
import ru.protei.service.grpc.SingleGrpcService;
import ru.protei.service.grpc.UnidirectionalGrpcService;

import java.io.IOException;

public class GrpcServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(8080)
                .addService(new SingleGrpcService())
                .addService(new UnidirectionalGrpcService())
                .addService(new BidirectionalGrpcService())
                .build();
        server.start();
        server.awaitTermination();
    }
}
