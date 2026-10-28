package io.github.projetoalgoritmos.model;

public class RankingEntry {
    private String name;
    private int robotsRepaired;
    private int daysCompleted;
    private float time;

    public RankingEntry(String name, int robotsRepaired, int daysCompleted, float time) {
        this.name = name;
        this.robotsRepaired = robotsRepaired;
        this.daysCompleted = daysCompleted;
        this.time = time;
    }

    public String getName() { return name; }
    public int getRobotsRepaired() { return robotsRepaired; }
    public int getDaysCompleted() { return daysCompleted; }
    public float getTime() { return time; }

    @Override
    public String toString() {
        return String.format("%s: %d Robos | %d Dias | %ds",
            name.toUpperCase(), robotsRepaired, daysCompleted, (int) time);
    }
}
