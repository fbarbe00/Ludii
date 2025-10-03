package manager.api;

import java.util.Objects;

/**
 * A Data Transfer Object (DTO) that represents a game available in Ludii.
 * This class is designed to be simple and serializable, making it easy to
 * pass game information between the core logic and the UI.
 */
public class GameInfo {

    private final String name;
    private final String description;
    private final int minPlayers;
    private final int maxPlayers;
    private final String iconPath;
    private final String filePath;

    public GameInfo(String name, String description, int minPlayers, int maxPlayers, String iconPath, String filePath) {
        this.name = name;
        this.description = description;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.iconPath = iconPath;
        this.filePath = filePath;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameInfo gameInfo = (GameInfo) o;
        return minPlayers == gameInfo.minPlayers &&
                maxPlayers == gameInfo.maxPlayers &&
                Objects.equals(name, gameInfo.name) &&
                Objects.equals(description, gameInfo.description) &&
                Objects.equals(iconPath, gameInfo.iconPath) &&
                Objects.equals(filePath, gameInfo.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, minPlayers, maxPlayers, iconPath, filePath);
    }

    @Override
    public String toString() {
        return "GameInfo{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", minPlayers=" + minPlayers +
                ", maxPlayers=" + maxPlayers +
                ", iconPath='" + iconPath + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}