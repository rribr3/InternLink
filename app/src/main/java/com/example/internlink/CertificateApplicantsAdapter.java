package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CertificateApplicantsAdapter extends RecyclerView.Adapter<CertificateApplicantsAdapter.ViewHolder> {

    private List<CompletedApplicant> applicants;
    private OnCertificateActionListener listener;
    private CompletedApplicant selectedApplicant;

    public interface OnCertificateActionListener {
        void onAction(CompletedApplicant applicant, String action);
    }

    public CertificateApplicantsAdapter(List<CompletedApplicant> applicants, OnCertificateActionListener listener) {
        this.applicants = applicants;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_certificate_applicant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CompletedApplicant applicant = applicants.get(position);

        holder.applicantName.setText(applicant.getName());

        if (applicant.getProfileUrl() != null && !applicant.getProfileUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(applicant.getProfileUrl())
                    .placeholder(R.drawable.ic_profile)
                    .into(holder.applicantImage);
        }

        // Show/hide certificate preview based on whether certificate exists
        holder.certificatePreviewLayout.setVisibility(
                applicant.getCertificateUrl() != null ? View.VISIBLE : View.GONE);

        holder.btnSendCertificate.setOnClickListener(v ->
                listener.onAction(applicant, "send"));

        holder.btnViewCertificate.setOnClickListener(v ->
                listener.onAction(applicant, "view"));

        holder.btnDeleteCertificate.setOnClickListener(v ->
                listener.onAction(applicant, "delete"));
    }

    @Override
    public int getItemCount() {
        return applicants.size();
    }

    public void setSelectedApplicant(CompletedApplicant applicant) {
        this.selectedApplicant = applicant;
    }

    public CompletedApplicant getSelectedApplicant() {
        return selectedApplicant;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView applicantImage;
        TextView applicantName;
        TextView projectTitle;
        Button btnSendCertificate;
        LinearLayout certificatePreviewLayout;
        TextView certificateFileName;
        ImageButton btnViewCertificate;
        ImageButton btnDeleteCertificate;

        ViewHolder(View view) {
            super(view);
            applicantImage = view.findViewById(R.id.applicantImage);
            applicantName = view.findViewById(R.id.applicantName);
            projectTitle = view.findViewById(R.id.projectTitle);
            btnSendCertificate = view.findViewById(R.id.btnSendCertificate);
            certificatePreviewLayout = view.findViewById(R.id.certificatePreviewLayout);
            certificateFileName = view.findViewById(R.id.certificateFileName);
            btnViewCertificate = view.findViewById(R.id.btnViewCertificate);
            btnDeleteCertificate = view.findViewById(R.id.btnDeleteCertificate);
        }
    }
}