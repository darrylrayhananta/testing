package custom_adapters;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonParseException;
import entity.player.Inventory;
import items.Items;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class InventoryAdapter implements JsonSerializer<Inventory>, JsonDeserializer<Inventory> {

    @Override
    public JsonElement serialize(Inventory src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        JsonObject itemsMap = new JsonObject();
        for (Map.Entry<Items, Integer> entry : src.checkInventory().entrySet()) {
            itemsMap.add(entry.getKey().getName() + "_" + entry.getKey().getType(), context.serialize(entry.getValue()));
        }
        jsonObject.add("items", itemsMap);
        return jsonObject;
    }

    @Override
    public Inventory deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Inventory inventory = new Inventory();
        JsonObject jsonObject = json.getAsJsonObject();
        JsonObject itemsMap = jsonObject.getAsJsonObject("items");

        for (Map.Entry<String, JsonElement> entry : itemsMap.entrySet()) {
            String key = entry.getKey(); // e.g., "Parsnip Seeds_Seed"
            String[] parts = key.split("_");
            if (parts.length == 2) {
                String itemName = parts[0];
                String itemType = parts[1];
                int quantity = context.deserialize(entry.getValue(), int.class);

                Items item = ItemsAdapter.getItemInstance(itemName, itemType); // Use ItemsAdapter to get instance
                if (item != null) {
                    inventory.addItem(item, quantity);
                } else {
                    System.err.println("Warning: Could not deserialize item " + itemName + " of type " + itemType);
                }
            }
        }
        return inventory;
    }
}