package io.github.projetoalgoritmos.model;

public class RankingEntry {
    private String name;
    private int robotsRepaired;
    private int totalComponents;
    private float time;

    public RankingEntry(String name, int robotsRepaired, int totalComponents, float time) {
        this.name = name;
        this.robotsRepaired = robotsRepaired;
        this.totalComponents = totalComponents;
        this.time = time;
    }

    public String getName() { return name; }
    public int getRobotsRepaired() { return robotsRepaired; }
    public int getTotalComponents() { return totalComponents; }
    public float getTime() { return time; }

    @Override
    public String toString() {
        return String.format("%s: %d Robos | %ds", name.toUpperCase(), robotsRepaired, (int)time);
    }
}
