package breakout.app.GameObjects;

import org.json.JSONObject;
import org.json.JSONArray;

public class Ball extends GameObject {
    private float[] position = {0.0f, 0.0f};
    private float speed = 1.0f;
    private float movement_angle = 90.0f;

    public Ball(float X, float Y){
        this.position[0] = X;
        this.position[1] = Y;

        this.content = new JSONObject();
        JSONArray Vposition = new JSONArray();
            Vposition.put(this.position[0]);
            Vposition.put(this.position[1]);
        this.content.append("id", "x");
        this.content.append("position", Vposition);
        this.content.append("speed", speed);
        this.content.append("angle", movement_angle);
    }

    @Override
    public void updateContent(Property property){

    }
}
