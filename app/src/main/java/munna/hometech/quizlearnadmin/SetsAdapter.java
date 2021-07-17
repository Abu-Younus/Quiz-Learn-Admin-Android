package munna.hometech.quizlearnadmin;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class SetsAdapter extends BaseAdapter {

    public List<String> sets;
    private String categoryName;
    private SetsListener setsListener;

    public SetsAdapter(List<String> sets, String categoryName, SetsListener setsListener) {
        this.sets = sets;
        this.categoryName = categoryName;
        this.setsListener = setsListener;
    }

    @Override
    public int getCount() {
        return sets.size()+1;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sets_item,parent,false);
        } else {
            view = convertView;
        }
        if (position == 0) {
            ((TextView)view.findViewById(R.id.tv_sets_no)).setText("+");
        } else {
            ((TextView) view.findViewById(R.id.tv_sets_no)).setText(String.valueOf(position));
        }
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position == 0) {
                    setsListener.addSets();
                } else {
                    Intent questionIntent = new Intent(parent.getContext(), QuestionsActivity.class);
                    questionIntent.putExtra("categoryName", categoryName);
                    questionIntent.putExtra("setId", sets.get(position-1));
                    parent.getContext().startActivity(questionIntent);
                }
            }
        });
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (position != 0) {
                    setsListener.onLongClick(sets.get(position-1),position);
                }
                return false;
            }
        });
        return view;
    }

    public interface SetsListener {
        public void addSets();
        void onLongClick(String setId, int position);
    }
}
