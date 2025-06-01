package com.example.uspenrolme.student;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
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
import com.example.uspenrolme.adapters.GradesPrintDocumentAdapter;
import com.example.uspenrolme.shared.ErrorFragment;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;

// Import data model classes
import com.example.uspenrolme.models.GradeItem;
import com.example.uspenrolme.models.RegisteredCourseItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.print.PrintManager;
import android.os.Environment;

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

    // Define an interface for fragments displayed in the ViewPager to provide data
    public interface GradesDataProvider {
        List<GradeItem> getCompletedGradesData();
        List<RegisteredCourseItem> getRegisteredCoursesData();
        double getCalculatedGpa();
        int getRegisteredCourseCount();
    }

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

         // Add listener to update GPA and count when tab changes
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                updateGpaAndCountDisplay();
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });

        // Initial display update
         updateGpaAndCountDisplay();
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
                tabLayout.setVisibility(View.VISIBLE);
                 updateGpaAndCountDisplay(); // Update display after data is likely loaded
            }
        });
    }

    private void setupButtonListeners(View view) {
        Button savePdfButton = view.findViewById(R.id.savePdfButton);

        savePdfButton.setOnClickListener(v -> saveAsPdf());
    }

    public void updateGpaAndCountDisplay() {
        Fragment currentFragment = getChildFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + viewPager.getCurrentItem());
        if (currentFragment instanceof GradesDataProvider) {
            GradesDataProvider dataProvider = (GradesDataProvider) currentFragment;
            double gpa = dataProvider.getCalculatedGpa();
            int registeredCount = dataProvider.getRegisteredCourseCount();

            gpaTextView.setText(String.format(Locale.getDefault(), "GPA: %.2f", gpa));
            registeredCountTextView.setText(String.format(Locale.getDefault(), "REGISTERED: %d", registeredCount));
        } else {
            // Handle case where fragment is not a GradesDataProvider (shouldn't happen with our adapter)
            gpaTextView.setText("GPA: N/A");
            registeredCountTextView.setText("REGISTERED: N/A");
        }
    }

    private void showHoldPage() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, new ErrorFragment())
                .commit();
    }

    private void saveAsPdf() {
         Fragment currentFragment = getChildFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + viewPager.getCurrentItem());
         if (currentFragment instanceof GradesDataProvider) {
             GradesDataProvider dataProvider = (GradesDataProvider) currentFragment;
             List<GradeItem> completedGrades = dataProvider.getCompletedGradesData();
             List<RegisteredCourseItem> registeredCourses = dataProvider.getRegisteredCoursesData();
              double gpa = dataProvider.getCalculatedGpa(); // Get GPA from the active fragment

              // Only include grades and GPA if the current tab is Completed
             List<GradeItem> gradesToSave = (currentFragment instanceof CompletedGradesFragment) ? completedGrades : new ArrayList<>();
             List<RegisteredCourseItem> coursesToSave = (currentFragment instanceof RegisteredCoursesFragment) ? registeredCourses : new ArrayList<>();
              double gpaToSave = (currentFragment instanceof CompletedGradesFragment) ? gpa : 0.0; // Only show GPA for completed grades

             PdfDocument document = new PdfDocument();
             try {
                 PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                 PdfDocument.Page page = document.startPage(pageInfo);
                 Canvas canvas = page.getCanvas();

                  // You will need to adapt the drawDocumentContent method in GradesPrintDocumentAdapter
                  // to handle potentially empty lists based on which tab is active.
                 GradesPrintDocumentAdapter pdfAdapter = new GradesPrintDocumentAdapter(requireContext(), gradesToSave, coursesToSave, gpaToSave);
                 pdfAdapter.drawDocumentContent(canvas);

                 document.finishPage(page);

                 File file = createPdfFile();
                 saveDocumentToFile(document, file);
             } catch (Exception e) {
                 Log.e(TAG, "Error generating PDF", e);
                 showErrorToast("Error generating PDF");
             } finally {
                 document.close();
             }
         }
    }

    // The following methods are related to PDF drawing and file handling.
    // They might need adjustments in GradesPrintDocumentAdapter to handle cases where
    // one of the lists (completedGrades or registeredCourses) is empty based on the active tab.

     private File createPdfFile() {
         File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
         String fileName = "Grades_" + sharedPref.getValue_string("userID") + "_" + System.currentTimeMillis() + ".pdf";
         return new File(downloadsDir, fileName);
     }

     private void saveDocumentToFile(PdfDocument document, File file) throws IOException {
         try (FileOutputStream fos = new FileOutputStream(file)) {
             document.writeTo(fos);
             showSuccessToast("PDF saved to Downloads");
         }
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

    private void setupViewPager(View view) {
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

        viewPager.setAdapter(sectionsPagerAdapter);

        tabLayout.setupWithViewPager(viewPager);

        viewPager.setVisibility(View.GONE); // Keep hidden initially until hold check

        viewPager.setCurrentItem(0);
    }
}