package org.jboss.pnc.scheduler.core.tasks;

import org.jboss.pnc.scheduler.core.api.Dependent;

import javax.enterprise.inject.spi.CDI;
import javax.transaction.TransactionManager;
import java.util.Set;

public class DependencyStoppedJob extends DependentControllerJob {
    Dependent dependentAPI;
    
    public DependencyStoppedJob(Set<String> dependents) {
        super(dependents);
        this.dependentAPI = CDI.current().select(Dependent.class).get();
    }

    @Override
    void inform(String dependentName) {
        dependentAPI.dependencyStopped(dependentName);
    }
}
