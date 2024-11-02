package breakout.app.GameObjects;

import java.util.UUID;

import org.json.JSONObject;
import org.json.JSONArray;

public class Ball extends GameObject {

    private String id;
    private float[] position = {0.0f, 0.0f};
    private float speed = 1.0f;

    public Ball(float X, float Y){
        UUID uuid = UUID.randomUUID();
        this.id = uuid.toString();
        this.position[0] = X;
        this.position[1] = Y;

        this.content = new JSONObject();
        JSONArray Vposition = new JSONArray();
            Vposition.put(this.position[0]);
            Vposition.put(this.position[1]);
        this.content.append("id", this.id);
        this.content.append("position", Vposition.toString());
        this.content.append("speed", this.speed);
    }

    public synchronized void move(float newX, float newY){
        this.position[0] = newX;
        this.position[1] = newY;
        this.updateContent(Property.POSITION);
    }

    public synchronized void setSpeed(float newSpeed){
        this.speed = newSpeed;
        this.updateContent(Property.SPEED);
    }

    public Ball duplicate(){
        Ball newinstance = new Ball(this.position[0], this.position[1]);
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
                this.content.put("position", Vposition.toString());
                break;
            case SPEED:
                this.content.put("speed", this.speed);
                break;
        }
    }
}