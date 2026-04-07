package ru.protei;

import java.util.Objects;

import static ru.protei.Result.error;
import static ru.protei.Result.ok;

public class Main {
    public static void main(String[] args) {
        Result<Long> result = new Chain<Delivery, Long>()
                .addLink(new PrivilegeChainLink(
                        new Chain<Delivery, Long>()
                                .addLink(new HasOnePrivilege())
                                .addLink(new HasTwoPrivilege())
                                .addLink(new HasThreePrivilege())
                ))
                .addLink(new IdIsNotNull())
                .addLink(new NameIsNotNull())
                .addLink(new DeliveryHistoryChainLink(new Chain<>()))
                .addLink(new SavingChainLink())
                .addLink(new ResultChainLink())
                .start(new Delivery(2L, null));

        System.out.println(result);
    }

    public static class PrivilegeChainLink implements ChainLink<Delivery, Long> {
        private final Chain<Delivery, Long> privilegesChain;

        public PrivilegeChainLink(Chain<Delivery, Long> privilegesChain) {
            this.privilegesChain = privilegesChain;
        }

        @Override
        public void doAction(Delivery delivery, Result<Long> r, ChainStep<Delivery, Long> chain) {
            Result<Long> innerResult = privilegesChain.start(delivery, error(En_ResultStatus.PERMISSION_DENIED));

            if (innerResult.isOk()) {
                chain.doNext(delivery, r);
            } else {
                chain.doComplete(innerResult);
            }
        }
    }

    public static class HasOnePrivilege implements ChainLink<Delivery, Long> {
        @Override
        public void doAction(Delivery delivery, Result<Long> r, ChainStep<Delivery, Long> chain) {
            if (Objects.equals(1L, delivery.getId())) {
                chain.doComplete(ok());
            } else {
                chain.doNext(delivery, r);
            }
        }
    }

    public static class HasTwoPrivilege implements ChainLink<Delivery, Long> {
        @Override
        public void doAction(Delivery delivery, Result<Long> r, ChainStep<Delivery, Long> chain) {
            if (Objects.equals(2L, delivery.getId())) {
                chain.doComplete(ok());
            } else {
                chain.doNext(delivery, r);
            }
        }
    }

    public static class HasThreePrivilege implements ChainLink<Delivery, Long> {
        @Override
        public void doAction(Delivery delivery, Result<Long> r, ChainStep<Delivery, Long> chain) {
            if (Objects.equals(3L, delivery.getId())) {
                chain.doComplete(ok());
            } else {
                chain.doNext(delivery, r);
            }
        }
    }

    public static class IdIsNotNull implements ChainLink<Delivery, Long> {
        @Override
        public void doAction(Delivery delivery, Result<Long> r, ChainStep<Delivery, Long> chain) {
            if (delivery.getId() == null) {
                chain.doComplete(error(En_ResultStatus.ID_IS_NULL));
            } else {
                chain.doNext(delivery, r);
            }
        }
    }

    public static class NameIsNotNull extends AutoChainLink<Delivery, Long> {
        @Override
        public void doAutoAction(Delivery delivery, Result<Long> r, ChainStep<Delivery, Long> chain) {
            if (delivery.getName() == null) {
                chain.doComplete(error(En_ResultStatus.NAME_IS_NULL));
            }
        }
    }

    public static class DeliveryHistoryChainLink implements ChainLink<Delivery, Long> {
        private final Chain<DiffEntity<Delivery>, ?> chain;

        public DeliveryHistoryChainLink(Chain<DiffEntity<Delivery>, ?> chain) {
            this.chain = chain;
        }

        @Override
        public void doAction(Delivery delivery, Result<Long> r, ChainStep<Delivery, Long> chain) {
            Delivery dbDelivery = new Delivery(1L, "asdf");
            chain.doNext(delivery, r);

            if (!r.isOk()) {
                return;
            }

            Delivery newDelivery = new Delivery(2L, "asdvvv");

            DiffEntity<Delivery> diffEntity = new DiffEntity<>(dbDelivery, newDelivery);

            Result<?> result = this.chain.start(diffEntity);
        }
    }

    public static class SavingChainLink implements ChainLink<Delivery, Long> {
        @Override
        public void doAction(Delivery delivery, Result<Long> r, ChainStep<Delivery, Long> chain) {
            r.setData(delivery.getId());
            chain.doNext(delivery, r);
        }
    }

    public static class ResultChainLink implements ChainLink<Delivery, Long> {
        @Override
        public void doAction(Delivery delivery, Result<Long> r, ChainStep<Delivery, Long> chain) {
            r.setData(delivery.getId());
            chain.doNext(delivery, r);
        }
    }

    public static abstract class AutoChainLink<T, R> implements ChainLink<T, R> {
        private class AutoChainStep implements ChainStep<T, R> {
            private T t;
            private Result<R> r;
            private boolean completed;

            public AutoChainStep(T t, Result<R> r) {
                this.t = t;
                this.r = r;
            }

            @Override
            public void doNext(T t, Result<R> res) {
                this.t = t;
                this.r = res;
            }

            @Override
            public void doComplete(Result<R> err) {
                this.r = err;
                completed = true;
            }
        }

        @Override
        public void doAction(T t, Result<R> r, ChainStep<T, R> chain) {
            AutoChainStep autoChainStep = new AutoChainStep(t, r);
            doAutoAction(t, r, autoChainStep);

            if (autoChainStep.completed) {
                chain.doComplete(autoChainStep.r);
            } else {
                chain.doNext(autoChainStep.t, autoChainStep.r);
            }
        }

        protected abstract void doAutoAction(T t, Result<R> r, ChainStep<T, R> chain);
    }
}
