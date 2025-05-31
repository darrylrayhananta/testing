package custom_adapters;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonParseException;
import entity.npc.NPC;
import data.NPCData;
import main.GamePanel; // You might need this reference if NPC constructor requires it.
import java.lang.reflect.Type;
import java.util.List;

public class NPCAdapter implements JsonSerializer<NPC>, JsonDeserializer<NPC> {

    private GamePanel gp; // Added constructor to pass GamePanel

    public NPCAdapter(GamePanel gp) {
        this.gp = gp;
    }

    // Default constructor for deserialization without GamePanel if needed elsewhere,
    // but likely, you'll always need GamePanel for NPC instantiation.
    public NPCAdapter() {
        this.gp = null; // Set to null or handle appropriately if no GP context is available
    }

    @Override
    public JsonElement serialize(NPC src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("npcName", src.getNPCName());
        jsonObject.addProperty("heartPoints", src.getHeartPoints());
        jsonObject.addProperty("relationshipStatus", src.getRelationshipStatus());
        // Do not serialize lists like lovedItems, likedItems, hatedItems as they are static data
        return jsonObject;
    }

    @Override
    public NPC deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String npcName = jsonObject.get("npcName").getAsString();
        int heartPoints = jsonObject.get("heartPoints").getAsInt();
        String relationshipStatus = jsonObject.get("relationshipStatus").getAsString();

        // Retrieve the actual NPC instance from NPCData
        // This assumes NPCData.initialize(gp) has been called before loading
        NPC npc = NPCData.getNPCByName(npcName);
        if (npc != null) {
            npc.setHeartPoints(heartPoints);
            npc.setRelationshipStatus(relationshipStatus);
            return npc;
        } else {
            throw new JsonParseException("Could not find NPC with name: " + npcName + " during deserialization.");
        }
    }
}