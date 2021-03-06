import lombok.Builder;

@Builder
public class RailwayTrack {
    private final String trackId;

    private boolean isTrackOccupied;

    public void occupyTrack() {
        isTrackOccupied = true;
    }

    public void freeTrack() {
        isTrackOccupied = false;
    }

    public String getTrackId() {
        return trackId;
    }
}
