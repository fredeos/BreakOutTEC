package breakout.app.GameObjects;

import org.json.JSONObject; 

public abstract class GameObject {
    protected JSONObject content;

    protected abstract void updateContent(Property property);

    public String getContent(){
        return this.content.toString();
    }
};
