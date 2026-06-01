package org.javaspec.doubles;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

final class DoubleInvocationHandler implements InvocationHandler {
    private final Class<?> interfaceType;
    private final int id;
    private final List<Call> calls;
    private final List<StubbedInvocation> stubs;

    DoubleInvocationHandler(Class<?> interfaceType, int id) {
        this.interfaceType = interfaceType;
        this.id = id;
        this.calls = new ArrayList<Call>();
        this.stubs = new ArrayList<StubbedInvocation>();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
        Object[] safeArguments = arguments == null ? new Object[0] : arguments;
        if (isObjectMethod(method)) {
            return invokeObjectMethod(proxy, method, safeArguments);
        }

        StubbedInvocation stub;
        synchronized (this) {
            calls.add(new Call(method.getName(), safeArguments));
            stub = findStub(method.getName(), safeArguments);
        }
        if (stub == null) {
            return ReturnValues.valueFor(method, null, false);
        }
        return ReturnValues.valueFor(method, stub.returnValue(), true);
    }

    synchronized void addStub(MethodPattern pattern, Object returnValue) {
        stubs.add(new StubbedInvocation(pattern, returnValue));
    }

    synchronized int count(MethodPattern pattern) {
        int count = 0;
        for (int i = 0; i < calls.size(); i++) {
            if (pattern.matches(calls.get(i))) {
                count++;
            }
        }
        return count;
    }

    synchronized List<Call> calls() {
        return new ArrayList<Call>(calls);
    }

    synchronized List<Call> calls(MethodPattern pattern) {
        List<Call> matchingCalls = new ArrayList<Call>();
        for (int i = 0; i < calls.size(); i++) {
            Call call = calls.get(i);
            if (pattern.matches(call)) {
                matchingCalls.add(call);
            }
        }
        return matchingCalls;
    }

    synchronized void clearCalls() {
        calls.clear();
    }

    synchronized void clearStubs() {
        stubs.clear();
    }

    String description() {
        return "Double<" + interfaceType.getName() + ">#" + id;
    }

    private StubbedInvocation findStub(String methodName, Object[] arguments) {
        for (int i = stubs.size() - 1; i >= 0; i--) {
            StubbedInvocation stub = stubs.get(i);
            if (stub.exactArguments() && stub.matches(methodName, arguments)) {
                return stub;
            }
        }
        for (int i = stubs.size() - 1; i >= 0; i--) {
            StubbedInvocation stub = stubs.get(i);
            if (!stub.exactArguments() && stub.matches(methodName, arguments)) {
                return stub;
            }
        }
        return null;
    }

    private boolean isObjectMethod(Method method) {
        String name = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if ("toString".equals(name) && parameterTypes.length == 0) {
            return true;
        }
        if ("hashCode".equals(name) && parameterTypes.length == 0) {
            return true;
        }
        return "equals".equals(name)
                && parameterTypes.length == 1
                && Object.class.equals(parameterTypes[0]);
    }

    private Object invokeObjectMethod(Object proxy, Method method, Object[] arguments) {
        String name = method.getName();
        if ("toString".equals(name)) {
            return description();
        }
        if ("hashCode".equals(name)) {
            return Integer.valueOf(id);
        }
        if ("equals".equals(name)) {
            return Boolean.valueOf(arguments.length == 1 && proxy == arguments[0]);
        }
        return null;
    }
}
