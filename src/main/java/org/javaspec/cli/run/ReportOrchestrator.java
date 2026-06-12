package org.javaspec.cli.run;

import org.javaspec.reporting.JUnitXmlReportWriter;
import org.javaspec.reporting.RunReportWriter;
import org.javaspec.runner.RunResult;
import org.javaspec.runner.SpecResult;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;

/**
 * Orchestrates report writing (JSON run report and JUnit XML).
 * <p>Extracted from {@link org.javaspec.cli.Main Main} to isolate reporting
 * logic and enable unit testing.</p>
 */
public final class ReportOrchestrator {
    private static final int EXIT_OK = 0;
    private static final int EXIT_IO_ERROR = 70;

    private ReportOrchestrator() {
    }

    /**
     * Writes requested reports (JSON run report and/or JUnit XML).
     *
     * @param runResult    the run result to report
     * @param reportPath   JSON report file path (may be null)
     * @param junitXmlPath JUnit XML report file path (may be null)
     * @param err          error stream for diagnostic messages
     * @return EXIT_OK on success, EXIT_IO_ERROR on failure
     */
    public static int writeRequested(
            RunResult runResult,
            String reportPath,
            String junitXmlPath,
            PrintStream err
    ) {
        if (reportPath != null) {
            int reportExitCode = writeRunReport(runResult, reportPath, err);
            if (reportExitCode != EXIT_OK) {
                return reportExitCode;
            }
        }
        if (junitXmlPath != null) {
            int reportExitCode = writeJUnitXmlReport(runResult, junitXmlPath, err);
            if (reportExitCode != EXIT_OK) {
                return reportExitCode;
            }
        }
        return EXIT_OK;
    }

    /**
     * Creates an empty {@link RunResult} with no spec results.
     */
    public static RunResult emptyResult() {
        return RunResult.of(Collections.<SpecResult>emptyList());
    }

    private static int writeRunReport(RunResult runResult, String reportPath, PrintStream err) {
        File reportFile = new File(reportPath);
        try {
            RunReportWriter.write(runResult, reportFile);
            return EXIT_OK;
        } catch (IOException ex) {
            err.println("I/O error while writing run report: " + messageOf(ex));
            err.println("Report path: " + reportFile.getPath());
            return EXIT_IO_ERROR;
        } catch (SecurityException ex) {
            err.println("I/O error while writing run report: " + messageOf(ex));
            err.println("Report path: " + reportFile.getPath());
            return EXIT_IO_ERROR;
        }
    }

    private static int writeJUnitXmlReport(RunResult runResult, String junitXmlPath, PrintStream err) {
        File reportFile = new File(junitXmlPath);
        try {
            JUnitXmlReportWriter.write(runResult, reportFile);
            return EXIT_OK;
        } catch (IOException ex) {
            err.println("I/O error while writing JUnit XML report: " + messageOf(ex));
            err.println("JUnit XML path: " + reportFile.getPath());
            return EXIT_IO_ERROR;
        } catch (SecurityException ex) {
            err.println("I/O error while writing JUnit XML report: " + messageOf(ex));
            err.println("JUnit XML path: " + reportFile.getPath());
            return EXIT_IO_ERROR;
        }
    }

    private static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }
}
