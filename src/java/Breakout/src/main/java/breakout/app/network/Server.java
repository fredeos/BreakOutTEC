package breakout.app.network;

import java.io.*;
import java.net.*;
import org.json.*;

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

    public LinkedList pending;
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
        this.pending = new LinkedList();
        this.clientlist = new LinkedList();
        this.playerlist = new CircularList();

        System.out.println("Servidor activo!");
        System.out.println(PORT);
        System.out.println(IP);
    }

    /* Abre una sala de espera donde recibe
     * 
    */
    public synchronized void turnON(){
        this.active = true;
        this.handler = new Thread(()->{
            System.out.println("Iniciando sala de espera...");
            System.out.println("Esperando que se conecten clientes");
            do {
                try {
                    Socket client = this.socket.accept();
                    System.out.println("Cliente conectado!");
                    
                    BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String message = input.readLine();
                    System.out.println(message);

                    JSONObject startup = new JSONObject(message);
                    String type = startup.getString("type");

                    Client new_client;
                    if (type.equals("player")){
                        new_client = new ClientPlayer(client, startup.getString("id"), startup.getString("name"));
                        this.clientlist.insert(new_client);
                        this.playerlist.insert(new_client);
                        this.pending.insert(new_client);
                        System.out.println("Clients on hold:"+this.pending.size);
                        new_client.changeOutput(this.prepareResponse("on-standby"));
                        this.openPlayerChannel((ClientPlayer)new_client);
                    } else {
                        new_client = new ClientSpectator(client, null, startup.getString("id"), startup.getString("name"));
                        this.clientlist.insert(new_client);
                        this.pending.insert(new_client);
                        new_client.changeOutput(this.prepareResponse("on-standby"));
                        this.openSpectatorChannel((ClientSpectator)new_client);
                    }
                } catch (IOException e1) {
                    System.err.println(e1);
                    break;
                }
            } while (this.isActive());
            System.out.println("Finalizando sala de espera...");
        });
        this.handler.start();
    }

    private void openPlayerChannel(ClientPlayer client){
        Thread communication_thread = new Thread(()->{
            System.out.println("Escuchando al cliente jugador: "+client.username+"."+client.identifier);
            loop: while (this.isActive()){ 
                try {
                    client.send();
                    if(!client.standby){
                        String receivedmsg = client.read();
                        System.out.println(receivedmsg);
                        JSONObject json = new JSONObject(receivedmsg);
                        switch (json.getString("request")) {
                            case "end-connection":
                                client.changeOutput(this.prepareResponse("end-connection"));
                                client.send();
                                client.terminate();
                                break loop;
                            case "no-update":
                                client.changeOutput(this.prepareResponse("no-update"));
                                break;
                            case "on-standby":
                                client.changeOutput(this.prepareResponse("on-standby"));
                                break;
                            default:
                                client.process();
                                break;
                        }
                    } else {
                        this.approveClient(0);
                    }
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        });
        communication_thread.start();
    }

    private void openSpectatorChannel(ClientSpectator client){
        Thread communication_thread = new Thread(()->{
            System.out.println("Escuchando al cliente espectador: "+client.username+"."+client.identifier);
            while (this.isActive()){
                if(!client.standby){
                    try {
                        String receivedmsg = client.read();
                        switch (receivedmsg) {
                            case "next-player":
                                break;
                        
                            default:
                                break;
                        }
                        client.send();
                    } catch (IOException e) {
                        // TODO: handle exception
                    }
                }
            }
        });
        communication_thread.start();
    }

    private String prepareResponse(String request){
        String jsonresponse = "";
        JSONObject json = new JSONObject();
        switch (request) {
            case "on-standby":
                json.put("code", 100);
                json.put("request", "connection");
                json.put("response", "standby");
                json.put("description", "wait");
                break;
            case "approve":
                json.put("code", 100);
                json.put("request", "connection");
                json.put("response", "approved");
                json.put("description", "start");
                break;
            case "reject":
                json.put("code", 100);
                json.put("request", "connection");
                json.put("response", "rejected");
                json.put("description", "end");
                break;
            case "end-connection":
                json.put("code", 100);
                json.put("request", "end-connection");
                json.put("response", "closing");
                json.put("description", "end");
                break;
            case "no-update":
                json.put("code", 100);
                json.put("request", "no-update");
                json.put("response", "standby");
                json.put("description", "wait");
            default:
                break;
        }
        jsonresponse = json.toString();
        return jsonresponse;
    }

    public void approveClient(int i){
        Client client = (Client) this.pending.get(i);
        client.continue_();
        client.changeOutput(this.prepareResponse("approve"));
        this.pending.removeContent(client);
    }

    public void rejectClient(int i){
        Client client = (Client) this.pending.get(i);
        client.continue_();
        client.changeOutput(this.prepareResponse("reject"));
        this.pending.removeContent(client);
    }

    /* Verifica que el servidor aun este activo*/
    public boolean isActive(){
        return this.active;
    }

    /* Desactiva la comunicacion del servidor con sus clientes*/
    public synchronized void turnOFF(){
        this.active = false;
        // TODO: Falta implementar que se notifique a los clientes que la conexion ha finalizado
    }
}
