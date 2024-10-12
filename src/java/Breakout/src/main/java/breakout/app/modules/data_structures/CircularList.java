package breakout.app.modules.data_structures;


public class CircularList {
    public int size;
    private Node origin;
    private Node current;

    public CircularList(){
        this.size = 0;
        this.origin = null;
        this.current = null;
    }

    public synchronized void insert(Object value){
        Node node = new Node(value);
        if (this.origin == null){
            this.origin = node;
            this.origin.setNext(node);
            this.origin.setPrev(node);
        } else if (this.origin != null && this.origin == this.current && this.size == 1) {
            this.origin.setNext(node);
            this.origin.setPrev(node);

            node.setPrev(this.origin);
            node.setNext(this.origin);
        } else {
            Node current_temp = this.current;

            node.setPrev(current_temp);
            node.setNext(current_temp.getNext());

            current_temp.getNext().setPrev(node);
            current_temp.setNext(node);
        }
        this.current = node;
        this.size++;
    }

    public synchronized void remove(Object value){
        if (this.origin == null && this.current == null){
            throw new Error("REMOVE: Can't remove specified node if list is empty");
        }
        Node temp = this.current;
        boolean match = false;
        while (true){
            if (temp.getData() == value){
                match = true;
                break;
            }
            if (temp.getNext() == this.current){
                break;
            }
            temp = temp.getNext();
        }
        if (match){
            if (this.size == 1){
                temp.setNext(null);
                temp.setPrev(null);
                this.origin = null;
                this.current = null;
            } else if (this.size > 1) {
                Node next = temp.getNext();
                Node prev = temp.getPrev();

                next.setPrev(prev);
                prev.setNext(next);

                temp.setNext(null);
                temp.setPrev(null);
                if (temp == this.origin){
                    this.origin = prev;
                }
                this.current = next;
            }
            this.size--;
        }
    }

    public synchronized void removeCurrent(){
        if (this.origin == null && this.current == null){
            throw new Error("REMOVE CURRENT: Can't remove current node if list is empty");
        }
        if (this.size == 1){
            this.current.setNext(null);
            this.current.setPrev(null);
            this.current = null;
            this.origin = null;
        } else if (this.size > 1){
            Node temp = this.current;

            Node next = this.current.getNext();
            Node prev = this.current.getPrev();
            next.setPrev(prev);
            prev.setNext(next);

            temp.setNext(null);
            temp.setPrev(null);
            if (this.origin == this.current){
                this.origin = prev;
            }
            this.current = next;
        }
        this.size--;
    }

    public synchronized Object goForward(){
        this.current = this.current.getNext();
        return this.current.getData();
    }

    public synchronized  Object goBackward(){
        this.current = this.current.getPrev();
        return this.current.getData();
    }

    public synchronized Object getCurrent(){
        return this.current.getData();
    }
}
