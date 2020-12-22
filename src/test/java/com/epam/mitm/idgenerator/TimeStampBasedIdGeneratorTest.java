package com.epam.mitm.idgenerator;
/*==========================================================================
Copyright since 2013, EPAM Systems
===========================================================================*/

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Test class for TimestampSimpleNumberIdGenerator.
 *
 * @author Marton_Sereg, Tamas Kohegyi
 */
public class TimeStampBasedIdGeneratorTest {

    String nowDateString;
    @InjectMocks
    private TimeStampBasedIdGenerator underTest;
    @Mock
    private CurrentDateProvider currentDateProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Whitebox.setInternalState(underTest, "currentDateProvider", currentDateProvider);
    }

    @Test
    public void testNextIdentifierShouldReturnAProperIdWhenThePreviousTimestampIsNull() {
        // GIVEN
        String expected = "20130624145212.0000";

        Calendar cal = Calendar.getInstance();
        cal.set(2013, 5, 24, 14, 52, 12);
        Date currentDate = cal.getTime();
        given(currentDateProvider.getCurrentDate()).willReturn(currentDate);
        Whitebox.setInternalState(underTest, "previousSimpleDate", null);
        // WHEN
        String actual = underTest.nextIdentifier();
        // THEN
        verify(currentDateProvider).getCurrentDate();
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testNextIdentifierShouldReturnAProperIdWhenThePreviousTimestampDiffersFromTheCurrent() {
        // GIVEN
        String expected = "20130624145212.0000";

        Calendar cal = Calendar.getInstance();
        cal.set(2013, 5, 24, 14, 52, 12);
        Date currentDate = cal.getTime();
        given(currentDateProvider.getCurrentDate()).willReturn(currentDate);
        Whitebox.setInternalState(underTest, "previousSimpleDate", "20130624145211");
        // WHEN
        String actual = underTest.nextIdentifier();
        // THEN
        verify(currentDateProvider).getCurrentDate();
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testNextIdentifierShouldReturnAProperIdWhenThePreviousTimestampEqualsTheCurrent() {
        // GIVEN
        String expected = "20130624145212.0888";

        Calendar cal = Calendar.getInstance();
        cal.set(2013, 5, 24, 14, 52, 12);
        Date currentDate = cal.getTime();
        given(currentDateProvider.getCurrentDate()).willReturn(currentDate);
        Whitebox.setInternalState(underTest, "previousSimpleDate", "20130624145212");
        Whitebox.setInternalState(underTest, "currentNumber", new AtomicInteger(888));
        // WHEN
        String actual = underTest.nextIdentifier();
        // THEN
        verify(currentDateProvider).getCurrentDate();
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testNextIdentifierShouldReturnAProperIdWhenThePreviousIDIsAtTheoreticalMaximum() {
        // GIVEN
        String expected = "20130624145212.10000";

        Calendar cal = Calendar.getInstance();
        cal.set(2013, 5, 24, 14, 52, 12);
        Date currentDate = cal.getTime();
        given(currentDateProvider.getCurrentDate()).willReturn(currentDate);
        Whitebox.setInternalState(underTest, "previousSimpleDate", "20130624145212");
        Whitebox.setInternalState(underTest, "currentNumber", new AtomicInteger(9999));
        // WHEN
        underTest.nextIdentifier();
        String actual = underTest.nextIdentifier();
        // THEN
        Assert.assertEquals(actual, expected);
    }

}
