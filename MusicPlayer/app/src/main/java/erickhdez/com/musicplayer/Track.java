package erickhdez.com.musicplayer;

/**
 * Created by erickhdez on 15/3/18.
 */

public class Track {
    private String artist;
    private String name;

    public Track(String artist, String name) {
        this.artist = artist;
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public String getName() {
        return name;
    }
}
