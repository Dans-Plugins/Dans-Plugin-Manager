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
        gh("chathub",                  "ChatHub");
        gh("currencies",               "Currencies");
        gh("dansessentials",           "Dans-Essentials");
        gh("dansspawnsystem",          "Dans-Spawn-System");
        gh("easylinks",               "Easy-Links");
        gh("fiefs",                    "Fiefs");
        gh("foodspoilage",             "FoodSpoilage");
        gh("mailboxes",               "Mailboxes");
        gh("medievaleconomy",          "Medieval-Economy");
        gh("medievalfactions",         "Medieval-Factions");
        gh("medievalroleplayengine",   "Medieval-Roleplay-Engine");
        gh("morerecipes",              "More-Recipes");
        gh("netheraccesscontroller",   "Nether-Access-Controller");
        gh("nomorecreepers",           "NoMoreCreepers");
        gh("playerlore",              "PlayerLore");
        gh("simpleskills",             "SimpleSkills");
        gh("wildpets",                 "Wild-Pets");

        // Spigot-hosted — no GitHub releases API available
        link("conquestrecipes", "https://www.spigotmc.org/resources/conquest-recipes.83594/download?version=355341");
    }

    private void gh(String name, String repo) {
        projectRecordFactory.createGitHubRecord(name, DANS_PLUGINS, repo);
    }

    private void link(String name, String directLink) {
        projectRecordFactory.createDirectLinkRecord(name, directLink);
    }
}
