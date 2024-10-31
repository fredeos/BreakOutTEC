package breakout.app.GameObjects;

import org.json.JSONObject;

public class SpeedIncrease extends PowerUp {
    public float speed_multipler;

    public SpeedIncrease(float multiplier, String target){
        this.speed_multipler = multiplier;
        this.description = new JSONObject();
        this.description.put("category", target); // El target debe ser 'ball' o 'racket'
        this.description.put("modifier", "speed");
        this.description.put("value", this.speed_multipler);
    }
}
