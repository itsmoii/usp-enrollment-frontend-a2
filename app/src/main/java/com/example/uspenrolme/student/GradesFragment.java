package com.example.uspenrolme.student;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.example.uspenrolme.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.UtilityService.HoldUtils;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.shared.ErrorFragment;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;

public class GradesFragment extends Fragment {

    private static final String TAG = "GradesFragment";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private SharedPreference sharedPref;
    private RequestQueue requestQueue;

    // UI Components
    private TextView gpaTextView;
    private TextView registeredCountTextView;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    public GradesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_grades, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupRequestQueue();
        checkForHolds();
        setupButtonListeners(view);
        setupViewPager(view);
    }

    private void initializeViews(View view) {
        gpaTextView = view.findViewById(R.id.gpaTextView);
        registeredCountTextView = view.findViewById(R.id.registeredCountTextView);
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
    }

    private void setupRequestQueue() {
        sharedPref = new SharedPreference(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());
    }

    private void checkForHolds() {
        String token = sharedPref.getValue_string("token");
        HoldUtils.checkHold(requireContext(), token, "grades", isBlocked -> {
            if (isBlocked) {
                showHoldPage();
            } else {
                viewPager.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupButtonListeners(View view) {
        Button printButton = view.findViewById(R.id.printButton);
        Button savePdfButton = view.findViewById(R.id.savePdfButton);
    }

    private void setupViewPager(View view) {
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

        viewPager.setAdapter(sectionsPagerAdapter);

        tabLayout.setupWithViewPager(viewPager);

        viewPager.setVisibility(View.GONE);

        viewPager.setCurrentItem(0);
    }

    private void showHoldPage() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, new ErrorFragment())
                .commit();
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void showErrorToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccessToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new CompletedGradesFragment();
                case 1:
                    return new RegisteredCoursesFragment();
                default:
                    return new CompletedGradesFragment();
            }
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Completed";
                case 1:
                    return "Registered";
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}