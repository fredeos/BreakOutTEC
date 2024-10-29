package breakout.app.network;

import java.io.*;
import java.net.*;

import breakout.app.Structures.CircularList;
import breakout.app.Structures.LinkedList;

/* Clase para crear un servidor por medio de sockets TCP
 * Maneja las conexiones con todos los clientes
 */
public class Server {

    // ------------------------------[ Atributos/Propiedades ]------------------------------
    private ServerSocket socket; 
    private Thread handler;
    private boolean active;

    private LinkedList clientlist;
    private CircularList playerlist;

    // ------------------------------[ Metodos ]------------------------------
    /* Metodo constructor de la clase servidor
     * @param PORT: 
     * @param IP
     * @returns
    */
    public Server(int PORT, String IP) throws IOException{
        InetAddress address = InetAddress.getByName(IP);
        this.socket = new ServerSocket(PORT, 0, address);
        System.out.println("Servidor activo!");
        System.out.println(PORT);
        System.out.println(IP);

        this.active = true;
        this.waitingRoom();
    }

    /* Abre una sala de espera donde recibe
     * 
    */
    private void waitingRoom(){
        this.handler = new Thread(()->{
            System.out.println("Iniciando sala de espera...");
            System.out.println("Esperando que se conecten clientes");
            do {
                try {
                    Socket cliente = this.socket.accept();
                    System.out.println("Cliente conectado!");
                    
                    BufferedReader input = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
                    PrintWriter output = new PrintWriter(cliente.getOutputStream());

                    String message = input.readLine();
                    System.out.println("Cliente: "+message);
                    System.out.println("Mensaje leido. Terminando conexion");
                    cliente.close();

                    this.toggleActive();
                } catch (IOException e) {
                    System.err.println(e);
                    break;
                }
            } while (this.isActive());
            System.out.println("Finalizando sala de espera...");
        });
        this.handler.start();
    }

    private void openPlayerChannel(ClientPlayer client){
        Thread communication_thread = new Thread(()->{

        });
        communication_thread.start();
    }

    private void openSpectatorChannel(ClientSpectator client){
        Thread communication_thread = new Thread(()->{

        });
        communication_thread.start();
    }


    /* Verifica que el servidor aun este activo*/
    private synchronized boolean isActive(){
        return this.active;
    }

    /* Activa/desactiva la comunicacion del servidor con sus clientes*/
    public synchronized void toggleActive(){
        this.active = !this.active;
        if (this.active == false){
            // TODO: Falta implementar que se notifique a los clientes que la conexion ha finalizado
        }
    }
}
