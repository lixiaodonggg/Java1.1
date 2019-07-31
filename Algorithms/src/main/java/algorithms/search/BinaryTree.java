package algorithms.search;

public class BinaryTree<K extends Comparable<K>, V> {
    private Node root;

    private class Node {  //节点的数据结构
        private K key; //键
        private V value;//值
        private Node left, right; //左节点和右节点
        private int N; //计数器,得到子节点和本身的总数

        public Node(K key, V value, int N) {
            this.key = key;
            this.value = value;
            this.N = N;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "key=" + key +
                    ", value=" + value +
                    ", N=" + N +
                    '}';
        }
    }

    public int size() { //节点的数量
        return size(root);
    }

    private int size(Node node) { //计算某个节点下的总的节点数
        if (node == null) {
            return 0;
        } else {
            return node.N;
        }
    }

    public V get(K key) {
        return get(root, key);
    }

    /**
     * 二叉树的查找，从根节点开始，
     * 将键与节点做比较，如果大于节点的键值，
     * 则与根节点的右节点比较，反之则左，一直找到或者节点为null
     */
    private V get(Node node, K key) {
        if (node == null) { //找不到，返回null
            return null;
        }
        int cmp = key.compareTo(node.key); //将键值与节点的键值作比较
        if (cmp < 0) {
            return get(node.left, key);  //如果小于节点的键值就与子节点的左节点比较
        } else if (cmp > 0) {
            return get(node.right, key);
        } else {
            return node.value;
        }
    }

    public void put(K key, V value) {
        root = put(root, key, value);
    }

    private Node put(Node node, K key, V value) {
        if (node == null) {
            return new Node(key, value, 1);
        }
        int cmp = key.compareTo(node.key); //将要插入的键值与节点比较
        if (cmp > 0) {
            node.right = put(node.right, key, value);
        } else if (cmp < 0) {
            node.left = put(node.left, key, value);
        } else {
            node.value = value; //相等就替换掉
        }
        node.N = size(node.left) + size(node.right) + 1;
        return node;
    }


    private K min() {
        return min(root).key;
    }

    private Node min(Node node) {
        if (node.left == null) {
            return node;
        }
        return min(node.left);
    }

    public K max() {
        return max(root).key;
    }

    private Node max(Node node) {
        if (node.right == null) { //如果最右边的节点是空的，返回父节点
            return node;
        }
        return max(node.right);//一直找最右边的
    }

    public K floor(K key) {
        Node node = floor(root, key);
        if (node == null) {
            return null;
        }
        return node.key;
    }

    /**
     * 向下取整
     * 找到一个key做比较，如果大于这个节点的键值,则向左找,小于的话
     */

    private Node floor(Node node, K key) {
        if (node == null) {
            return null;
        }
        int cmp = key.compareTo(node.key);
        if (cmp == 0) {
            return node;
        } else if (cmp < 0) {
            return floor(node.left, key);
        } else {
            Node right = floor(node.right, key);
            if (right == null) {
                return node;
            } else {
                return right;
            }
        }

    }


    public K cling(K key) {
        Node node = cling(root, key);
        if (node == null) {
            return null;
        }
        return node.key;
    }

    public Node cling(Node node, K key) {
        if (node == null) {
            return null;
        }
        int cmp = key.compareTo(node.key);
        if (cmp == 0) {
            return node;
        } else if (cmp > 0) {
            return cling(node.right, key);
        } else {
            Node left = cling(node.left, key);
            if (left == null) {
                return node;
            } else {
                return left;
            }
        }
    }

    public static void main(String[] args) {
        BinaryTree<Integer, String> bt = new BinaryTree<>();
        int N = 100000;
        for (int i = 0; i < N; i++) {
            int temp = (int) (Math.random() * (N));
            bt.put(temp, temp + "");
        }
        System.out.println(bt.get(7547));
        System.out.println(bt.max());
        System.out.println(bt.min());
        System.out.println(bt.size());

    }

}
