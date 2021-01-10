package org.rockhill.mitm.util.idgenerator;

import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The purpose is to generate a Message ID that is used to mark the messages.
 * Same ID is used for a request and response pairs. Usually it is a timestamp + a 4 digit number.
 * More than 4 digit is possible, but in theory only, as that would mean we have over 10K message pairs in a sec.
 *
 * @author Tamas_Kohegyi
 */
public class TimeStampBasedIdGenerator {

    private static final int NO_DIGITS = 4;
    private final AtomicInteger currentNumber = new AtomicInteger();
    private final CurrentDateProvider currentDateProvider = new CurrentDateProvider();
    private final SimpleDateFormat fileSimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private String previousSimpleDate;


    public synchronized String nextIdentifier() {
        String currentSimpleDate = getCurrentDateFormattedForFiles();
        checkPreviousDate(currentSimpleDate);
        return currentSimpleDate + "." + fourDigitString();
    }

    private String getCurrentDateFormattedForFiles() {
        return fileSimpleDateFormat.format(currentDateProvider.getCurrentDate());
    }

    private void checkPreviousDate(final String currentSimpleDate) {
        if (!currentSimpleDate.equals(previousSimpleDate)) {
            previousSimpleDate = currentSimpleDate;
            currentNumber.set(0);
        }
    }

    private String fourDigitString() {
        String convertedNumber = String.valueOf(currentNumber.getAndIncrement());
        String zeros = createZeros(convertedNumber);
        return zeros + convertedNumber;
    }

    private String createZeros(final String convertedNumber) {
        int size = NO_DIGITS - convertedNumber.length();
        if (size <= 0) {
            return "";
        } else {
            StringBuilder outputBuffer = new StringBuilder(size);
            for (int i = 0; i < size; i++) {
                outputBuffer.append("0");
            }
            return outputBuffer.toString();
        }
    }

}

