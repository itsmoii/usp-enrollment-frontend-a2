package com.example.uspenrolme.student.finance;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.uspenrolme.R;

public class FinanceMenu extends Fragment {

    Button invoiceBtn;
    Button paymentsBtn;
    Button holdsBtn;
    Button sponsorBtn;

    public FinanceMenu() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view =  inflater.inflate(R.layout.fragment_finance_menu, container, false);

        invoiceBtn = view.findViewById(R.id.invoice_btn);
        paymentsBtn = view.findViewById(R.id.payments_btn);
        holdsBtn = view.findViewById(R.id.holds_btn);
        sponsorBtn =  view.findViewById(R.id.sponsorship_btn);

        invoiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFragment(new InvoiceFragment());
            }
        });

        paymentsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFragment(new PaymentsFragment());
            }
        });

        holdsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFragment(new HoldsFragment());
            }
        });

        sponsorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFragment(new SponsorshipFragment());
            }
        });

        return view;
    }

    private void openFragment(Fragment fragment){

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, fragment)
                .addToBackStack(null)
                .commit();

    }
}