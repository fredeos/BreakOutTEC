package breakout.app.GameObjects;

 
import org.json.JSONObject;
public class SizeIncrease extends PowerUp {
    public int size_multiplier;

    public SizeIncrease(int multiplier){
        this.size_multiplier = multiplier;
        this.description = new JSONObject();
        this.description.put("category", "racket");
        this.description.put("modifier", "size");
        this.description.put("value", this.size_multiplier);
    }
}
