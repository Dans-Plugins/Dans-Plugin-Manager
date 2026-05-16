package dansplugins.dpm.utils;

import dansplugins.dpm.factories.ProjectRecordFactory;

public class ProjectRecordInitializer {
    private static final String DANS_PLUGINS = "Dans-Plugins";

    private final ProjectRecordFactory projectRecordFactory;

    public ProjectRecordInitializer(ProjectRecordFactory projectRecordFactory) {
        this.projectRecordFactory = projectRecordFactory;
    }

    public void initializeProjectRecords() {
        gh("activitytracker",          "Activity-Tracker");
        gh("alternateaccountfinder",   "AlternateAccountFinder");
        gh("bluemapmedievalfactions",  "Bluemap_MedievalFactions");
        gh("bookshelvesyoucanuse",     "Bookshelves-You-Can-Use");
        gh("conquestrecipes",          "Conquest-Recipes");
        gh("currencies",               "Currencies");
        gh("dansessentials",           "Dans-Essentials");
        gh("danssethome",              "Dans-Set-Home");
        gh("dansspawnsystem",          "Dans-Spawn-System");
        gh("democracy",                "Democracy");
        gh("easylinks",                "Easy-Links");
        gh("fiefs",                    "Fiefs");
        gh("flycommand",               "FlyCommand");
        gh("foodspoilage",             "FoodSpoilage");
        gh("herald",                   "Herald");
        gh("kdrtracker",               "KDRTracker");
        gh("mailboxes",                "Mailboxes");
        gh("medievalcookery",          "Medieval-Cookery");
        gh("medievaleconomy",          "Medieval-Economy");
        gh("medievalfactions",         "Medieval-Factions");
        gh("medievalroleplayengine",   "Medieval-Roleplay-Engine");
        gh("minifactions",             "MiniFactions");
        gh("morerecipes",              "More-Recipes");
        gh("netheraccesscontroller",   "Nether-Access-Controller");
        gh("nomorecreepers",           "NoMoreCreepers");
        gh("playerlore",               "PlayerLore");
        gh("simpleskills",             "SimpleSkills");
        gh("wildpets",                 "Wild-Pets");
    }

    private void gh(String name, String repo) {
        projectRecordFactory.createGitHubRecord(name, DANS_PLUGINS, repo);
    }
}
