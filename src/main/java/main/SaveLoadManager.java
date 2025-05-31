package main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import custom_adapters.InventoryAdapter;
import custom_adapters.ItemsAdapter;
import custom_adapters.PointAdapter;
import custom_adapters.NPCAdapter;
import custom_adapters.CropsPlantedAdapter;
import entity.player.Player;
import entity.npc.NPC;
import items.Items;
import world.CropsPlanted;
import java.awt.Point;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import data.NPCData;
import data.RecipeData;
import world.environment.GameClock;
import world.environment.Season;
import world.environment.Weather;

public class SaveLoadManager {
    private GamePanel gp;
    private Gson gson;
    private ScheduledExecutorService executor;

    public SaveLoadManager(GamePanel gp) {
        this.gp = gp;
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Point.class, new PointAdapter());
        gsonBuilder.registerTypeAdapter(Items.class, new ItemsAdapter());
        gsonBuilder.registerTypeAdapter(Inventory.class, new InventoryAdapter());
        gsonBuilder.registerTypeAdapter(NPC.class, new NPCAdapter());
        gsonBuilder.registerTypeAdapter(CropsPlanted.class, new CropsPlantedAdapter());
        this.gson = gsonBuilder.setPrettyPrinting().create();
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void saveGame(String filePath) {
        executor.submit(() -> {
            try (FileWriter writer = new FileWriter(filePath)) {
                SaveData data = new SaveData();
                data.playerName = gp.playerData.getName();
                data.playerGender = gp.playerData.getGender();
                data.playerEnergy = gp.playerData.getEnergy();
                data.playerGold = gp.playerData.getGold();
                data.playerWorldX = gp.player.worldX;
                data.playerWorldY = gp.player.worldY;
                data.currentMap = gp.currentMap;
                data.equippedItemName = (gp.playerData.getEquppedItem() != null) ? gp.playerData.getEquppedItem().getName() : null;
                data.inventory = gp.playerData.getInventory().checkInventory();
                data.proposingDay = Player.getProposingDay();
                data.partnerName = (gp.playerData.getPartner() != null) ? gp.playerData.getPartner().getNPCName() : null;

                data.farmName = gp.farm.getFarmName();
                data.day = gp.farm.getDay();
                data.currentSeason = gp.farm.getSeason().getCurrentSeason();
                data.currentSeasonDayCounter = gp.farm.getSeason().getDayInSeason() - 1; // Adjust for 0-indexed dayCounter
                data.currentWeather = gp.farm.getWeather().getCurrentWeather();
                data.rainyDaysThisSeason = gp.farm.getWeather().getRainyDaysThisSeason();
                data.gameClockHours = gp.farm.getGameClock().getHours();
                data.gameClockMinutes = gp.farm.getGameClock().getMinutes();
                data.randomMapIndex = gp.randomMapIndex;

                // Save NPC data
                data.npcHeartPoints = new HashMap<>();
                data.npcRelationshipStatus = new HashMap<>();
                for (NPC npc : NPCData.getAllNPC()) {
                    data.npcHeartPoints.put(npc.getNPCName(), npc.getHeartPoints());
                    data.npcRelationshipStatus.put(npc.getNPCName(), npc.getRelationshipStatus());
                }

                // Save planted crops
                data.plantedCrops = gp.farm.getFieldManager().getAllPlantedCrops();

                // Save unlocked recipes
                data.unlockedRecipes = new HashMap<>();
                RecipeData.getAllRecipes().forEach(recipe ->
                    data.unlockedRecipes.put(recipe.getItemID(), recipe.isUnlocked())
                );

                // Save statistics
                data.totalIncome = gp.manager.getTotalIncome();
                data.totalExpenditure = gp.manager.getTotalExpenditure();
                data.totalDaysPlayed = gp.manager.getTotalDaysPlayed();
                data.npcChatFrequencies = gp.manager.getNPCChatFrequencies();
                data.npcGiftFrequencies = gp.manager.getNPCGiftFrequencies();
                data.npcVisitFrequencies = gp.manager.getNPCVisitFrequencies();
                data.totalCropsHarvested = gp.manager.getTotalCropsHarvested();
                data.totalFishCaught = gp.manager.getTotalFishCaught();
                data.totalCommonFishCaught = gp.manager.getTotalCommonFishCaught();
                data.totalRegularFishCaught = gp.manager.getTotalRegularFishCaught();
                data.totalLegendaryFishCaught = gp.manager.getTotalLegendaryFishCaught();
                data.goldMilestoneAchieved = gp.manager.isGoldMilestoneAchieved();
                data.marriageMilestoneAchieved = gp.manager.isMarriageMilestoneAchieved();
                data.milestoneDays = gp.manager.getMilestoneDays();

                gson.toJson(data, writer);
                gp.ui.addMessage("Game saved successfully!");
            } catch (IOException e) {
                gp.ui.addMessage("Failed to save game: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void loadGame(String filePath) {
        executor.submit(() -> {
            try (FileReader reader = new FileReader(filePath)) {
                SaveData data = gson.fromJson(reader, SaveData.class);

                if (data == null) {
                    gp.ui.addMessage("Save file is empty or corrupted.");
                    return;
                }

                // Restore Player data
                gp.playerData.setName(data.playerName);
                gp.playerData.setGender(data.playerGender);
                gp.playerData.setEnergy(data.playerEnergy);
                gp.playerData.setGold(data.playerGold);
                gp.player.worldX = data.playerWorldX;
                gp.player.worldY = data.playerWorldY;
                gp.currentMap = data.currentMap;
                gp.playerData.getInventory().checkInventory().clear(); // Clear current inventory
                for (Map.Entry<Items, Integer> entry : data.inventory.entrySet()) {
                    gp.playerData.getInventory().addItem(entry.getKey(), entry.getValue());
                }
                if (data.equippedItemName != null) {
                    gp.playerData.setEquppedItem(gp.playerData.getInventory().getItemByName(data.equippedItemName));
                } else {
                    gp.playerData.setEquppedItem(null);
                }
                Player.setProposingDay(data.proposingDay);
                if (data.partnerName != null) {
                    gp.playerData.setPartner(NPCData.getNPCByName(data.partnerName));
                } else {
                    gp.playerData.setPartner(null);
                }

                // Restore Farm data
                gp.farm = new Farm(data.farmName, gp.playerData, gp);
                gp.farm.setDay(data.day);
                gp.farm.getSeason().setSeason(data.currentSeason);
                gp.farm.getSeason().setDayCounter(data.currentSeasonDayCounter);
                gp.farm.getWeather().setWeather(data.currentWeather);
                gp.farm.getWeather().setRainyDaysThisSeason(data.rainyDaysThisSeason);
                gp.farm.getGameClock().setHours(data.gameClockHours);
                gp.farm.getGameClock().setMinutes(data.gameClockMinutes);
                gp.randomMapIndex = data.randomMapIndex;
                gp.tileM = new TileManager(gp, gp.randomMapIndex); // Reload tiles based on saved index

                // Restore NPC data
                for (Map.Entry<String, Integer> entry : data.npcHeartPoints.entrySet()) {
                    NPC npc = NPCData.getNPCByName(entry.getKey());
                    if (npc != null) {
                        npc.setHeartPoints(entry.getValue());
                    }
                }
                for (Map.Entry<String, String> entry : data.npcRelationshipStatus.entrySet()) {
                    NPC npc = NPCData.getNPCByName(entry.getKey());
                    if (npc != null) {
                        npc.setRelationshipStatus(entry.getValue());
                    }
                }

                // Restore planted crops
                gp.farm.getFieldManager().setPlantedCrops(data.plantedCrops);
                
                // Restore unlocked recipes
                for (Map.Entry<String, Boolean> entry : data.unlockedRecipes.entrySet()) {
                    RecipeData.getRecipeById(entry.getKey()).setUnlocked(entry.getValue());
                }

                // Restore statistics
                gp.manager.setTotalIncome(data.totalIncome);
                gp.manager.setTotalExpenditure(data.totalExpenditure);
                gp.manager.setTotalDaysPlayed(data.totalDaysPlayed);
                gp.manager.setNpcChatFrequencies(data.npcChatFrequencies);
                gp.manager.setNpcGiftFrequencies(data.npcGiftFrequencies);
                gp.manager.setNpcVisitFrequencies(data.npcVisitFrequencies);
                gp.manager.setTotalCropsHarvested(data.totalCropsHarvested);
                gp.manager.setTotalFishCaught(data.totalFishCaught);
                gp.manager.setTotalCommonFishCaught(data.totalCommonFishCaught);
                gp.manager.setTotalRegularFishCaught(data.totalRegularFishCaught);
                gp.manager.setTotalLegendaryFishCaught(data.totalLegendaryFishCaught);
                gp.manager.trackCurrentPlayerGold(data.playerGold); // This also sets goldMilestoneAchieved
                gp.manager.trackPlayerMarriageStatus(data.marriageMilestoneAchieved);
                gp.manager.setMilestoneDays(data.milestoneDays);

                gp.aSetter.setObject(gp.randomMapIndex); // Re-set objects based on map index
                gp.aSetter.setNPC(); // Re-set NPCs

                gp.gameState = gp.playState;
                gp.ui.addMessage("Game loaded successfully!");
            } catch (IOException e) {
                gp.ui.addMessage("Failed to load game: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                gp.ui.addMessage("Error during game loading: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void shutdownExecutor() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}