package breakout.app.GameObjects;

import org.json.JSONObject; 

public abstract class PowerUp {

    protected JSONObject description;

    public String getDescription(){
        return this.description.toString();
    }
}
