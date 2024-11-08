package breakout.app.GameObjects;

import org.json.JSONObject; 

public class AddBall extends PowerUp {
    public int quantity;
    public AddBall(int quantity_value){
        this.quantity = quantity_value;
        this.description = new JSONObject();
        this.description.put("category", "ball");
        this.description.put("modifier", "quantity");
        this.description.put("value", this.quantity);
    }
}
