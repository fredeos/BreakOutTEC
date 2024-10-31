public class LinkedList {
    Node head;
    Node tail;

    static class Node {
        int data;
        Node next;

        Node(int d){
            data= d;
            next= null;
        }
            
    }

    public void addElemEnd(Node x){
        if (tail==null){
            head=x;
            tail= x;
            return;
        }

        tail.next= x;
        tail= tail.next;
           

    
    }

    public void addElemStart(Node x){
        if (head==null){
            head=x;
            tail= x;
            return;
        }

        x.next= head;
        head=x;
           

    
    }

    public void addElemIdx(Node x, int idx){
        Node n= head;
        if (n==null){
            head=x;
            return;
        }

        for (int i=0; i<idx-1; i++){
            n= n.next;
        }

        x.next= n.next;
        n.next= x;
    }

    public void deleteElemStart(){
        if (head==null){
            return;
        }
        head= head.next;
    }

    public void deleteElemEnd(){
        if (tail==null){
            return;
        }
        Node n= head;
        while (n.next != tail){
            n= n.next;
        }
        tail= n;
        n.next= null;
    
    }


}

