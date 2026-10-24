/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ExtractTest {
	/*
	 * Testing strategy
	 *
	 * getTimespan(tweets): partition on number of tweets: 0, 1, 2, many partition
	 * on order of timestamps: ascending, descending, unsorted partition on
	 * duplicate timestamps: yes, no
	 *
	 * getMentionedUsers(tweets): partition on number of mentions: 0, 1, many
	 * partition on same user mentioned twice: no, same case, different case
	 * partition on position of '@': start, middle, end of text partition on
	 * character before '@': space, username character (like an email), punctuation
	 * partition on character after the name: space, punctuation, end of text
	 *
	 * Choosing test cases: one test for each partition value, plus edge cases like
	 * a single tweet and an email address.
	 */
	private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
	private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");

	private static final Tweet tweet1 = new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
	private static final Tweet tweet2 = new Tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype", d2);

	// Helper: runs getMentionedUsers on one tweet with this text and
	// returns the names in lower case, so tests pass whichever case
	// an implementation chooses to return.
	private static Set<String> mentions(String text) {
		Tweet t = new Tweet(1, "author", text, d1);
		Set<String> result = Extract.getMentionedUsers(Arrays.asList(t));
		Set<String> lowered = new HashSet<>();
		for (String user : result) {
			lowered.add(user.toLowerCase());
		}
		return lowered;
	}

	// Helper: builds the expected set of names (use lower case).
	private static Set<String> setOf(String... names) {
		return new HashSet<>(Arrays.asList(names));
	}

	@Test
	public void testGetTimespanSingleTweet() {
		Timespan ts = Extract.getTimespan(Arrays.asList(tweet1));
		assertEquals("start", d1, ts.getStart());
		assertEquals("end", d1, ts.getEnd());
	}

	@Test
	public void testGetTimespanDescendingOrder() {
		Timespan ts = Extract.getTimespan(Arrays.asList(tweet2, tweet1));
		assertEquals("start", d1, ts.getStart());
		assertEquals("end", d2, ts.getEnd());
	}

	@Test
	public void testGetTimespanUnsortedManyTweets() {
		Instant early = Instant.parse("2016-02-17T09:00:00Z");
		Instant late = Instant.parse("2016-02-17T12:00:00Z");
		Tweet a = new Tweet(10, "user1", "middle tweet", d2);
		Tweet b = new Tweet(11, "user2", "earliest tweet", early);
		Tweet c = new Tweet(12, "user3", "latest tweet", late);
		Tweet d = new Tweet(13, "user4", "another middle tweet", d1);

		Timespan ts = Extract.getTimespan(Arrays.asList(a, b, c, d));
		assertEquals("start", early, ts.getStart());
		assertEquals("end", late, ts.getEnd());
	}

	@Test
	public void testGetTimespanSameTimestamps() {
		Tweet a = new Tweet(20, "user1", "first tweet", d1);
		Tweet b = new Tweet(21, "user2", "second tweet", d1);

		Timespan ts = Extract.getTimespan(Arrays.asList(a, b));
		assertEquals("start", d1, ts.getStart());
		assertEquals("end", d1, ts.getEnd());
	}

	@Test
	public void testGetTimespanEmptyListDoesNotThrow() {
		Timespan ts = Extract.getTimespan(Collections.<Tweet>emptyList());
		assertNotNull("expected some timespan", ts);
	}

	@Test
	public void testGetMentionedUsersSingleMentionMiddle() {
	    assertEquals(setOf("alyssa"), mentions("hello @alyssa how are you"));
	}
	@Test
	public void testGetMentionedUsersStartAndEnd() {
	    assertEquals(setOf("alyssa", "ben"), mentions("@alyssa meet @ben"));
	}
	@Test
	public void testGetMentionedUsersSameUserDifferentCase() {
	    Set<String> result = mentions("@Alyssa and @aLYSSA");
	    assertEquals(setOf("alyssa"), result);
	}
	@Test
	public void testGetMentionedUsersEmailIsNotMention() {
	    assertTrue(mentions("mail bitdiddle@mit.edu now").isEmpty());
	}
	
	@Test
	public void testGetMentionedUsersFollowedByPunctuation() {
	    assertEquals(setOf("alyssa", "ben"), mentions("hi @alyssa, and @ben!"));
	}
	
	@Test
	public void testGetMentionedUsersPrecededByPunctuation() {
	    assertEquals(setOf("alyssa"), mentions("(@alyssa) said hi"));
	}
	
	@Test
	public void testGetMentionedUsersBareAt() {
	    assertTrue(mentions("meet @ noon, or email @").isEmpty());
	}
	@Test
	public void testGetMentionedUsersAcrossTweets() {
	    Tweet a = new Tweet(30, "user1", "hi @alyssa", d1);
	    Tweet b = new Tweet(31, "user2", "yo @ben and @ALYSSA", d2);
	    Set<String> result = Extract.getMentionedUsers(Arrays.asList(a, b));

	    Set<String> lowered = new HashSet<>();
	    for (String user : result) {
	        lowered.add(user.toLowerCase());
	    }
	    assertEquals(setOf("alyssa", "ben"), lowered);
	    assertEquals("each user at most once", 2, result.size());
	}
	@Test
	public void testGetMentionedUsersUsernameCharacters() {
	    assertEquals(setOf("a1", "b_2", "c-3"), mentions("@a1 @b_2 and @c-3 are here"));
	}
	@Test(expected = AssertionError.class)
	public void testAssertionsEnabled() {
		assert false; // make sure assertions are enabled with VM argument: -ea
	}

	@Test
	public void testGetTimespanTwoTweets() {
		Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1, tweet2));

		assertEquals("expected start", d1, timespan.getStart());
		assertEquals("expected end", d2, timespan.getEnd());
	}

	@Test
	public void testGetMentionedUsersNoMention() {
		Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet1));

		assertTrue("expected empty set", mentionedUsers.isEmpty());
	}

	/*
	 * Warning: all the tests you write here must be runnable against any Extract
	 * class that follows the spec. It will be run against several staff
	 * implementations of Extract, which will be done by overwriting (temporarily)
	 * your version of Extract with the staff's version. DO NOT strengthen the spec
	 * of Extract or its methods.
	 * 
	 * In particular, your test cases must not call helper methods of your own that
	 * you have put in Extract, because that means you're testing a stronger spec
	 * than Extract says. If you need such helper methods, define them in a
	 * different class. If you only need them in this test class, then keep them in
	 * this test class.
	 */

}
