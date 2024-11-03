package breakout.app.GameObjects;

import org.json.JSONArray;
import org.json.JSONObject;

public class Brick extends GameObject {

    private PowerUp contained;
    private int durability;
    private String color;
    private int[] matrix_position;

    public Brick(int durability, String color, int i, int j){
        this.durability = durability;
        this.color = color;
        this.matrix_position = new int[2];
        this.matrix_position[0] = i;
        this.matrix_position[1] = j;
        
        this.content = new JSONObject();
            this.content.put("powerup", "none");
            this.content.put("durability", this.durability);
            this.content.put("color", this.color);
            
        JSONArray position = new JSONArray();
        position.put(this.matrix_position[0]);
        position.put(this.matrix_position[1]);
            this.content.put("position", position);
    }

    public void setPowerUp(PowerUp pUp){
        this.contained = pUp;
        this.updateContent(Property.POWER);
    }

    public PowerUp strike(){
        if (this.durability > 0){
            this.durability --;
            this.updateContent(Property.DURABILITY);
        }
        if (this.durability == 0 && this.contained != null){
            PowerUp copy = this.contained;
            this.contained = null;
            this.updateContent(Property.POWER);
            return copy;
        }
        return null;
    }

    @Override
    public synchronized void updateContent(Property property){
        switch (property) {
            case POWER:
                if (this.contained == null){
                    this.content.put("powerup", "none");
                } else {
                    this.content.put("powerup", this.contained.description);
                }
                break;
            case DURABILITY:
                this.content.put("durability", this.durability);
                break;
        }
    }
}
