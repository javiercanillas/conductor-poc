package io.github.javiercanillas.workers;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class Utils {

    public static void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static String printException(Exception e) {
        try (StringWriter out = new StringWriter(); PrintWriter writer = new PrintWriter(out)) {
            e.printStackTrace(writer);
            writer.flush();
            return out.toString();
        } catch (IOException ioException) {
            // discard ?
            return "Error while printing Exception";
        }
    }
}
