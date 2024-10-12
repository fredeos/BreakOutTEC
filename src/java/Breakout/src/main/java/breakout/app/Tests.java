package breakout.app;

import breakout.app.modules.data_structures.CircularList;

public class Tests {
    public static void main( String[] args ) {
        CircularList lista = new CircularList();
            lista.insert(31);
            lista.insert(44);
            lista.insert(777);
            lista.insert(69);
            lista.insert(11);
            lista.insert(800);
        System.out.println(lista.size);
        int reference = (int) lista.getCurrent();
        boolean cycle = true;
        do {
            System.out.println(lista.goForward());
            if ((int)lista.getCurrent()==reference){
                break;
            }
        } while (cycle);

    }
}
