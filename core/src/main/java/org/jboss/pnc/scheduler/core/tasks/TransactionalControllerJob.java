package org.jboss.pnc.scheduler.core.tasks;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

public abstract class TransactionalControllerJob extends ControllerJob {

    protected final TransactionManager tm;

    private boolean alreadyInTransaction;

    TransactionalControllerJob(TransactionManager tm) {
        this.tm = tm;
        try {
            this.alreadyInTransaction = tm.getTransaction()==null ? false : true;
        } catch (SystemException e) {
            throw new IllegalStateException("Cannot start Transaction, unexpected error was thrown", e);
        }
    }

    @Override
    void beforeExecute() {
        if (!alreadyInTransaction) {
            try {
                tm.begin();
            } catch (NotSupportedException | SystemException e) {
                //should not happen, thrown when attempting f.e. nested transaction
                throw new IllegalStateException("Cannot start Transaction, unexpected error was thrown", e);
            }
        }
    }

    @Override
    void onException(Throwable e) {
        try {
            //transaction initiator should handle the rollback
            if (!alreadyInTransaction) tm.rollback();
        } catch (SystemException ex) {
            throw new IllegalStateException("Cannot rollback Transaction, unexpected error was thrown", ex);
        }
    }

    @Override
    void afterExecute() {
        if (!alreadyInTransaction) {
            try {
                tm.commit();
            } catch (RollbackException e) {
                throw new IllegalStateException("Transaction rolled back!", e);
            } catch (SystemException | HeuristicRollbackException | HeuristicMixedException e) {
                //should not happen, thrown when attempting f.e. nested transaction
                throw new IllegalStateException("Unexpected error was thrown while committing transaction", e);
            }
        }
    }


}