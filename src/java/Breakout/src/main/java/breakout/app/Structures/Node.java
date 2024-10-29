package breakout.app.Structures;

/* Clase para creacion de nodos necesarios en estructuras de datos lineales.
 * Atributos de la clase:
 * next: siguiente nodo conectado
 * prev: anterior nodo conectado
 * data: objecto o dato almacendao
*/
public class Node {
    private Node next;
    private Node prev;
    private Object data;

    /* Metodo constructor de un nodo
     * @param data: instancia de un objeto para guardar en un nodo
    */
    public Node(Object data){
        this.data = data;
        this.next = null;
        this.prev = null;
    }

    /* Obtiene una referencia al siguiente nodo */
    public synchronized Node getNext(){
        return this.next;
    }

    /* Obtiene una referencia al nodo anterior */
    public synchronized Node getPrev(){
        return this.prev;
    }

    /* Modifica la referencia al siguiente nodo 
     * @param node: nodo al que enlazar como el siguiente
    */
    public synchronized void setNext(Node node){
        this.next = node;
    }

    /* Modifica la referencia al nodo anterior 
     * @param node: nodo al que enlazar como el anterior
    */
    public synchronized void setPrev(Node node){
        this.prev = node;
    }
    
    /* Extrae los datos en el nodo */
    public Object getData(){
        return this.data;
    }

    /* Cambia los datos guardados en el nodo
     * data: nuevo objeto o dato por insertar en el nodo
    */
    public synchronized void setData(Object data){
        this.data = data;
    }
}
