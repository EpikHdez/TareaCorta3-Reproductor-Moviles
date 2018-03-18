package erickhdez.com.musicplayer;

import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    private ListView tracksListView;
    private TextView timeEndTextView;
    private TextView currentTime;

    private SeekBar trackProgressSeekBar;
    private SeekBar volumeSeekBar;

    private ArrayList<String> tracksList;
    private ArrayList<Track> tracksListUI;

    private AssetManager assetManager;
    private TextView lyricsTextView;

    private int currentTrackIndex;
    private List<List<String>> lyricsTimes;
    private List<String> lyrics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentTime = findViewById(R.id.txtCurrentTime);
        timeEndTextView = findViewById(R.id.txtTimeEnd);

        lyricsTextView = findViewById(R.id.lyricsTextView);
        lyricsTextView.setText("");

        audioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
        assetManager = getAssets();

        setUpTracksListView();
        setUpSeekBars();

        changeTrack(0);
        setUpUpdateTimer();
        mediaPlayer.stop();
    }

    @Override
    protected void onPause() {
        mediaPlayer.pause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mediaPlayer.pause();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaPlayer.start();
    }

    /**
     *
     */
    private void setUpSeekBars() {
        trackProgressSeekBar = findViewById(R.id.trackProgressSeekBar);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);

        trackProgressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    currentTime.setText(getTimeAsString(progress));
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mediaPlayer.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayer.start();
            }
        });

        volumeSeekBar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volumeSeekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int volume, boolean fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.ADJUST_SAME);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    /**
     *
     */
    private void setUpTracksListView() {
        tracksListView = findViewById(R.id.tracksListView);
        Field[] tracks = R.raw.class.getFields();

        formatTracks(tracks);

        TrackAdapter adapter = new TrackAdapter(this, tracksListUI);
        tracksListView.setAdapter(adapter);

        tracksListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                currentTrackIndex = i;
                changeTrack(i);
            }
        });
    }

    /**
     *
     * @param tracks
     */
    private void formatTracks(Field[] tracks) {
        tracksList = new ArrayList<>();
        tracksListUI = new ArrayList<>();
        String trackName;

        for(Field track : tracks) {
            trackName = track.getName();
            Log.d("TrackName", "formatTracks: " + trackName);

            tracksListUI.add(formatTrackInfo(trackName.split("0")));
            tracksList.add(trackName);
        }
    }

    /**
     *
     * @param trackInfo
     * @return
     */
    private Track formatTrackInfo(String[] trackInfo) {
        String[] trackSubInfo;
        String tempValue = "";
        char firstLetter;

        for(int i = 0; i < trackInfo.length; i++) {
            for(String word : trackInfo[i].split("_")) {
                word = word.replace("1", "'");
                firstLetter = word.charAt(0);
                firstLetter = Character.toUpperCase(firstLetter);

                tempValue += String.format("%c%s ", firstLetter, word.substring(1));
            }

            tempValue += "-";
        }

        trackSubInfo = tempValue.split("-");

        return new Track(trackSubInfo[0], trackSubInfo[1]);
    }

    /**
     *
     * @param trackPosition
     */
    private void changeTrack(int trackPosition) {
        if(trackPosition < 0) {
            currentTrackIndex++;
            Toast.makeText(this, R.string.no_previous, Toast.LENGTH_SHORT).show();
            return;
        }

        if(trackPosition >= tracksList.size()) {
            currentTrackIndex--;
            Toast.makeText(this, R.string.no_next, Toast.LENGTH_SHORT).show();
            return;
        }

        if(mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        String trackName = tracksList.get(trackPosition);
        int trackId = getResources().getIdentifier(trackName, "raw", getPackageName());
        mediaPlayer = MediaPlayer.create(this, trackId);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                changeTrack(++currentTrackIndex);
                changeBtnPlayPauseImage(android.R.drawable.ic_media_play);
            }
        });

        openTrackLyrics(trackName);
        updateViewsForTrack();
        mediaPlayer.start();
        changeBtnPlayPauseImage(android.R.drawable.ic_media_pause);
    }

    private void openTrackLyrics(String trackName) {
        lyricsTimes = null;
        lyrics = null;

        try {
            InputStream inputStream = assetManager.open(String.format("%s.txt", trackName));
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            String[] lineParams;
            String lineLyrics;

            lyricsTimes = new LinkedList<>();
            lyrics = new LinkedList<>();

            while ((line = bufferedReader.readLine()) != null) {
                line = line.replace("[", "");
                lineParams = line.split("]");

                lineLyrics = lineParams[lineParams.length - 1];
                lineParams = Arrays.copyOf(lineParams, lineParams.length - 1);

                lyricsTimes.add(Arrays.asList(lineParams));
                lyrics.add(lineLyrics);
            }
        } catch(Exception e) {

        }
    }

    /**
     *
     */
    private void updateViewsForTrack() {
        currentTime.setText(R.string.zero_zero_zero);
        timeEndTextView.setText(getTimeAsString(mediaPlayer.getDuration()));

        trackProgressSeekBar.setMax(mediaPlayer.getDuration());
        trackProgressSeekBar.setProgress(0);

        lyricsTextView.setText("");
    }

    /**
     *
     */
    private void setUpUpdateTimer() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            int progress;
            String time, lyric;
            int index;

            @Override
            public void run() {
                if(mediaPlayer.isPlaying()) {
                    progress = mediaPlayer.getCurrentPosition();
                    time = getTimeAsString(progress);
                    index = 0;

                    if(lyricsTimes != null) {
                        for (List<String> times : lyricsTimes) {
                            if (times.contains(time)) {
                                lyric = lyrics.get(index);
                                break;
                            }

                            index++;
                        }
                    } else {
                        lyric = "";
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            trackProgressSeekBar.setProgress(progress);
                            currentTime.setText(time);
                            lyricsTextView.setText(lyric);
                        }
                    });
                }
            }
        }, 0, 1000 );
    }

    /**
     *
     * @param milliseconds
     * @return
     */
    private String getTimeAsString(int milliseconds) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds));

        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     *
     * @param view
     */
    public void onButtonClicked(View view) {
        switch(view.getId()) {
            case R.id.btnPrevious:
                changeTrack(--currentTrackIndex);
                break;

            case R.id.btnPlayPause:
                changeMediaPlayerState();
                break;

            case R.id.btnNext:
                changeTrack(++currentTrackIndex);
                break;

            default:
                return;
        }
    }

    /**
     *
     */
    private void changeMediaPlayerState() {
        if(mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            changeBtnPlayPauseImage(android.R.drawable.ic_media_play);
        } else {
            mediaPlayer.start();
            changeBtnPlayPauseImage(android.R.drawable.ic_media_pause);
        }
    }

    /**
     *
     * @param resourceImage
     */
    private void changeBtnPlayPauseImage(int resourceImage) {
        Button btnPlayPause = findViewById(R.id.btnPlayPause);
        btnPlayPause.setBackgroundResource(resourceImage);
    }
}
