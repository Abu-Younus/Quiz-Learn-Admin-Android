package munna.hometech.quizlearnadmin;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class QuestionsAdapter extends RecyclerView.Adapter<QuestionsAdapter.ViewHolder> {

    private List<QuestionsModel> questionsModelList;
    private String categoryName;
    private DeleteListener deleteListener;

    public QuestionsAdapter(List<QuestionsModel> questionsModelList, String categoryName, DeleteListener deleteListener) {
        this.questionsModelList = questionsModelList;
        this.categoryName = categoryName;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.questions_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String question = questionsModelList.get(position).getQuestion();
        String answer = questionsModelList.get(position).getCorrectAns();
        holder.setData(question,answer,position);
    }

    @Override
    public int getItemCount() {
        return questionsModelList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvQuestion, tvAnswer;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.question);
            tvAnswer = itemView.findViewById(R.id.answer);
        }

        private void setData(String question, String answer, int position) {
            tvQuestion.setText(position+1+". "+question);
            tvAnswer.setText("Answer: "+answer);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent editIntent = new Intent(itemView.getContext(), AddQuestionActivity.class);
                    editIntent.putExtra("categoryName", categoryName);
                    editIntent.putExtra("setId",questionsModelList.get(position).getSetId());
                    editIntent.putExtra("position",position);
                    itemView.getContext().startActivity(editIntent);
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    deleteListener.onLongClick(position,questionsModelList.get(position).getId());
                    return false;
                }
            });
        }
    }

    public interface DeleteListener {
        void onLongClick(int position,String id);
    }
}
