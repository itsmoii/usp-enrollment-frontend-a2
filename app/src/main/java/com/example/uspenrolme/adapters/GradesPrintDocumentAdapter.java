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
import com.example.uspenrolme.student.GradesFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GradesPrintDocumentAdapter extends PrintDocumentAdapter {
    private static final String TAG = "GradesPrintAdapter";
    private static final int PAGE_WIDTH = 595;  // A4 width in points
    private static final int PAGE_HEIGHT = 842; // A4 height in points
    private static final int MARGIN = 50;
    private static final int TEXT_SIZE_HEADER = 12;
    private static final int TEXT_SIZE_BODY = 10;
    private static final int LINE_HEIGHT = 20;

    private final Context context;
    private final List<GradesFragment.GradeItem> grades;
    private final List<GradesFragment.RegisteredCourseItem> registeredCourses;
    private final double gpa;
    private PrintedPdfDocument document;
    private PrintAttributes printAttributes;

    public GradesPrintDocumentAdapter(Context context,
                                      List<GradesFragment.GradeItem> grades,
                                      List<GradesFragment.RegisteredCourseItem> registeredCourses,
                                      double gpa) {
        this.context = context;
        this.grades = grades;
        this.registeredCourses = registeredCourses;
        this.gpa = gpa;
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

    private void drawDocumentContent(Canvas canvas) {
        Paint paint = createDefaultPaint();
        float currentY = MARGIN;

        // Draw header section
        currentY = drawHeaderSection(canvas, paint, currentY);

        // Draw completed courses section
        currentY = drawCompletedCoursesSection(canvas, paint, currentY);

        // Draw registered courses section if available
        if (registeredCourses != null && !registeredCourses.isEmpty()) {
            currentY = drawRegisteredCoursesSection(canvas, paint, currentY);
        }

        // Draw GPA section
        drawGpaSection(canvas, paint, currentY + LINE_HEIGHT);
    }

    private Paint createDefaultPaint() {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(TEXT_SIZE_BODY);
        paint.setAntiAlias(true);
        return paint;
    }

    private float drawHeaderSection(Canvas canvas, Paint paint, float startY) {
        SharedPreference sharedPref = new SharedPreference(context);
        String studentName = sharedPref.getValue_string("username");
        String studentId = sharedPref.getValue_string("userID");
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Draw title
        paint.setTextSize(TEXT_SIZE_HEADER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("GRADES REPORT", MARGIN, startY, paint);

        // Draw student info
        paint.setTextSize(TEXT_SIZE_BODY);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Student: " + studentName, MARGIN, startY + LINE_HEIGHT, paint);
        canvas.drawText("ID: " + studentId, MARGIN, startY + (2 * LINE_HEIGHT), paint);
        canvas.drawText("Date: " + currentDate, MARGIN, startY + (3 * LINE_HEIGHT), paint);

        return startY + (4 * LINE_HEIGHT);
    }

    private float drawCompletedCoursesSection(Canvas canvas, Paint paint, float startY) {
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("COMPLETED COURSES", MARGIN, startY + LINE_HEIGHT, paint);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        float currentY = startY + (2 * LINE_HEIGHT);

        // Draw table headers
        canvas.drawText("Term", MARGIN, currentY, paint);
        canvas.drawText("Course", MARGIN + 100, currentY, paint);
        canvas.drawText("Title", MARGIN + 200, currentY, paint);
        canvas.drawText("Grade", MARGIN + 450, currentY, paint);

        currentY += LINE_HEIGHT;

        // Draw grades
        for (GradesFragment.GradeItem grade : grades) {
            paint.setColor(grade.getGrade().equalsIgnoreCase("F") ? Color.RED : Color.BLACK);

            canvas.drawText(grade.getTerm(), MARGIN, currentY, paint);
            canvas.drawText(grade.getCourseCode(), MARGIN + 100, currentY, paint);
            canvas.drawText(shortenTextIfNeeded(grade.getTitle(), 30), MARGIN + 200, currentY, paint);
            canvas.drawText(grade.getGrade(), MARGIN + 450, currentY, paint);

            currentY += LINE_HEIGHT;
        }

        return currentY + LINE_HEIGHT;
    }

    private float drawRegisteredCoursesSection(Canvas canvas, Paint paint, float startY) {
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("REGISTERED COURSES", MARGIN, startY + LINE_HEIGHT, paint);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        float currentY = startY + (2 * LINE_HEIGHT);

        // Draw table headers
        canvas.drawText("Term", MARGIN, currentY, paint);
        canvas.drawText("Course", MARGIN + 100, currentY, paint);
        canvas.drawText("Title", MARGIN + 200, currentY, paint);
        canvas.drawText("Status", MARGIN + 450, currentY, paint);

        currentY += LINE_HEIGHT;

        // Draw registered courses
        for (GradesFragment.RegisteredCourseItem course : registeredCourses) {
            paint.setColor(course.getStatus().equalsIgnoreCase("Failed") ? Color.RED : Color.BLACK);

            canvas.drawText(course.getTerm(), MARGIN, currentY, paint);
            canvas.drawText(course.getCourseCode(), MARGIN + 100, currentY, paint);
            canvas.drawText(shortenTextIfNeeded(course.getTitle(), 30), MARGIN + 200, currentY, paint);
            canvas.drawText(course.getStatus(), MARGIN + 450, currentY, paint);

            currentY += LINE_HEIGHT;
        }

        return currentY + LINE_HEIGHT;
    }

    private void drawGpaSection(Canvas canvas, Paint paint, float y) {
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setColor(Color.BLACK);
        canvas.drawText("Cumulative GPA: " + String.format(Locale.getDefault(), "%.2f", gpa),
                MARGIN, y, paint);
    }

    private String shortenTextIfNeeded(String text, int maxLength) {
        if (text.length() > maxLength) {
            return text.substring(0, maxLength - 3) + "...";
        }
        return text;
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