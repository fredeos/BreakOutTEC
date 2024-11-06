package breakout.app.GameObjects;

import java.util.Random;

import org.json.JSONObject;
import org.json.JSONArray;

public class Ball extends GameObject {

    public int id;
    private double[] position = {0, 0};
    private int speed = 1;

    public Ball(double X, double Y){
        Random gen = new Random();
        this.id = gen.nextInt(9999);
        this.position[0] = X;
        this.position[1] = Y;

        this.content = new JSONObject();
        JSONArray Vposition = new JSONArray();
            Vposition.put(this.position[0]);
            Vposition.put(this.position[1]);
        this.content.put("id", this.id);
        this.content.put("position", Vposition);
        this.content.put("speed", this.speed);
    }

    public Ball(double X, double Y, int id){
        this.id = id;
        this.position[0] = X;
        this.position[1] = Y;

        this.content = new JSONObject();
        JSONArray Vposition = new JSONArray();
            Vposition.put(this.position[0]);
            Vposition.put(this.position[1]);
        this.content.put("id", this.id);
        this.content.put("position", Vposition);
        this.content.put("speed", this.speed);
    }

    public synchronized void move(double newX, double newY){
        this.position[0] = newX;
        this.position[1] = newY;
        this.updateContent(Property.POSITION);
    }

    public synchronized void setSpeed(int newSpeed){
        this.speed = newSpeed;
        this.updateContent(Property.SPEED);
    }

    public Ball duplicate(){
        Ball newinstance = new Ball(50.0, 50.0);
            newinstance.setSpeed(this.speed);
        return newinstance;
    }

    @Override
    public void updateContent(Property property){
        switch (property) {
            case POSITION:
                JSONArray Vposition = new JSONArray();
                    Vposition.put(this.position[0]);
                    Vposition.put(this.position[1]);
                this.content.put("position", Vposition);
                break;
            case SPEED:
                this.content.put("speed", this.speed);
                break;
        }
    }
}
