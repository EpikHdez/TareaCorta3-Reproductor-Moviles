package erickhdez.com.musicplayer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by erickhdez on 13/3/18.
 */

public class TrackAdapter extends ArrayAdapter<Track> {
    private LayoutInflater inflater;

    public TrackAdapter(@NonNull Context context, @NonNull ArrayList<Track> objects) {
        super(context, android.R.layout.two_line_list_item, objects);

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView nameTextView;
        TextView artistTextView;
        View vi = convertView;

        if(vi == null) {
            vi = inflater.inflate(android.R.layout.two_line_list_item, null);
        }

        nameTextView = vi.findViewById(android.R.id.text1);
        nameTextView.setText(this.getItem(position).getName());

        artistTextView = vi.findViewById(android.R.id.text2);
        artistTextView.setText(this.getItem(position).getArtist());

        return vi;
    }
}
