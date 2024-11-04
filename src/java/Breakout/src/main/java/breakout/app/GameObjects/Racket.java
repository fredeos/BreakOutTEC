package breakout.app.GameObjects;

import org.json.JSONArray;
import org.json.JSONObject;

public class Racket extends GameObject {
    
    private int[] position = { 0, 0};
    private int size = 1;
    private int speed = 1;

    public Racket(int X, int Y){
        this.position[0] = X;
        this.position[1] = Y;

        this.content = new JSONObject();
        JSONArray Vposition = new JSONArray();
            Vposition.put(this.position[0]);
            Vposition.put(this.position[1]);
        
        this.content.put("position", Vposition);
        this.content.put("size", this.size);
        this.content.put("speed", this.speed);
    }

    public synchronized void move(int newX, int newY){
        this.position[0] = newX;
        this.position[1] = newY;
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
                JSONArray Vposition = new JSONArray();
                    Vposition.put(this.position[0]);
                    Vposition.put(this.position[1]);
                this.content.put("position", Vposition);
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
