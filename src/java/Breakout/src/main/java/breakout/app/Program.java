package breakout.app;
//      _____________________________
//_____/ Librerias

import java.io.*;

import breakout.app.network.Server;

public class Program {
    public static void main( String[] args ) {
        try {
            Server servidor = new Server(8080, "127.0.0.1");
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
