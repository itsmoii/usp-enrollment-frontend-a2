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

        currentY = drawSeparatorLine(canvas, paint, currentY + SECTION_SPACING);

        if (!grades.isEmpty()) {
             currentY = drawCompletedCoursesSection(canvas, paint, currentY + SECTION_SPACING);
             currentY = drawSeparatorLine(canvas, paint, currentY + SECTION_SPACING);
        }

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

        currentY += SECTION_SPACING;
        canvas.drawText(sharedPref.getValue_string("username"), MARGIN, currentY, paint);
        currentY += LINE_HEIGHT;
        canvas.drawText("PO Box 851", MARGIN, currentY, paint);
        currentY += LINE_HEIGHT;
        canvas.drawText("Nabua, Fiji", MARGIN, currentY, paint);

        return currentY;
    }

    private float drawTitleSection(Canvas canvas, Paint paint, float startY) {
        float currentY = startY;

        paint.setTextSize(TEXT_SIZE_HEADER + 4);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("NOTIFICATION OF EXAM RESULTS", MARGIN, currentY, paint);

        currentY += LINE_HEIGHT * 1.5f;
        paint.setTextSize(TEXT_SIZE_BODY);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Semester 1 2021", MARGIN, currentY, paint);

        currentY += LINE_HEIGHT * 1.5f;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        return currentY + LINE_HEIGHT;
    }

    private float drawCompletedCoursesSection(Canvas canvas, Paint paint, float startY) {
        float currentY = startY;

        // Draw table headers
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Course", MARGIN, currentY, paint);
        canvas.drawText("Title", MARGIN + 100, currentY, paint);
        canvas.drawText("Pass", MARGIN + 400, currentY, paint);
        canvas.drawText("Fail", MARGIN + 480, currentY, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        currentY += LINE_HEIGHT;

        // Draw grades
        for (GradeItem grade : grades) {
            boolean isPass = isPassingGrade(grade.getGrade());

            paint.setColor(isPass ? Color.BLACK : Color.RED);

            canvas.drawText(grade.getCourseCode(), MARGIN, currentY, paint);
            canvas.drawText(shortenTextIfNeeded(grade.getTitle(), 30), MARGIN + 100, currentY, paint);

            if (isPass) {
                canvas.drawText(grade.getGrade(), MARGIN + 400, currentY, paint);
            } else {
                canvas.drawText(grade.getGrade(), MARGIN + 480, currentY, paint);
            }

            currentY += LINE_HEIGHT;
        }

        // Draw GPA
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

        // Draw table headers
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Course", MARGIN, currentY, paint);
        canvas.drawText("Title", MARGIN + 100, currentY, paint);
        canvas.drawText("Status", MARGIN + 450, currentY, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        currentY += LINE_HEIGHT;

        // Draw registered courses
        for (RegisteredCourseItem course : registeredCourses) {
            paint.setColor(course.getStatus().equalsIgnoreCase("Failed") ? Color.RED : Color.BLACK);

            canvas.drawText(course.getCourseCode(), MARGIN, currentY, paint);
            canvas.drawText(shortenTextIfNeeded(course.getTitle(), 30), MARGIN + 100, currentY, paint);

            canvas.drawText(course.getStatus(), MARGIN + 450, currentY, paint);

            currentY += LINE_HEIGHT;
        }

        return currentY;
    }

    private float drawSeparatorLine(Canvas canvas, Paint paint, float startY) {
        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(0.5f);
        canvas.drawLine(MARGIN, startY, PAGE_WIDTH - MARGIN, startY, paint);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(1f);
        return startY;
    }

     private float drawNotesSection(Canvas canvas, Paint paint, float startY) {
         float currentY = startY;

         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
         canvas.drawText("Note:", MARGIN, currentY, paint);
         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
         currentY += LINE_HEIGHT;

         String[] notes = {
                 "1 Fee for reconsideration of course grade is FJ$100",
                 "2 Issued without alterations or erasures",
                 "3 Invalid unless official university stamp appears",
                 "4 English is the medium of teaching in all undergraduate and postgraduate courses at the University of the South Pacific"
         };

         for (String note : notes) {
             canvas.drawText(note, MARGIN, currentY, paint);
             currentY += LINE_HEIGHT;
         }

         return currentY;
     }

     private void drawGradingKeySection(Canvas canvas, Paint paint, float startY) {
         float currentY = startY;

         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
         canvas.drawText("Key to Grading System", MARGIN, currentY, paint);
         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
         currentY += LINE_HEIGHT * 1.5f;

         float leftColX = MARGIN;
         float rightColX = MARGIN + 250;

         // Pass Grades
         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
         canvas.drawText("Pass Grades", leftColX, currentY, paint);
         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
         currentY += LINE_HEIGHT;

         String[] passGrades = {
                 "A+ | Pass with Distinction",
                 "A",
                 "B+ | Pass with Credit",
                 "B",
                 "C+",
                 "C",
                 "R  | Restricted Pass",
                 "Aeq| Aegrotat Pass",
                 "Comp| Compassionate Pass",
                 "Pas | Pass or Competent",
                 "S  | Satisfactory"
         };

         for (String grade : passGrades) {
             canvas.drawText(grade, leftColX, currentY, paint);
             currentY += LINE_HEIGHT;
         }

         currentY = startY + LINE_HEIGHT * 1.5f; // Reset Y for right column

         // Fail Grades
         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
         canvas.drawText("Fail Grades", rightColX, currentY, paint);
         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
         currentY += LINE_HEIGHT;

         String[] failGrades = {
                 "D  | Work below standard required for a pass",
                 "E  | Very weak performance",
                 "NV | Dishonest practice",
                 "U  | Unsatisfactory",
                 "   | Fail", // Keep alignment
                 "\nX  | Not competent",
                  "The letter 'X' when used together with a fail grade indicates that the student",
                  "not sit the final examination in that course."
         };

         for (String grade : failGrades) {
              // Handle potential newlines in the text
              String[] lines = grade.split("\\n");
              for(String line : lines) {
                 canvas.drawText(line, rightColX, currentY, paint);
                 currentY += LINE_HEIGHT;
              }
         }

         currentY += LINE_HEIGHT; // Add some space before Pending Results

         // Pending Results
         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
         canvas.drawText("Pending Results", rightColX, currentY, paint);
         paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
         currentY += LINE_HEIGHT;

         String[] pendingResults = {
                 "I  | Incomplete - student still to complete course requirements",
                 "IP | In Progress - multi-semester course",
                 "NA | Not available - staff member still to submit results"
         };

          for (String result : pendingResults) {
             canvas.drawText(result, rightColX, currentY, paint);
             currentY += LINE_HEIGHT;
         }
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

    private void drawHorizontalLine(Canvas canvas, Paint paint, float y1, float y2) {
        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(0.5f);
        canvas.drawLine(MARGIN, y1, PAGE_WIDTH - MARGIN, y1, paint);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(1f);
    }
}