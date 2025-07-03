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
        holder.projectTitle.setText(applicant.getProjectTitle());

        if (applicant.getProfileUrl() != null && !applicant.getProfileUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(applicant.getProfileUrl())
                    .placeholder(R.drawable.ic_profile)
                    .into(holder.applicantImage);
        }

        boolean hasCertificate = applicant.getCertificateUrl() != null;
        holder.certificatePreviewLayout.setVisibility(hasCertificate ? View.VISIBLE : View.GONE);
        holder.btnViewCertificate.setVisibility(hasCertificate ? View.VISIBLE : View.GONE);
        holder.btnDeleteCertificate.setVisibility(hasCertificate ? View.VISIBLE : View.GONE);

        holder.btnSendCertificate.setVisibility(hasCertificate ? View.GONE : View.VISIBLE);
        holder.certificateSentText.setVisibility(hasCertificate ? View.VISIBLE : View.GONE);

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
        ImageButton btnViewCertificate;
        ImageButton btnDeleteCertificate;
        TextView certificateSentText;

        ViewHolder(View view) {
            super(view);
            applicantImage = view.findViewById(R.id.applicantImage);
            applicantName = view.findViewById(R.id.applicantName);
            projectTitle = view.findViewById(R.id.projectTitle);
            btnSendCertificate = view.findViewById(R.id.btnSendCertificate);
            certificatePreviewLayout = view.findViewById(R.id.certificatePreviewLayout);
            btnViewCertificate = view.findViewById(R.id.btnViewCertificate);
            btnDeleteCertificate = view.findViewById(R.id.btnDeleteCertificate);
            certificateSentText = view.findViewById(R.id.certificateSentText);
        }
    }
}