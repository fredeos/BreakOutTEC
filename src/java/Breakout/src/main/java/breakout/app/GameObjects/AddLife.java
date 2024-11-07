package breakout.app.GameObjects;

import org.json.JSONObject; 

public class AddLife extends PowerUp{
    public int quantity;
    public AddLife(int quantity_value){
        this.quantity = quantity_value;
        this.description = new JSONObject();
        this.description.put("category", "life");
        this.description.put("modifier", "quantity");
        this.description.put("value", this.quantity);
    }
}
