package ru.protei;

import akka.actor.*;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class App {
    static class MyErrorException extends RuntimeException {
        public MyErrorException(String message) {
            super(message);
        }
    }
    static class EntitySaver extends AbstractLoggingActor {
        private final EntityDAO entityDAO;
        private final ActorRef historySaver;
        private final ActorRef wsSender;
        private int counter = 0;
        private ActorRef child = getContext().actorOf(HistorySaver.props(new HistoryDAO()), "child");

        public EntitySaver(EntityDAO entityDAO, ActorRef historySaver, ActorRef wsSender) {
            this.entityDAO = entityDAO;
            this.historySaver = historySaver;
            this.wsSender = wsSender;
        }

        static class Save {
            private final Entity entity;
            public Save(Entity entity) {
                this.entity = entity;
            }
        }

        @Override
        public Receive createReceive() {
            return ReceiveBuilder.create()
                    .match(Save.class, this::onSave)
                    .build();
        }

        @Override
        public SupervisorStrategy supervisorStrategy() {
            return new OneForOneStrategy(
                    DeciderBuilder.match(MyErrorException.class, ex -> SupervisorStrategy.escalate()).build()
            );
        }

        private void onSave(Save save) throws InterruptedException {
            log().info("EntitySaver: onSave {}", save.entity);
//            log().info("EntitySaver: counter {}", counter++);
//            if (counter == 5) {
//                log().info("EntitySaver: sleep");
//                TimeUnit.HOURS.sleep(3);
//            }
            Entity oldEntity = entityDAO.get(save.entity.getId());
            entityDAO.persist(save.entity);
            Entity newEntity = entityDAO.get(save.entity.getId());
//            historySaver.tell(new HistorySaver.Save(oldEntity, newEntity), self());
//            wsSender.tell(new WsSender.Send("Entity with id = " + newEntity.getId() + " has been saved."), self());
            child.tell(new HistorySaver.Save(oldEntity, newEntity), self());
        }

        public static Props props(EntityDAO entityDAO, ActorRef historySaver, ActorRef wsSender) {
            return Props.create(EntitySaver.class, () -> new EntitySaver(entityDAO, historySaver, wsSender));
        }
    }
    static class HistorySaver extends AbstractLoggingActor {
        private final List<History> cache = new ArrayList<>();
        private final HistoryDAO historyDAO;
        private int counter = 1;

        public HistorySaver(HistoryDAO historyDAO) {
            this.historyDAO = historyDAO;
        }

        public static class Save {
            private final Entity oldEntity;
            private final Entity newEntity;

            public Save(Entity oldEntity, Entity newEntity) {
                this.oldEntity = oldEntity;
                this.newEntity = newEntity;
            }
        }

        @Override
        public Receive createReceive() {
            return ReceiveBuilder.create()
                    .match(Save.class, this::onSave)
                    .build();
        }

        private void onSave(Save save) throws InterruptedException {
            log().info("HistorySaver: saving");

            if (save.oldEntity == null && save.newEntity == null) {
                log().warning("HistorySaver: old and new entity are null");
                return;
            }

            log().info("HistorySaver: counter {}, entityId {}", counter, save.newEntity.getId());
            if (counter++ % 3 == 0) {
                throw new MyErrorException("my-exception");
            }

            if (save.oldEntity == null) {
                cache.add(new History(save.newEntity.getId(), null, save.newEntity.getName()));
                cache.add(new History(save.newEntity.getId(), null, String.valueOf(save.newEntity.getWeight())));
                cache.add(new History(save.newEntity.getId(), null, String.valueOf(save.newEntity.getLength())));
            } else if (save.newEntity == null) {
                cache.add(new History(save.oldEntity.getId(), save.oldEntity.getName(), null));
                cache.add(new History(save.oldEntity.getId(), String.valueOf(save.oldEntity.getWeight()), null));
                cache.add(new History(save.oldEntity.getId(), String.valueOf(save.oldEntity.getLength()), null));
            } else {
                if (!Objects.equals(save.oldEntity, save.newEntity)) {
                    cache.add(new History(save.newEntity.getId(), save.newEntity.getName(), save.newEntity.getName()));
                }
                if (save.oldEntity.getLength() != save.newEntity.getLength()) {
                    cache.add(new History(save.newEntity.getId(), String.valueOf(save.newEntity.getLength()), String.valueOf(save.newEntity.getLength())));
                }
                if (save.oldEntity.getWeight() != save.newEntity.getWeight()) {
                    cache.add(new History(save.newEntity.getId(), String.valueOf(save.newEntity.getWeight()), String.valueOf(save.newEntity.getWeight())));
                }
            }

            if (cache.size() > 7) {
                historyDAO.persistBatch(cache);
                cache.clear();
            }
        }

        public static Props props(HistoryDAO historyDAO) {
            return Props.create(HistorySaver.class, () -> new HistorySaver(historyDAO));
        }
    }
    static class WsSender extends AbstractLoggingActor {
        private final Map<String, Recipient> recipients;

        public WsSender(List<Recipient> recipients) {
            this.recipients = recipients.stream().collect(Collectors.toMap(Recipient::getToken, Function.identity()));
        }

        public static class AddRecipient {
            private final Recipient recipient;
            public AddRecipient(Recipient recipient) {
                this.recipient = recipient;
            }
        }
        public static class RemoveRecipient {
            private final String token;
            public RemoveRecipient(String token) {
                this.token = token;
            }
        }
        public static class Send {
            private final String message;
            public Send(String message) {
                this.message = message;
            }
        }

        @Override
        public Receive createReceive() {
            return ReceiveBuilder.create()
                    .match(AddRecipient.class, this::onAddRecipient)
                    .match(RemoveRecipient.class, this::onRemoveRecipient)
                    .match(Send.class, this::onSend)
                    .build();
        }

        private void onAddRecipient(AddRecipient addRecipient) {
            log().info("Thread: {}", Thread.currentThread().getName());
            log().info("WsSender: onAddRecipient {}", addRecipient.recipient);
            recipients.put(addRecipient.recipient.getToken(), addRecipient.recipient);
        }

        private void onRemoveRecipient(RemoveRecipient removeRecipient) {
            log().info("WsSender: onRemoveRecipient {}", removeRecipient.token);
            recipients.remove(removeRecipient.token);
        }

        private void onSend(Send send) throws InterruptedException {
            log().info("WsSender: onSend");
            recipients.values().forEach(recipient -> recipient.send(send.message));
        }

        public static Props props(List<Recipient> recipients) {
            return Props.create(WsSender.class, () -> new WsSender(recipients));
        }
    }

    public static void main(String[] args) {
        ActorSystem actorSystem = ActorSystem.create("sample");
        ActorRef wsSender = actorSystem.actorOf(WsSender.props(new ArrayList<>(List.of(new Recipient("token-1")))).withDispatcher("akkasample.some-dispatcher"), "wsSender");
        wsSender.tell(new WsSender.AddRecipient(new Recipient("token-" + new Random().nextInt(10))), ActorRef.noSender());
        ActorRef historySaver = actorSystem.actorOf(HistorySaver.props(new HistoryDAO()), "historySaver");
        ActorRef entitySaver = actorSystem.actorOf(EntitySaver.props(new EntityDAO(), historySaver, wsSender), "entitySaver");

//        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
//            wsSender.tell(new WsSender.AddRecipient(new Recipient("token-" + new Random().nextInt(10))), ActorRef.noSender());
//        }, 1, 2, TimeUnit.SECONDS);
//
//        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
//            wsSender.tell(new WsSender.RemoveRecipient("token-" + new Random().nextInt(10)), ActorRef.noSender());
//        }, 1, 2, TimeUnit.SECONDS);

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            System.out.println("ENTITY TELL");
            entitySaver.tell(new EntitySaver.Save(new Entity(
                    new Random().nextLong(10),
                    UUID.randomUUID().toString(),
                    new Random().nextInt(10),
                    new Random().nextInt(10)
            )), ActorRef.noSender());
        }, 1, 1, TimeUnit.SECONDS);
    }
}
