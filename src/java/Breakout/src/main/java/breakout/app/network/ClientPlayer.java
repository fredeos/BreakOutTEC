package breakout.app.network;

import java.io.*;
import java.net.Socket;

import org.json.JSONArray;
import org.json.JSONObject; 

import breakout.app.Controller.SessionController; 

import breakout.app.Structures.LinkedList;
import breakout.app.network.observer.*;

public class ClientPlayer extends Client implements Publisher {

    private LinkedList subscribers;
    private SessionController session;

    public ClientPlayer(Socket client, String id, String name){
        this.socket = client;
        this.subscribers = new LinkedList();
        this.identifier = id;
        this.username = name;
        this.session = null;
        try {
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.out = new PrintWriter(this.socket.getOutputStream());
        } catch (IOException e) {
            System.err.println("ERROR: No se pudieron iniciar los componentes necesarios para el cliente\nDETALLE:\n"+e);
        }
    }

    @Override
    public synchronized void continue_(){
        this.standby = false;
        this.session = new SessionController(this);
        this.session.initiate();
    }

    public synchronized void process(){
        JSONObject json = new JSONObject(this.received);
        JSONObject response = new JSONObject(); 
        switch (json.getString("request")) {
            case "init":
                response.put("code", 100);
                response.put("request", "initiate-game");
                response.put("response", "session-created");
                response.put("description", "new-game");
                response.put("attach", this.session.getSessionInformation()); 
                break;
            case "update-game":
                String[] parameters = json.getString("action").split(":");
                String action = parameters[0];

                response.put("code", 100);
                response.put("request", "update-game");
                response.put("response", "session-updated");
                response.put("description", "data-updated");
                response.put("action", action);
                if (action.equals("move-ball")){
                    JSONObject ball = json.getJSONObject("attach");
                    JSONArray position = ball.getJSONArray("position");
                    this.session.moveBall(ball.getInt("id"), position.getInt(0), position.getInt(1));
                } else if (action.equals("move-racket")){
                    JSONObject racket = json.getJSONObject("attach");
                    JSONArray position = racket.getJSONArray("position");
                    this.session.moveRacket(position.getInt(0), position.getInt(1));
                } else if (action.equals("strike-brick")){
                    JSONObject brick = json.getJSONObject("attach");
                    JSONArray position = brick.getJSONArray("position");
                    this.session.registerHitOnBrick(position.getInt(0), position.getInt(1));
                } else if (action.equals("rm-ball")){
                    JSONObject ball = json.getJSONObject("attach");
                    this.session.deleteBall(ball.getInt("id"));
                } else { // Caso de apply-powerup
                    String powerup_type = parameters[1];
                    if (powerup_type.equals("add-life")){
                        int total_life = json.getInt("attach");
                        this.session.increaseLife(total_life);
                    } else if (powerup_type.equals("add-ball")){
                        JSONObject new_ball = json.getJSONObject("attach");
                        JSONArray position = new_ball.getJSONArray("position");
                        this.session.addNewBall(new_ball.getInt("id"), position.getInt(0),position.getInt(1));
                    } else if (powerup_type.equals("increase-ball-speed")){
                        int speed = json.getInt("attach");
                        this.session.increaseBallSpeed(speed);
                    } else if (powerup_type.equals("increase-racket-speed")){
                        int speed = json.getInt("attach");
                        this.session.increaseRacketSpeed(speed);
                    } else if (powerup_type.equals("increase-racket-size")){
                        int size = json.getInt("attach");
                        this.session.increaseRacketSize(size);
                    }
                }
                // Obtener los cambios del hechos en la sesion
                if (this.session.getServerChanges().isEmpty()){
                    response.put("byserver",false);
                } else {
                    response.put("byserver",true);
                    response.put("server-changes",this.session.getServerChanges());
                }
                if (this.session.getClientChanges().isEmpty()){
                    response.put("byclient",false);
                } else {
                    response.put("byclient",true);
                    response.put("client-changes",this.session.getClientChanges());
                }
                break;
            default:
                break;
        }
        this.changeOutput(response.toString());
        this.NotifyAll();
    }

    @Override 
    public synchronized void subscribe(Subscriber subscriber){
        this.subscribers.insert(subscriber);
    }

    @Override
    public synchronized void unsubscribe(Subscriber subscriber){
        this.subscribers.removeContent(subscriber);
    }

    @Override
    public synchronized void NotifyAll(){
        for (int i = 0; i < this.subscribers.size; i++){
            Subscriber subscriber = (Subscriber) this.subscribers.get(i);
            this.Notify(subscriber);
        }
    }

    @Override
    public synchronized void Notify(Subscriber subscriber){
        subscriber.update(this.message);
    }
    
}
