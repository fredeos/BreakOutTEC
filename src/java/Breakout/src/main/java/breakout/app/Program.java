package breakout.app;
//      _____________________________
//_____/ Librerias
import org.json.JSONObject;

//      _____________________________
//_____/ Modulos
import breakout.app.network.Server;
import java.io.*;

/**
 * Hello world! 
 *
 */
public class Program {
    public static void main( String[] args ) {
        JSONObject object = new JSONObject("{\"mensaje\":\"Ejecutando: [Servidor]\"}");
        System.out.println(object.getString("mensaje"));
        try {
            Server servidor = new Server(8080, "127.0.0.1");
            servidor.turnON();
            // while (servidor.isActive()){
            //     Thread.currentThread().sleep(3000);
            //     servidor.approveClient(0);
            // }
        } catch (IOException e1) {
            System.err.println(e1);
        } // catch (InterruptedException e2){
        //     System.err.println(e2);
        // }
    }
}
