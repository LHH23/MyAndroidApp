package com.lhh2333.knowledgeapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class KnowledgeAdapter extends RecyclerView.Adapter<KnowledgeAdapter.ViewHolder> {

    private List<MainActivity.KnowledgeItem> items;

    public KnowledgeAdapter(List<MainActivity.KnowledgeItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_knowledge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MainActivity.KnowledgeItem item = items.get(position);
        holder.tvContent.setText(item.content);
        holder.tvAuthor.setText(item.author != null ? item.author : "匿名");
        holder.tvTime.setText(item.created_at != null ? item.created_at : "");

        // 列表条目渐入动画
        Animation fadeIn = AnimationUtils.loadAnimation(holder.itemView.getContext(), android.R.anim.fade_in);
        holder.itemView.startAnimation(fadeIn);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvAuthor, tvTime;

        ViewHolder(View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }
}