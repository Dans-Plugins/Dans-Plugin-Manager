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
        createRecord("medievalfactions", "https://github.com/Dans-Plugins/Medieval-Factions/releases/download/v4.6.2/Medieval-Factions-4.6.2.jar");
        createRecord("simpleskills", "https://github.com/Dans-Plugins/SimpleSkills/releases/download/v2.0/SimpleSkills-2.0.jar");
        createRecord("wildpets", "https://github.com/Dans-Plugins/Wild-Pets/releases/download/1.4/WildPets-1.4.jar");
        createRecord("currencies", "https://github.com/Dans-Plugins/Currencies/releases/download/v1.2/Currencies-1.2.jar");
        createRecord("foodspoilage", "https://github.com/Dans-Plugins/FoodSpoilage/releases/download/v2.0/FoodSpoilage-v2.0.jar");
    }

    private void createRecord(String name, String link) {
        ProjectRecordFactory.getInstance().createProjectRecord(name, link);
    }
}