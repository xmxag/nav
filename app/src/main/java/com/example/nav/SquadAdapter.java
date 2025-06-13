package com.example.nav;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SquadAdapter extends RecyclerView.Adapter<SquadAdapter.SquadViewHolder> {
    private List<Squad> squads;
    private Context context;
    private OnSquadRemoveListener removeListener;

    public interface OnSquadRemoveListener {
        void onRemove(Squad squad);
    }

    public SquadAdapter(Context context, List<Squad> squads, OnSquadRemoveListener listener) {
        this.context = context;
        this.squads = squads;
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public SquadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_squad, parent, false);
        return new SquadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SquadViewHolder holder, int position) {
        Squad squad = squads.get(position);
        holder.textViewName.setText(squad.getName());
        holder.textViewDirection.setText(squad.getDirection());
        holder.buttonRemove.setOnClickListener(v -> removeListener.onRemove(squad));
    }

    @Override
    public int getItemCount() {
        return squads.size();
    }

    static class SquadViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewDirection;
        Button buttonRemove;

        SquadViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewSquadName);
            textViewDirection = itemView.findViewById(R.id.textViewSquadDirection);
            buttonRemove = itemView.findViewById(R.id.buttonRemoveSquad);
        }
    }
}