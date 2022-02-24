package dansplugins.dpm.utils;

import dansplugins.dpm.factories.ProjectRecordFactory;

public class ProjectRecordInitializer {
    private static ProjectRecordInitializer instance;

    private ProjectRecordInitializer() {

    }

    public static ProjectRecordInitializer getInstance() {
        if (instance == null) {
            instance = new ProjectRecordInitializer();
        }
        return instance;
    }

    public void initializeProjectRecords() {
        createRecord("activitytracker", "https://github.com/Dans-Plugins/Activity-Tracker/releases/download/v1.0/ActivityTracker-v1.0.jar");
        createRecord("alternateaccountfinder", "https://github.com/Dans-Plugins/AlternateAccountFinder/releases/download/v1.1/AlternateAccountFinder-v1.1.jar");
        createRecord("chathub", "https://github.com/Dans-Plugins/ChatHub/releases/download/v0.4/ChatHub-v0.4.jar");
        createRecord("conquestrecipes", "https://www.spigotmc.org/resources/conquest-recipes.83594/download?version=355341");
        createRecord("currencies", "https://github.com/Dans-Plugins/Currencies/releases/download/v1.2/Currencies-1.2.jar");
        createRecord("dansessentials", "https://github.com/Dans-Plugins/Dans-Essentials/releases/download/2.2/Dans-Essentials-2.2.jar");
        createRecord("dansspawnsystem", "https://github.com/Dans-Plugins/Dans-Spawn-System/releases/download/v1.1/Dans-Spawn-System-v1.1.jar");
        createRecord("easylinks", "https://github.com/Dans-Plugins/Easy-Links/releases/tag/v0.2.1");
        createRecord("examplemfexpansion", "https://github.com/Dans-Plugins/Example-MF-Expansion/releases/download/v1.0/ExampleMFExpansion-1.0.jar");
        createRecord("fiefs", "https://github.com/Dans-Plugins/Fiefs/releases/download/v0.10/Fiefs-0.10.jar");
        createRecord("foodspoilage", "https://github.com/Dans-Plugins/FoodSpoilage/releases/download/v2.0/FoodSpoilage-v2.0.jar");
        createRecord("itemdisabler", "https://github.com/Dans-Plugins/ItemDisabler/releases/download/v0.1/ItemDisabler-0.1.jar");
        createRecord("mailboxes", "https://github.com/Dans-Plugins/Mailboxes/releases/download/v1.1/Mailboxes-v1.1.jar");
        createRecord("medievaleconomy", "https://github.com/Dans-Plugins/Medieval-Economy/releases/download/v1.1.1/Medieval-Economy-v1.1.1.jar");
        createRecord("medievalfactions", "https://github.com/Dans-Plugins/Medieval-Factions/releases/download/v4.6.2/Medieval-Factions-4.6.2.jar");
        createRecord("medievalroleplayengine", "https://github.com/Dans-Plugins/Medieval-Roleplay-Engine/releases/download/v1.9/Medieval-Roleplay-Engine-v1.9.jar");
        createRecord("modassist", "https://github.com/Dans-Plugins/ModAssist/releases/download/v1.1/ModAssist-v1.1.jar");
        createRecord("morerecipes", "https://github.com/Dans-Plugins/More-Recipes/releases/download/v1.5/More-Recipes-v1.5.jar");
        createRecord("netheraccesscontroller", "https://github.com/Dans-Plugins/Nether-Access-Controller/releases/download/v1.0.1/NetherAccessController-v1.0.1.jar");
        createRecord("nomorecreepers", "https://github.com/Dans-Plugins/NoMoreCreepers/releases/download/v0.5/NoMoreCreepers-0.5.jar");
        createRecord("playerlore", "https://github.com/Dans-Plugins/PlayerLore/releases/download/v0.4/PlayerLore-0.4.jar");
        createRecord("simpleskills", "https://github.com/Dans-Plugins/SimpleSkills/releases/download/v2.0/SimpleSkills-2.0.jar");
        createRecord("wildpets", "https://github.com/Dans-Plugins/Wild-Pets/releases/download/1.4/WildPets-1.4.jar");
    }

    private void createRecord(String name, String link) {
        ProjectRecordFactory.getInstance().createProjectRecord(name, link);
    }
}