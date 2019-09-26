package com.liveaction.reactiff.server.internal.context;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.liveaction.reactiff.server.context.ExecutionContext;
import com.liveaction.reactiff.server.context.ExecutionContextService;

import java.util.List;
import java.util.stream.Collectors;

public final class ExecutionContextServiceManager implements ExecutionContextService {

    private final List<ExecutionContextService> executionContextServices = Lists.newCopyOnWriteArrayList();

    public void addExecutionContextService(ExecutionContextService executionContextService) {
        this.executionContextServices.add(executionContextService);
    }

    public void removeExecutionContextService(ExecutionContextService executionContextService) {
        this.executionContextServices.remove(executionContextService);
    }

    @Override
    public ExecutionContext prepare() {
        ImmutableList<ExecutionContext> executionContexts = ImmutableList.copyOf(executionContextServices.stream()
                .map(ExecutionContextService::prepare)
                .collect(Collectors.toList()));
        return () -> executionContexts.forEach(ExecutionContext::apply);
    }
}
