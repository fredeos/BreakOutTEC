package breakout.app.network;

import java.io.*;
import java.net.*;
import org.json.*;
import java.util.concurrent.Semaphore;

import breakout.app.Structures.CircularList;
import breakout.app.Structures.LinkedList;
import breakout.app.View.MainWindow;

/* Clase para crear un servidor por medio de sockets TCP
 * Maneja las conexiones con todos los clientes
 */
public class Server {

    // ------------------------------[ Atributos/Propiedades ]------------------------------
    private ServerSocket socket; 
    private Thread handler;
    private boolean active;

    public LinkedList pending;
    public LinkedList clientlist;
    private CircularList playerlist;

    private MainWindow window;
    public Semaphore traffic_lock = new Semaphore(1);
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

    /* Abre una sala de espera donde reciben los clientes
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

                    this.traffic_lock.acquire();
                    Client new_client;
                    if (type.equals("player")){
                        new_client = new ClientPlayer(client, startup.getString("id"), startup.getString("name"));
                        this.clientlist.insert(new_client);
                        this.pending.insert(new_client);
                        System.out.println("Clients on hold:"+this.pending.size);
                        new_client.changeMessage(this.prepareResponse("on-standby"));
                        this.openPlayerChannel((ClientPlayer)new_client);
                    } else {
                        new_client = new ClientSpectator(client, null, startup.getString("id"), startup.getString("name"));
                        this.clientlist.insert(new_client);
                        this.pending.insert(new_client);
                        new_client.changeMessage(this.prepareResponse("on-standby"));
                        this.openSpectatorChannel((ClientSpectator)new_client);
                    }
                    this.traffic_lock.release();
                } catch (IOException e1) {
                    System.err.println(e1);
                    break;
                } catch (InterruptedException e2){
                    System.err.println(e2);
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
                    String receivedmsg = client.read();
                    if (!client.isOnStandBy()) {
                        JSONObject json = new JSONObject(receivedmsg);
                        switch (json.getString("request")) {
                            case "end-connection":
                                client.changePriorityMessage(this.prepareResponse("end-connection"));
                                client.send();
                                this.traffic_lock.acquire();
                                this.clientlist.removeContent(client);
                                while (true) {
                                    if (this.playerlist.getCurrent() == client){
                                        this.playerlist.removeCurrent();
                                        break;
                                    }
                                    this.playerlist.goForward();
                                }
                                client.terminate();
                                this.traffic_lock.release();
                                break loop;
                            case "no-update":
                                client.changeMessage(this.prepareResponse("no-update"));
                                break;
                            case "on-standby":
                                client.changeMessage(this.prepareResponse("on-standby"));
                                break;
                            default:
                                client.process();
                                break;
                        }
                    }
                } catch (IOException e1) {
                    System.err.println(e1);
                    break;
                }  catch (InterruptedException e2){
                    System.err.println(e2);
                }
            }
        });
        communication_thread.start();
    }

    private void openSpectatorChannel(ClientSpectator client){
        Thread communication_thread = new Thread(()->{
            System.out.println("Escuchando al cliente espectador: "+client.username+"."+client.identifier);
            loop: while (this.isActive()){
                try {
                    client.send();
                    String receivedmsg = client.read();
                    if(!client.isOnStandBy()){
                        JSONObject json = new JSONObject(receivedmsg);
                        switch (json.getString("request")) {
                            case "end-connection":
                                client.changePriorityMessage(this.prepareResponse("end-connection"));
                                client.send();
                                this.traffic_lock.acquire();
                                this.clientlist.removeContent(client);
                                client.terminate();
                                this.traffic_lock.release();
                                break loop;
                            case "spectating-player":
                                client.acquireUpdate();
                                break;
                            case "waiting-for-player":
                                this.traffic_lock.acquire();
                                if (this.playerlist.size > 0){
                                    client.changeTarget((ClientPlayer)this.playerlist.getCurrent());
                                    client.acquireUpdate();
                                } else {
                                    client.changeMessage(this.prepareResponse("no-player-yet"));
                                }
                                this.traffic_lock.release();
                                break;
                            case "spectator-lost-visual":
                                client.changeTarget(null);
                                client.changeMessage("no-player-yet");
                                break;
                            case "next-player":
                                this.traffic_lock.acquire();
                                if (this.playerlist.size > 0){
                                    client.changeTarget((ClientPlayer)this.playerlist.goForward());
                                    client.acquireUpdate();
                                } else {
                                    client.changeMessage(this.prepareResponse("no-player-yet"));
                                }
                                this.traffic_lock.release();
                                break;
                            case "previous-player":
                                this.traffic_lock.acquire();
                                if (this.playerlist.size > 0){
                                    client.changeTarget((ClientPlayer)this.playerlist.goBackward());
                                    client.acquireUpdate();
                                } else {
                                    client.changeMessage(this.prepareResponse("no-player-yet"));
                                }
                                this.traffic_lock.release();
                                break;
                            default:
                                client.changeMessage(this.prepareResponse("no-update"));
                                break;
                        }
                    }
                } catch (IOException e1) {
                    System.err.println(e1);
                } catch (InterruptedException e2){
                    System.err.println(e2);
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
                break;
            case "no-player-yet":
                json.put("code", 100);
                json.put("request", "waiting-for-player");
                json.put("response", "no-player-yet");
                json.put("description", "wait");
                break;
            default:
                break;
        }
        jsonresponse = json.toString();
        return jsonresponse;
    }

    public synchronized void approveClient(int i){
        try {
            this.traffic_lock.acquire();
            System.out.println(this.pending.size);
            Client client = (Client) this.pending.get(i);
            client.continue_();
            client.changePriorityMessage(this.prepareResponse("approve"));
            if (client.type.equals("player")){
                this.playerlist.insert(client);
            }
            this.pending.removeContent(client);
            System.out.println(this.pending.size);
        } catch (InterruptedException e1) {
            System.err.println(e1);
        } finally {
            this.traffic_lock.release();
        }
    }

    public synchronized void rejectClient(int i){
        try {
            this.traffic_lock.acquire();
            System.out.println(this.pending.size);
            Client client = (Client) this.pending.get(i);
            client.continue_();
            client.changePriorityMessage(this.prepareResponse("reject"));
            this.pending.removeContent(client);
            System.out.println(this.pending.size);
        } catch (InterruptedException e1) {
            System.err.println(e1);
        } finally {
            this.traffic_lock.release();
        }
    }

    /* Verifica que el servidor aun este activo*/
    public boolean isActive(){
        return this.active;
    }

    /* Desactiva la comunicacion del servidor con sus clientes*/
    public synchronized void turnOFF(){
        try {
            this.traffic_lock.acquire();
            while(this.pending.size > 0){
                Client client = (Client) this.pending.get(0);
                client.changePriorityMessage(this.prepareResponse("reject"));
                this.pending.remove(0);
            }
            while(this.playerlist.size > 0){
                this.playerlist.removeCurrent();
            }
            while (this.clientlist.size > 0){
                Client client = (Client) this.clientlist.get(0);
                this.clientlist.remove(0);
                client.changePriorityMessage(this.prepareResponse("end-connection"));
            }
        } catch (InterruptedException e1){
            System.err.println(e1);
        }
        this.active = false;
    }
}
