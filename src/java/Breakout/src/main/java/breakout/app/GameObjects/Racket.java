package breakout.app.GameObjects;

import org.json.JSONArray;
import org.json.JSONObject;

public class Racket extends GameObject {
    
    private float[] position = { 0.0f, 0.0f};
    private double size = 1.0;
    private float speed = 1.0f;

    public Racket(float X, float Y){
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

    public synchronized void move(float newX, float newY){
        this.position[0] = newX;
        this.position[1] = newY;
        this.updateContent(Property.POSITION);
    }

    public synchronized void setSize(double newSize){
        this.size = newSize;
        this.updateContent(Property.SIZE);
    }

    public synchronized void setSpeed(float newSpeed){
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
