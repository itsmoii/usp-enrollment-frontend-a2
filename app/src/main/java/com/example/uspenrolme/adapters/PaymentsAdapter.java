package com.example.uspenrolme.adapters;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uspenrolme.R;
import com.example.uspenrolme.models.PaymentModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PaymentsAdapter extends RecyclerView.Adapter<PaymentsAdapter.MyViewHolder>{

    ArrayList<PaymentModel> arrayList;
    Context context;

    public PaymentsAdapter(Context context, ArrayList<PaymentModel> arrayList) {

        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public PaymentsAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.payments_list_item, parent, false);

        final MyViewHolder myViewHolder = new MyViewHolder(view);

        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {


        final String paymentDate = arrayList.get(position).getDate();
        final double paymentAmount = arrayList.get(position).getAmount();
        final String paymentMethod = arrayList.get(position).getMethod();

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());


            Date date = inputFormat.parse(paymentDate);
            String formattedDate = outputFormat.format(date);


            holder.date.setText(formattedDate);

        } catch (Exception e) {
            e.printStackTrace();
            holder.date.setText(paymentDate);
        }

        holder.amount.setText("$" + String.format("%.2f", paymentAmount));
        holder.method.setText(paymentMethod);

    }



    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{

        TextView date;
        TextView method;
        TextView amount;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            date = (TextView) itemView.findViewById(R.id.payment_date);
            method = (TextView)  itemView.findViewById(R.id.payment_method);
            amount = (TextView) itemView.findViewById(R.id.payment_amount);

        }
    }



}
