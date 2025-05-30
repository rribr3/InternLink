package com.example.internlink;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CompanyAdapter extends RecyclerView.Adapter<CompanyAdapter.CompanyViewHolder> {

    private List<User> companyList;
    private Context context;

    public CompanyAdapter(Context context, List<User> companyList) {
        this.context = context;
        this.companyList = companyList;
    }

    public static class CompanyViewHolder extends RecyclerView.ViewHolder {
        ImageView logo;
        TextView name, industry;

        public CompanyViewHolder(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.company_logo);
            name = itemView.findViewById(R.id.company_name);
            industry = itemView.findViewById(R.id.company_industry);
        }
    }

    @NonNull
    @Override
    public CompanyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_company, parent, false);
        return new CompanyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompanyViewHolder holder, int position) {
        User company = companyList.get(position);
        holder.name.setText(company.getName());
        holder.industry.setText(company.getIndustry());

        // Load logo (optional)
        if (company.getLogoUrl() != null && !company.getLogoUrl().isEmpty()) {
            Glide.with(context).load(company.getLogoUrl()).into(holder.logo);
        }
    }

    @Override
    public int getItemCount() {
        return companyList.size();
    }
}
