package com.example.uspenrolme.adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.util.Log;

import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.R;
import com.example.uspenrolme.models.GradeItem;
import com.example.uspenrolme.models.RegisteredCourseItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

public class GradesPrintDocumentAdapter extends PrintDocumentAdapter {
    private static final String TAG = "GradesPrintAdapter";
    private static final int PAGE_WIDTH = 595;  // A4 width in points
    private static final int PAGE_HEIGHT = 842; // A4 height in points
    private static final int MARGIN = 50;
    private static final int TEXT_SIZE_HEADER = 12;
    private static final int TEXT_SIZE_BODY = 10;
    private static final int LINE_HEIGHT = 15;
    private static final int SECTION_SPACING = 25;

    private final Context context;
    private final List<GradeItem> grades;
    private final List<RegisteredCourseItem> registeredCourses;
    private final double gpa;
    private PrintedPdfDocument document;
    private PrintAttributes printAttributes;
    private SharedPreference sharedPref;

    public GradesPrintDocumentAdapter(Context context,
                                      List<GradeItem> grades,
                                      List<RegisteredCourseItem> registeredCourses,
                                      double gpa) {
        this.context = context;
        this.grades = grades != null ? grades : new ArrayList<>();
        this.registeredCourses = registeredCourses != null ? registeredCourses : new ArrayList<>();
        this.gpa = gpa;
        this.sharedPref = new SharedPreference(context);
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes,
                         PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal,
                         LayoutResultCallback callback,
                         Bundle extras) {
        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        this.printAttributes = newAttributes;

        PrintDocumentInfo info = new PrintDocumentInfo.Builder("grades_report.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build();

        boolean changed = !newAttributes.equals(oldAttributes);
        callback.onLayoutFinished(info, changed);
    }

    @Override
    public void onWrite(PageRange[] pages,
                        ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal,
                        WriteResultCallback callback) {
        try {
            document = new PrintedPdfDocument(context, printAttributes);
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH, PAGE_HEIGHT, 1).create();

            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            try {
                drawDocumentContent(canvas);
                document.finishPage(page);

                writeToFile(destination, callback);
            } catch (Exception e) {
                callback.onWriteFailed(e.getMessage());
                Log.e(TAG, "Error writing PDF content", e);
            }
        } catch (Exception e) {
            callback.onWriteFailed(e.getMessage());
            Log.e(TAG, "Error creating PDF document", e);
        } finally {
            closeDocument();
        }
    }

    public void drawDocumentContent(Canvas canvas) {
        Paint paint = createDefaultPaint();
        float currentY = MARGIN;

        currentY = drawHeaderSection(canvas, paint, currentY);

        currentY = drawTitleSection(canvas, paint, currentY + SECTION_SPACING);

        if (!grades.isEmpty()) {
             currentY = drawCompletedCoursesSection(canvas, paint, currentY + SECTION_SPACING);
        }

         currentY = drawSeparatorLine(canvas, paint, currentY + SECTION_SPACING);

         currentY = drawNotesSection(canvas, paint, currentY + SECTION_SPACING);

         drawGradingKeySection(canvas, paint, currentY + SECTION_SPACING);
    }

    private Paint createDefaultPaint() {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(TEXT_SIZE_BODY);
        paint.setAntiAlias(true);
        return paint;
    }

    private float drawHeaderSection(Canvas canvas, Paint paint, float startY) {
        paint.setTextSize(TEXT_SIZE_BODY);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        float currentY = startY;
        canvas.drawText("Our Ref: S11195667", MARGIN, currentY, paint);
        currentY += LINE_HEIGHT;
        canvas.drawText("Laucala", MARGIN, currentY, paint);
        currentY += LINE_HEIGHT;
        canvas.drawText("Date: " + getCurrentDate(), MARGIN, currentY, paint);

        currentY += LINE_HEIGHT * 2;
        canvas.drawText(sharedPref.getValue_string("username"), MARGIN, currentY, paint);
        currentY += LINE_HEIGHT;
        canvas.drawText("PO Box 851", MARGIN, currentY, paint);
        currentY += LINE_HEIGHT;
        canvas.drawText("Nabua, Fiji", MARGIN, currentY, paint);

        return Math.max(currentY, startY + (LINE_HEIGHT * 6));
    }

    private float drawTitleSection(Canvas canvas, Paint paint, float startY) {
        paint.setTextSize(TEXT_SIZE_HEADER + 4);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("NOTIFICATION OF EXAM RESULTS", MARGIN, startY, paint);

        paint.setTextSize(TEXT_SIZE_BODY);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Semester 1 2021", MARGIN, startY + LINE_HEIGHT * 1.5f, paint);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Program: Bachelor of Science (majoring in Electrical/Electro Engeering and Computing Science)", MARGIN, startY + LINE_HEIGHT * 3f, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        return startY + LINE_HEIGHT * 4;
    }

    private float drawCompletedCoursesSection(Canvas canvas, Paint paint, float startY) {
        float currentY = startY;

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Campus", MARGIN, currentY, paint);
        canvas.drawText("Course", MARGIN + 50, currentY, paint);
        canvas.drawText("Title", MARGIN + 150, currentY, paint);
        canvas.drawText("Pass", MARGIN + 400, currentY, paint);
        canvas.drawText("Fail", MARGIN + 480, currentY, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        currentY += LINE_HEIGHT;

        for (GradeItem grade : grades) {
            boolean isPass = isPassingGrade(grade.getGrade());

            paint.setColor(isPass ? Color.BLACK : Color.RED);

            canvas.drawText(grade.getCampus(), MARGIN, currentY, paint);
            canvas.drawText(grade.getCourseCode(), MARGIN + 50, currentY, paint);
            canvas.drawText(shortenTextIfNeeded(grade.getTitle(), 30), MARGIN + 150, currentY, paint);

            if (isPass) {
                canvas.drawText(grade.getGrade(), MARGIN + 400, currentY, paint);
            } else {
                canvas.drawText(grade.getGrade(), MARGIN + 480, currentY, paint);
            }

            currentY += LINE_HEIGHT;
        }

        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("GPA: " + String.format(Locale.getDefault(), "%.2f", gpa), MARGIN, currentY + LINE_HEIGHT, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        return currentY + LINE_HEIGHT * 2;
    }

    private boolean isPassingGrade(String grade) {
        if (grade == null) return false;
        String upperGrade = grade.toUpperCase();
        return upperGrade.equals("A+") || upperGrade.equals("A") || upperGrade.equals("B+") ||
               upperGrade.equals("B") || upperGrade.equals("C+") || upperGrade.equals("C") ||
               upperGrade.equals("S") || upperGrade.equals("P");
    }

    private float drawRegisteredCoursesSection(Canvas canvas, Paint paint, float startY) {
         float currentY = startY;

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Campus", MARGIN, currentY, paint);
        canvas.drawText("Course", MARGIN + 50, currentY, paint);
        canvas.drawText("Title", MARGIN + 150, currentY, paint);
        canvas.drawText("Status", MARGIN + 450, currentY, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        currentY += LINE_HEIGHT;

        for (RegisteredCourseItem course : registeredCourses) {
            paint.setColor(course.getStatus().equalsIgnoreCase("Failed") ? Color.RED : Color.BLACK);

            canvas.drawText(course.getCampus(), MARGIN, currentY, paint);
            canvas.drawText(course.getCourseCode(), MARGIN + 50, currentY, paint);
            canvas.drawText(shortenTextIfNeeded(course.getTitle(), 30), MARGIN + 150, currentY, paint);

            canvas.drawText(course.getStatus(), MARGIN + 450, currentY, paint);

            currentY += LINE_HEIGHT;
        }

        return currentY;
    }

    private float drawSeparatorLine(Canvas canvas, Paint paint, float startY) {
         paint.setColor(Color.BLACK);
         paint.setStrokeWidth(1);
         canvas.drawLine(MARGIN, startY, PAGE_WIDTH - MARGIN, startY, paint);
         return startY;
    }

     private float drawNotesSection(Canvas canvas, Paint paint, float startY) {
         float currentY = startY;

         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
         canvas.drawText("Note:", MARGIN, currentY, paint);
         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

         currentY += LINE_HEIGHT;

         canvas.drawText("1 Fee for reconsideration of course grade is FJ$100", MARGIN, currentY, paint);
         currentY += LINE_HEIGHT;
         canvas.drawText("2 Issued without alterations or erasures", MARGIN, currentY, paint);
         currentY += LINE_HEIGHT;
         canvas.drawText("3 Invalid unless official university stamp appears", MARGIN, currentY, paint);
         currentY += LINE_HEIGHT;
         canvas.drawText("4 English is the medium of teaching in all undergraduate and postgraduate courses at the University of the South Pacific", MARGIN, currentY, paint);

         return currentY;
     }

     private void drawGradingKeySection(Canvas canvas, Paint paint, float startY) {
         float currentY = startY;

         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
         canvas.drawText("Key to Grading System", MARGIN, currentY, paint);
         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

         currentY += LINE_HEIGHT;

         float column1X = MARGIN;
         float column2X = MARGIN + 250;

         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
         canvas.drawText("Pass Grades", column1X, currentY, paint);
         canvas.drawText("Fail Grades", column2X, currentY, paint);
         canvas.drawText("Pending Results", column2X + 150, currentY, paint);
         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

         currentY += LINE_HEIGHT;

         canvas.drawText("A+", column1X, currentY, paint);
         canvas.drawText("A", column1X, currentY + LINE_HEIGHT, paint);
         canvas.drawText("B+", column1X, currentY + LINE_HEIGHT * 2, paint);
         canvas.drawText("B", column1X, currentY + LINE_HEIGHT * 3, paint);
         canvas.drawText("C+", column1X, currentY + LINE_HEIGHT * 4, paint);
         canvas.drawText("C", column1X, currentY + LINE_HEIGHT * 5, paint);
         canvas.drawText("R", column1X, currentY + LINE_HEIGHT * 6, paint);
         canvas.drawText("Aeg", column1X, currentY + LINE_HEIGHT * 7, paint);
         canvas.drawText("Comp", column1X, currentY + LINE_HEIGHT * 8, paint);
         canvas.drawText("Pas", column1X, currentY + LINE_HEIGHT * 9, paint);
         canvas.drawText("S", column1X, currentY + LINE_HEIGHT * 10, paint);

         canvas.drawText("}", column1X + 20, currentY + LINE_HEIGHT * 0.5f, paint);
         canvas.drawText("Pass with Distinction", column1X + 35, currentY + LINE_HEIGHT * 0.5f, paint);
         canvas.drawText("}", column1X + 20, currentY + LINE_HEIGHT * 2.5f, paint);
         canvas.drawText("Pass with Credit", column1X + 35, currentY + LINE_HEIGHT * 2.5f, paint);
         canvas.drawText("}", column1X + 20, currentY + LINE_HEIGHT * 5.5f, paint);
         canvas.drawText("Pass", column1X + 35, currentY + LINE_HEIGHT * 5.5f, paint);
         canvas.drawText("Restricted Pass", column1X + 35, currentY + LINE_HEIGHT * 6, paint);
         canvas.drawText("Aegrotat Pass", column1X + 35, currentY + LINE_HEIGHT * 7, paint);
         canvas.drawText("Compassionate Pass", column1X + 35, currentY + LINE_HEIGHT * 8, paint);
         canvas.drawText("Pass or Competent", column1X + 35, currentY + LINE_HEIGHT * 9, paint);
         canvas.drawText("Satisfactory", column1X + 35, currentY + LINE_HEIGHT * 10, paint);

         canvas.drawText("D", column2X, currentY, paint);
         canvas.drawText("E", column2X, currentY + LINE_HEIGHT, paint);
         canvas.drawText("NV", column2X, currentY + LINE_HEIGHT * 2, paint);
         canvas.drawText("U", column2X, currentY + LINE_HEIGHT * 3, paint);
         canvas.drawText("Fail", column2X, currentY + LINE_HEIGHT * 4, paint);
         canvas.drawText("Not competent", column2X + 35, currentY + LINE_HEIGHT * 5, paint);
         canvas.drawText("The letter 'X' when used together with a fail grade indicates that the student did", column2X, currentY + LINE_HEIGHT * 6, paint);
         canvas.drawText("not sit the final examination in that course", column2X, currentY + LINE_HEIGHT * 7, paint);

         canvas.drawText("I", column2X + 150, currentY + LINE_HEIGHT * 8, paint);
         canvas.drawText("IP", column2X + 150, currentY + LINE_HEIGHT * 9, paint);
         canvas.drawText("NA", column2X + 150, currentY + LINE_HEIGHT * 10, paint);

         canvas.drawText("Work below standard required for a pass", column2X + 20, currentY, paint);
         canvas.drawText("Very weak performance", column2X + 20, currentY + LINE_HEIGHT, paint);
         canvas.drawText("Dishonest practice", column2X + 20, currentY + LINE_HEIGHT * 2, paint);
         canvas.drawText("Unsatisfactory", column2X + 20, currentY + LINE_HEIGHT * 3, paint);

         canvas.drawText("Incomplete - student still to complete assessment element", column2X + 170, currentY + LINE_HEIGHT * 8, paint);
         canvas.drawText("In Progress - multi-semester thesis or dissertation", column2X + 170, currentY + LINE_HEIGHT * 9, paint);
         canvas.drawText("Not available - staff member still to confirm final grade", column2X + 170, currentY + LINE_HEIGHT * 10, paint);
     }

    private String shortenTextIfNeeded(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() > maxLength) {
            return text.substring(0, maxLength - 3) + "...";
        }
        return text;
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void writeToFile(ParcelFileDescriptor destination, WriteResultCallback callback)
            throws IOException {
        try (FileOutputStream output = new FileOutputStream(destination.getFileDescriptor())) {
            document.writeTo(output);
            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
        }
    }

    private void closeDocument() {
        if (document != null) {
            document.close();
            document = null;
        }
    }

    @Override
    public void onFinish() {
        super.onFinish();
        closeDocument();
    }
}