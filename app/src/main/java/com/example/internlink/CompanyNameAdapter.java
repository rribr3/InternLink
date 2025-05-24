package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CompanyNameAdapter extends RecyclerView.Adapter<CompanyNameAdapter.CompanyViewHolder> {

    private final List<String> companyNames;

    public CompanyNameAdapter(List<String> companyNames) {
        this.companyNames = companyNames;
    }

    @NonNull
    @Override
    public CompanyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_company_name, parent, false);
        return new CompanyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompanyViewHolder holder, int position) {
        String name = companyNames.get(position);
        holder.tvCompanyName.setText(name);

        holder.itemView.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "Selected Company: " + name, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public int getItemCount() {
        return companyNames.size();
    }

    static class CompanyViewHolder extends RecyclerView.ViewHolder {
        TextView tvCompanyName;

        CompanyViewHolder(View itemView) {
            super(itemView);
            tvCompanyName = itemView.findViewById(R.id.tv_company_name);
        }
    }
}
