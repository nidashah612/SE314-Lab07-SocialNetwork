/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class FilterTest {

	/*
	 * Testing strategy
	 *
	 * writtenBy(tweets, username):
	 *   partition on number of tweets: 0, 1, many
	 *   partition on number of matching tweets: 0, 1, many
	 *   partition on case of username vs author: same, different
	 *   partition on order: result keeps the input order
	 *
	 * inTimespan(tweets, timespan):
	 *   partition on number of tweets: 0, 1, many
	 *   partition on tweet time vs timespan: before, at start, inside, at end, after
	 *   partition on timespan length: zero-length, positive
	 *   partition on order: result keeps the input order
	 *
	 * containing(tweets, words):
	 *   partition on number of words: 0, 1, many
	 *   partition on match: none, one word matches, several words match
	 *   partition on case: same, different
	 *   partition on position of word in text: start, middle, end
	 *   partition on word inside a longer word (talk / talking): yes, no
	 *   partition on word next to punctuation (much / much?): yes, no
	 *   partition on order: result keeps the input order
	 *
	 * Choosing test cases: one test for each partition value, plus edge cases
	 * like an empty list and timestamps exactly at the ends of the timespan.
	 */
    
    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    
    private static final Tweet tweet1 = new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet tweet2 = new Tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype", d2);
    
    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }
    
    @Test
    public void testWrittenByMultipleTweetsSingleResult() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2), "alyssa");
        
        assertEquals("expected singleton list", 1, writtenBy.size());
        assertTrue("expected list to contain tweet", writtenBy.contains(tweet1));
    }
    @Test
    public void testWrittenByEmptyList() {
        List<Tweet> result = Filter.writtenBy(Collections.<Tweet>emptyList(), "alyssa");
        assertTrue("expected empty list", result.isEmpty());
    }
    
    @Test
    public void testWrittenByNoMatch() {
        List<Tweet> result = Filter.writtenBy(Arrays.asList(tweet1, tweet2), "nobody");
        assertTrue("expected empty list", result.isEmpty());
    }
    
    @Test
    public void testWrittenByCaseInsensitive() {
        List<Tweet> result = Filter.writtenBy(Arrays.asList(tweet1, tweet2), "ALYSSA");
        assertEquals(Arrays.asList(tweet1), result);
    }
    
    @Test
    public void testWrittenByManyMatchesKeepOrder() {
        Tweet a = new Tweet(10, "alyssa", "first by alyssa", d1);
        Tweet b = new Tweet(11, "bbitdiddle", "by someone else", d1);
        Tweet c = new Tweet(12, "Alyssa", "second by alyssa", d2);

        List<Tweet> result = Filter.writtenBy(Arrays.asList(c, b, a), "alyssa");
        assertEquals(Arrays.asList(c, a), result);
    }
    
    @Test
    public void testInTimespanEmptyList() {
        Timespan ts = new Timespan(d1, d2);
        List<Tweet> result = Filter.inTimespan(Collections.<Tweet>emptyList(), ts);
        assertTrue("expected empty list", result.isEmpty());
    }
    @Test
    public void testInTimespanBeforeAndAfterExcluded() {
        Instant early = Instant.parse("2016-02-17T09:00:00Z");
        Instant late = Instant.parse("2016-02-17T12:00:00Z");
        Tweet before = new Tweet(10, "user1", "too early", early);
        Tweet inside = new Tweet(11, "user2", "just right", d1);
        Tweet after = new Tweet(12, "user3", "too late", late);

        List<Tweet> result = Filter.inTimespan(
                Arrays.asList(before, inside, after), new Timespan(d1, d2));
        assertEquals(Arrays.asList(inside), result);
    }
    @Test
    public void testInTimespanEndpointsIncluded() {
        List<Tweet> result = Filter.inTimespan(
                Arrays.asList(tweet1, tweet2), new Timespan(d1, d2));
        assertEquals(Arrays.asList(tweet1, tweet2), result);
    }
    @Test
    public void testInTimespanZeroLength() {
        List<Tweet> result = Filter.inTimespan(
                Arrays.asList(tweet1, tweet2), new Timespan(d2, d2));
        assertEquals(Arrays.asList(tweet2), result);
    }
    @Test
    public void testInTimespanKeepsOrder() {
        List<Tweet> result = Filter.inTimespan(
                Arrays.asList(tweet2, tweet1), new Timespan(d1, d2));
        assertEquals(Arrays.asList(tweet2, tweet1), result);
    }
    @Test
    public void testContainingEmptyWords() {
        List<Tweet> result = Filter.containing(
                Arrays.asList(tweet1, tweet2), Collections.<String>emptyList());
        assertTrue("expected empty list", result.isEmpty());
    }
    @Test
    public void testContainingNoMatch() {
        List<Tweet> result = Filter.containing(
                Arrays.asList(tweet1, tweet2), Arrays.asList("zebra"));
        assertTrue("expected empty list", result.isEmpty());
    }
    @Test
    public void testContainingSingleResult() {
        List<Tweet> result = Filter.containing(
                Arrays.asList(tweet1, tweet2), Arrays.asList("reasonable"));
        assertEquals(Arrays.asList(tweet1), result);
    }
    @Test
    public void testContainingCaseInsensitive() {
        Tweet shout = new Tweet(10, "user1", "Talk TALK about Rivest", d1);
        List<Tweet> result = Filter.containing(
                Arrays.asList(tweet2, shout), Arrays.asList("RIVEST"));
        assertEquals(Arrays.asList(tweet2, shout), result);
    }
    @Test
    public void testContainingAnyOfSeveralWords() {
        List<Tweet> result = Filter.containing(
                Arrays.asList(tweet1, tweet2), Arrays.asList("reasonable", "minutes"));
        assertEquals(Arrays.asList(tweet1, tweet2), result);
    }
    @Test
    public void testContainingWordAtStartAndEnd() {
        Tweet t = new Tweet(10, "user1", "start middle end", d1);
        assertEquals(Arrays.asList(t),
                Filter.containing(Arrays.asList(t), Arrays.asList("start")));
        assertEquals(Arrays.asList(t),
                Filter.containing(Arrays.asList(t), Arrays.asList("end")));
    }
    @Test
    public void testContainingSubstringIsNotAMatch() {
        Tweet t = new Tweet(10, "user1", "talking about rivest", d1);
        List<Tweet> result = Filter.containing(Arrays.asList(t), Arrays.asList("talk"));
        assertTrue("talk is only part of talking", result.isEmpty());
    }
    @Test
    public void testContainingWordWithPunctuation() {
        // tweet1 ends with "much?", so its last word is "much?" and not "much"
        assertTrue("much is not the same word as much?",
                Filter.containing(Arrays.asList(tweet1), Arrays.asList("much")).isEmpty());
        assertEquals(Arrays.asList(tweet1),
                Filter.containing(Arrays.asList(tweet1), Arrays.asList("much?")));
    }
    @Test
    public void testContainingKeepsOrder() {
        List<Tweet> result = Filter.containing(
                Arrays.asList(tweet2, tweet1), Arrays.asList("rivest"));
        assertEquals(Arrays.asList(tweet2, tweet1), result);
    }
    @Test
    public void testInTimespanMultipleTweetsMultipleResults() {
        Instant testStart = Instant.parse("2016-02-17T09:00:00Z");
        Instant testEnd = Instant.parse("2016-02-17T12:00:00Z");
        
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2), new Timespan(testStart, testEnd));
        
        assertFalse("expected non-empty list", inTimespan.isEmpty());
        assertTrue("expected list to contain tweets", inTimespan.containsAll(Arrays.asList(tweet1, tweet2)));
        assertEquals("expected same order", 0, inTimespan.indexOf(tweet1));
    }
    
    @Test
    public void testContaining() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2), Arrays.asList("talk"));
        
        assertFalse("expected non-empty list", containing.isEmpty());
        assertTrue("expected list to contain tweets", containing.containsAll(Arrays.asList(tweet1, tweet2)));
        assertEquals("expected same order", 0, containing.indexOf(tweet1));
    }

    /*
     * Warning: all the tests you write here must be runnable against any Filter
     * class that follows the spec. It will be run against several staff
     * implementations of Filter, which will be done by overwriting
     * (temporarily) your version of Filter with the staff's version.
     * DO NOT strengthen the spec of Filter or its methods.
     * 
     * In particular, your test cases must not call helper methods of your own
     * that you have put in Filter, because that means you're testing a stronger
     * spec than Filter says. If you need such helper methods, define them in a
     * different class. If you only need them in this test class, then keep them
     * in this test class.
     */

}
