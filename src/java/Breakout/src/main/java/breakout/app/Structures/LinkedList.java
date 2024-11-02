package breakout.app.Structures;  

/* Clase para una lista enlazada bidireccional 
 * 
*/
public class LinkedList {

    private Node head;
    private Node tail;
    public int size;

    public LinkedList(){
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    /* Obtiene el nodo cabezal de la lista*/
    public synchronized Node getHead(){
        return this.head;
    }

    /* Obtiene el nodo en la cola de la lista*/
    public synchronized Node getTail(){
        return this.tail;
    }

    /* Inserta un elemento al inicio de lista
     * @parm value: dato o valor por insertar en la lista
    */
    public synchronized void insert(Object value){
        Node node = new Node(value);
        if (this.head == null && this.tail == null){
            this.head = node;
            this.tail = node;
        } else {
            Node temp = this.head;
            node.setNext(temp);
            temp.setPrev(node);
            if (temp.getNext()==null){
                this.tail = temp;
            }
            this.head = node;
        }
        this.size ++;
    }

    /* Inserta un elemento en una posicion especificada
     * 
    */
    public synchronized void insertAt(Object value, int index){
        if(index >= this.size || index < 0){
            throw new Error("INSERT: Index("+index+") out of range. Object insertion aborted");
        }
        Node current = this.head;
        int counter = 0;
        while (counter < index){
            current = current.getNext();
            counter++;
        }
        Node node = new Node(value);
            node.setNext(current);
            if (current.getPrev() != null){
                node.setPrev(current.getPrev());
            }
        current.setPrev(node);
        if (index == 0){
            this.head = node;
        }
        this.size ++;
    }

    public synchronized void removeContent(Object value){
        if(this.size == 0){
            throw new Error("REMOVE CONTENT: Can't remove values in an empty list");
        }
        Node current = this.head;
        boolean match = false;
        while (current != null){
            if (current.getData() == value){
                match = true;
                break;
            }
            current = current.getNext();
        }
        if (match){
            Node prev = current.getPrev();
            Node next = current.getNext();
            if (prev != null){
                prev.setNext(next);
            }
            if (next != null){
                next.setPrev(prev);
            }
            if (current == this.head){
                this.head = next;
            }
            if (current == this.tail){
                this.tail = prev;
            }
            current.setNext(null);
            current.setPrev(null);
            this.size --;
        }
    }

    /* Elimina un elemento de la lista en el indice indicado
     * 
    */
    public synchronized void remove(int index){
        if(index >= this.size || index < 0){
            throw new Error("REMOVE: Index("+index+") out of range.");
        }
        Node current = this.head;
        int counter = 0;
        if (index == 0){
            this.head = current.getNext();
            if (this.head != null){
                this.head.setPrev(null);
            }
            current.setNext(null);
        } else if (index == this.size-1) {
            current = this.tail;
            this.tail = current.getPrev();
            if (this.tail != null){
                this.tail.setNext(null);
            }
            current.setPrev(null);
        }else {
            while (counter < index){
                current = current.getNext();   
                counter++;
            }
            Node prev = current.getPrev();
            Node next = current.getNext();
            if (prev != null){
                prev.setNext(next);
            }
            if (next != null){
                next.setPrev(prev);
            }
            current.setNext(null);
            current.setPrev(null);
        }
        this.size --;
    }

    /* Extrae el dato almacenado en la posicion especificada 
     * @param index: indice de posicion
     * @return 
    */
    public synchronized Object get(int index){
        if(index >= this.size || index < 0){
            throw new Error("GET: Index("+index+") out of range.");
        }
        Node current = this.head;
        int counter = 0;
        if (index == this.size-1){
            current = this.tail;
        } else {
            while (counter < index){
                current = current.getNext();   
                counter++;
            }
        }
        return current.getData();
    }
}
