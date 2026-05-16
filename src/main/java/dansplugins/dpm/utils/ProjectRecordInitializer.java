package dansplugins.dpm.utils;

import dansplugins.dpm.factories.ProjectRecordFactory;
import dansplugins.dpm.objects.ProjectRecord;

import java.util.List;

public class ProjectRecordInitializer {
    private static final String DANS_PLUGINS = "Dans-Plugins";

    private final ProjectRecordFactory projectRecordFactory;

    public ProjectRecordInitializer(ProjectRecordFactory projectRecordFactory) {
        this.projectRecordFactory = projectRecordFactory;
    }

    public void initializeProjectRecords() {
        gh("activitytracker",         "Activity-Tracker",          "Logs player activity including logins, logouts, kills, and deaths.");
        gh("alternateaccountfinder",  "AlternateAccountFinder",    "Detects players using alternate accounts by comparing IP addresses.");
        gh("bluemapmedievalfactions", "Bluemap_MedievalFactions",  "Renders Medieval Factions territory and claims on BlueMap.",
                List.of("medievalfactions"), List.of());
        gh("bookshelvesyoucanuse",    "Bookshelves-You-Can-Use",   "Turns bookshelves into functional storage containers.");
        gh("conquestrecipes",         "Conquest-Recipes",          "Adds conquest-themed crafting recipes.");
        gh("currencies",              "Currencies",                "Adds configurable in-game currencies.",
                List.of("medievalfactions"), List.of());
        gh("dansessentials",          "Dans-Essentials",           "A collection of essential server administration commands.");
        gh("danssethome",             "Dans-Set-Home",             "Lets players set and teleport to personal home locations.");
        gh("dansspawnsystem",         "Dans-Spawn-System",         "Manages server spawn points and first-join spawn assignment.");
        gh("democracy",               "Democracy",                 "Adds in-game voting and democratic governance mechanics.",
                List.of("medievalfactions"), List.of());
        gh("easylinks",               "Easy-Links",                "Posts clickable URLs in chat.");
        gh("fiefs",                   "Fiefs",                     "Adds a land ownership and fiefs system.",
                List.of("medievalfactions"), List.of());
        gh("flycommand",              "FlyCommand",                "Allows operators and permitted players to toggle flight mode.");
        gh("foodspoilage",            "FoodSpoilage",              "Makes food items spoil over time, adding survival challenge.");
        gh("herald",                  "Herald",                    "Announces custom messages on player join and leave.");
        gh("kdrtracker",              "KDRTracker",                "Tracks and displays kill/death ratios for players.");
        gh("mailboxes",               "Mailboxes",                 "Adds physical in-world mailboxes so players can send each other items and messages.");
        gh("medievalcookery",         "Medieval-Cookery",          "Adds medieval-themed cooking recipes and food items.");
        gh("medievaleconomy",         "Medieval-Economy",          "A player-driven economy system with shops and trading.");
        gh("medievalfactions",        "Medieval-Factions",         "A comprehensive factions plugin built for medieval roleplay servers.",
                List.of(), List.of("mailboxes"));
        gh("medievalroleplayengine",  "Medieval-Roleplay-Engine",  "A roleplay engine that ties together DPC plugins for immersive medieval servers.",
                List.of(), List.of("medievalfactions", "mailboxes"));
        gh("minifactions",            "MiniFactions",              "A lightweight factions plugin for servers that want simple territory control.");
        gh("morerecipes",             "More-Recipes",              "Adds a variety of additional crafting recipes.");
        gh("netheraccesscontroller",  "Nether-Access-Controller",  "Controls which players or groups can access the Nether.");
        gh("nomorecreepers",          "NoMoreCreepers",            "Prevents creepers from spawning or dealing explosion damage.");
        gh("playerlore",              "PlayerLore",                "Lets players write and share their character backstory as in-game lore.");
        gh("simpleskills",            "SimpleSkills",              "Adds a skill progression system with levelling for common activities.");
        gh("wildpets",                "Wild-Pets",                 "Lets players tame wild animals and keep them as persistent pets.");
    }

    private void gh(String name, String repo, String description) {
        projectRecordFactory.register(
                ProjectRecord.builder(name, DANS_PLUGINS, repo)
                        .description(description)
                        .build()
        );
    }

    private void gh(String name, String repo, String description,
                    List<String> hardDeps, List<String> softDeps) {
        projectRecordFactory.register(
                ProjectRecord.builder(name, DANS_PLUGINS, repo)
                        .description(description)
                        .hardDependencies(hardDeps)
                        .softDependencies(softDeps)
                        .build()
        );
    }
}
