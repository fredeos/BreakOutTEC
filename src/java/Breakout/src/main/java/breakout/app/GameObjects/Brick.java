package breakout.app.GameObjects;

import org.json.JSONObject;

public class Brick extends GameObject {

    private PowerUp contained;
    private int durability;
    private String color;

    public Brick(int durability, String color){
        this.content = new JSONObject();
        this.content.append("powerup", null);
        this.content.append("durability", durability);
        this.content.append("color", color);
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
                this.content.append("powerup", this.contained.description.toString());
                break;
            case DURABILITY:
                this.content.append("durability", this.durability);
                break;
        }
    }
}
