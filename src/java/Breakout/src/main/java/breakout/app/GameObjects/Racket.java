package breakout.app.GameObjects;

import org.json.JSONArray;
import org.json.JSONObject;

public class Racket extends GameObject {
    
    private double position = 0.0;
    private int size = 1;
    private int speed = 1;

    public Racket(double X){
        this.position = X;

        this.content = new JSONObject();
        this.content.put("position", this.position);
        this.content.put("size", this.size);
        this.content.put("speed", this.speed);
    }

    public synchronized void move(double newX){
        this.position = newX;
        this.updateContent(Property.POSITION);
    }

    public synchronized void setSize(int newSize){
        this.size = newSize;
        this.updateContent(Property.SIZE);
    }

    public synchronized void setSpeed(int newSpeed){
        this.speed = newSpeed;
        this.updateContent(Property.SPEED);
    }

    @Override
    public void updateContent(Property property){
        switch (property) {
            case POSITION:
                this.content.put("position", this.position);
                break;
            case SPEED:
                this.content.put("speed", this.speed);
                break;
            case SIZE:
                this.content.put("size", this.size);
                break;
        }
    }
}
