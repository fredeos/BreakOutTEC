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
                json.put("description", "new-game");
                response.put("attach", this.session.getSessionInformation()); 
                break;
            case "update-game":
                String action = json.getString("action");
                response.put("code", 100);
                response.put("request", "update-game");
                response.put("response", "session-updated-succesfully");
                json.put("description", "data-updated");
                response.put("action", action);
                if (action == "move-ball"){
                    JSONArray balls = json.getJSONArray("attach");
                    for (int i = 0; i < balls.length(); i++){
                        JSONObject ball = balls.getJSONObject(i);
                        JSONArray position = ball.getJSONArray("position");
                        this.session.moveBall(ball.getString("id"), position.getFloat(0), position.getFloat(1));
                    }
                } else if (action == "move-racket"){
                    JSONObject racket = json.getJSONObject("attach");
                    JSONArray position = racket.getJSONArray("position");
                    this.session.moveRacket(position.getFloat(0), position.getFloat(1));
                } else if (action == "strike-brick"){
                    JSONObject brick = json.getJSONObject("attach");
                    JSONArray position = brick.getJSONArray("position");
                    this.session.registerHitOnBrick(position.getInt(0), position.getInt(1));
                    if (this.session.changes != "none"){
                        response.put("changes", true);
                        response.put("attach", this.session.changes);
                    } else {
                        response.put("changes", false);
                    }
                    this.session.changes = "none";
                }
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
