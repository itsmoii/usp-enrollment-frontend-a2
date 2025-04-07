package com.example.uspenrolme.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uspenrolme.R;
import com.example.uspenrolme.models.InvoiceModel;

import java.util.ArrayList;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.MyViewHolder> {

    ArrayList<InvoiceModel> arrayList;
    Context context;
    public InvoiceAdapter(Context context, ArrayList<InvoiceModel> arrayList) {

        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.invoice_list_item, parent, false);

        final MyViewHolder myViewHolder = new MyViewHolder(view);



        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull InvoiceAdapter.MyViewHolder holder, int position) {

        final String courseCode = arrayList.get(position).getCourseCode();
        final String courseMode = arrayList.get(position).getCourseMode();
        final String courseName = arrayList.get(position).getCourseName();
        final int courseLevel = arrayList.get(position).getCourseLevel();
        final int semester = arrayList.get(position).getSemester();
        final double amount = arrayList.get(position).getPrice();

        holder.courseCode.setText(courseCode);
        holder.courseMode.setText(courseMode);
        holder.courseName.setText(courseName);
        holder.courseLevel.setText(String.valueOf(courseLevel) + "00");
        holder.semester.setText(String.valueOf(courseLevel));
        holder.amount.setText("$" + String.format("%.2f", amount));

    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView courseCode;
        TextView courseMode;
        TextView courseName;
        TextView courseLevel;
        TextView semester;
        TextView amount;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            courseCode = (TextView) itemView.findViewById(R.id.course_code);
            courseMode = (TextView) itemView.findViewById(R.id.course_mode);
            courseName = (TextView) itemView.findViewById(R.id.course_name);
            courseLevel = (TextView) itemView.findViewById(R.id.course_level);
            semester = (TextView) itemView.findViewById(R.id.course_semester);
            amount = (TextView) itemView.findViewById(R.id.course_amount);


        }
    }
}
