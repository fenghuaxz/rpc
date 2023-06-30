package io.rpc.aop;

import java.util.Iterator;
import java.util.List;

final class DefaultPipe implements Pipe {

    private Node head, tail;

    public DefaultPipe(List<Object> aspects, boolean removeIfAdded) {
        aspects.stream().filter(obj -> obj instanceof Aspect)
                .map(obj -> (Aspect) obj)
                .peek(this::addLast)
                .filter(obj -> removeIfAdded)
                .forEach(aspects::remove);
    }


    @Override
    public Node head() {
        return this.head;
    }

    private void addLast(Aspect aspect) {
        Node node = new Node(aspect);
        if (head == null)
            head = node;
        else {
            tail.next = node;
            node.prev = tail;
        }
        tail = node;
    }
}
