package breakout.app;

import breakout.app.Structures.LinkedList;
 
public class Main {
    public static void main( String[] args ) {
        LinkedList list = new LinkedList();
            list.insert(77);
            list.insert(1);
        list.removeContent(1);
        System.out.println(list.size);
        System.out.println(list.get(0));
        // CircularList lista = new CircularList();
        //     lista.insert(31);
        //     lista.insert(44);
        //     lista.insert(777);
        //     lista.insert(69);
        //     lista.insert(11);
        //     lista.insert(800);
        // System.out.println(lista.size);
        // int reference = (int) lista.getCurrent();
        // boolean cycle = true;
        // do {
        //     System.out.println(lista.goForward());
        //     if ((int)lista.getCurrent()==reference){
        //         break;
        //     }
        // } while (cycle);

    }
}
