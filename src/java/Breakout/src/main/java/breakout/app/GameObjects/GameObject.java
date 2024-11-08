package breakout.app.GameObjects;

import org.json.JSONObject; 

public abstract class GameObject {
    protected JSONObject content;

    protected abstract void updateContent(Property property);

    public JSONObject getContent(){
        JSONObject json = new JSONObject(content.toString());
        return json;
    }
};
