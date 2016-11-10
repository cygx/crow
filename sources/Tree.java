import java.util.*;
import java.lang.reflect.Array;

public class Tree<K, V> {
    public static class Path<K> implements Iterable<K> {
        private K[] keys;

        @SafeVarargs
        public Path(K... keys) {
            this.keys = keys;
        }

        public void shift(int n) {
            keys = Arrays.copyOfRange(keys, n, keys.length);
        }

        public void clear() {
            @SuppressWarnings("unchecked")
            K[] k = (K[])Array.newInstance(keys.getClass().getComponentType(), 0);
            this.keys = k;
        }

        public Path<K> clone() {
            return new Path<K>(keys);
        }

        public K key(int i) {
            return keys[i];
        }

        public K[] keys() {
            return keys;
        }

        public int length() {
            return keys.length;
        }

        public boolean isEmpty() {
            return keys.length == 0;
        }

        public Iterator<K> iterator() {
            return Arrays.asList(keys).iterator();
        }
    }

    public static class Node<K, V> {
        public final K key;
        public V value;

        Node<K, V> nextSibling;
        Node<K, V> firstChild;

        Node(K key) {
            this.key = key;
        }

        Node<K, V> spawn(Path<K> path) {
            Node<K, V> sibling = firstChild;

            Node<K, V> node = this;
            for(K key : path) {
                Node<K, V> child = new Node<K, V>(key);
                node.firstChild = child;
                node = child;
            }

            firstChild.nextSibling = sibling;
            return node;
        }

        public void aggregateLeafValues(List<V> values) {
            if(firstChild == null) values.add(value);
            else for(Node<K, V> child = firstChild; child != null;
                    child = child.nextSibling) {
                child.aggregateLeafValues(values);
            }
        }
    }

    private Node<K, V> root = new Node<K, V>(null);

    public Node<K, V> getNode(Path<K> path) {
        Path<K> clone = path.clone();
        Node<K, V> node = consume(clone);
        return clone.isEmpty() ? node : null;
    }

    @SafeVarargs
    public final V get(K... keys) throws NoSuchElementException {
        Node<K, V> node = getNode(new Path<K>(keys));
        return node != null ? node.value : null;
    }

    public void put(V value) {
        root.value = value;
    }

    private void putImpl(V value, Path<K> path) {
        Node<K, V> node = consume(path);
        node.spawn(path).value = value;
    }

    @SafeVarargs
    public final void put(V value, K... keys) {
        putImpl(value, new Path<K>(keys));
    }

    public void put(V value, Path<K> path) {
        putImpl(value, path.clone());
    }

    @SafeVarargs
    public final Path<K> path(K... keys) {
        return new Path<K>(keys);
    }

    public Node<K, V> consume(Path<K> path) {
        if(path.isEmpty()) return root;

        int i = 0;
        Node<K, V> parent = root, child = parent.firstChild;
        while(child != null) {
            K key = path.key(i);
            if(key == null ? child.key == null : key.equals(child.key)) {
                if(++i == path.length()) {
                    path.clear();
                    return child;
                }
                else {
                    parent = child;
                    child = parent.firstChild;
                }
            }
            else child = child.nextSibling;
        }

        path.shift(i);
        return parent;
    }
}
