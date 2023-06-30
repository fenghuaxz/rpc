package io.rpc.aop;

final class Node {

    Node prev, next;
    final Aspect aspect;

    Node(Aspect aspect) {
        this.aspect = aspect;
    }
}
