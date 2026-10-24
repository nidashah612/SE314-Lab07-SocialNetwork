/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class SocialNetworkTest {

    /*
     * Testing strategy for guessFollowsGraph(tweets):
     *   number of tweets: 0, 1, > 1
     *   mentions in a tweet: 0, 1, > 1
     *   tweets by the same author: 1, > 1 (with different mentions)
     *   case of a mentioned username: same as elsewhere, different
     *
     * The spec is underdetermined: the graph may use any case for usernames,
     * may contain a user with an empty set, and may use extra evidence.
     * So the tests only check the follows that MUST be there (A @-mentions B),
     * and compare usernames after converting them to lowercase.
     *
     * Testing strategy for influencers(followsGraph):
     *   number of users in the graph: 0, 1, > 1
     *   followers of the top user: 0, 1, > 1
     *   follower counts: all different, some tied
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");

    private static final Tweet noMention = new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet oneMention = new Tweet(2, "bbitdiddle", "saving a seat for @Alyssa", d1);
    private static final Tweet twoMentions = new Tweet(3, "evaluator", "thanks @bbitdiddle and @alyssa", d2);
    private static final Tweet bbitAgain = new Tweet(4, "BBitDiddle", "@evaluator see you at lecture", d2);

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // ---------------- guessFollowsGraph ----------------

    // covers: 0 tweets
    @Test
    public void testGuessFollowsGraphEmpty() {
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(new ArrayList<>());

        assertTrue("expected empty graph", followsGraph.isEmpty());
    }
    // TOO STRICT: the spec allows "alyssa" -> {} in the graph
    
   
    // covers: 1 tweet, 0 mentions
    @Test
    public void testGuessFollowsGraphNoMentions() {
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(noMention));

        // alyssa may appear as a key, but she must not follow anybody
        for (Set<String> follows : followsGraph.values()) {
            assertTrue("expected no follows", follows.isEmpty());
        }
    }

    // covers: 1 tweet, 1 mention, different case
    @Test
    public void testGuessFollowsGraphSingleMention() {
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(oneMention));

        assertTrue("expected bbitdiddle follows alyssa", follows(followsGraph, "bbitdiddle", "alyssa"));
    }

    // covers: 1 tweet, > 1 mentions
    @Test
    public void testGuessFollowsGraphMultipleMentions() {
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(twoMentions));

        assertTrue(follows(followsGraph, "evaluator", "bbitdiddle"));
        assertTrue(follows(followsGraph, "evaluator", "alyssa"));
    }

    // covers: > 1 tweets, same author in different case, different mentions
    @Test
    public void testGuessFollowsGraphMultipleTweetsOneUser() {
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(oneMention, bbitAgain));

        assertTrue(follows(followsGraph, "bbitdiddle", "alyssa"));
        assertTrue(follows(followsGraph, "bbitdiddle", "evaluator"));
    }

    // ---------------- influencers ----------------

    // covers: 0 users
    @Test
    public void testInfluencersEmpty() {
        List<String> influencers = SocialNetwork.influencers(new HashMap<>());

        assertTrue("expected empty list", influencers.isEmpty());
    }

    // covers: 1 user, 0 followers
    @Test
    public void testInfluencersSingleUserNoFollowers() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alyssa", new HashSet<>());

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        // the spec says the list holds ALL usernames, so alyssa is still listed
        assertEquals(1, influencers.size());
        assertEquals("alyssa", influencers.get(0).toLowerCase());
    }

    // covers: > 1 users, top user has 1 follower
    @Test
    public void testInfluencersSingleInfluencer() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("bbitdiddle", new HashSet<>(Arrays.asList("alyssa")));

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals(2, influencers.size());
        assertEquals("alyssa", influencers.get(0).toLowerCase());
    }

    // covers: > 1 users, top user has > 1 followers, counts all different
    @Test
    public void testInfluencersMultipleInfluencers() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("bbitdiddle", new HashSet<>(Arrays.asList("alyssa")));
        followsGraph.put("evaluator", new HashSet<>(Arrays.asList("alyssa", "bbitdiddle")));

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals(3, influencers.size());
        assertEquals("alyssa", influencers.get(0).toLowerCase());     // 2 followers
        assertEquals("bbitdiddle", influencers.get(1).toLowerCase()); // 1 follower
        assertEquals("evaluator", influencers.get(2).toLowerCase());  // 0 followers
    }

    // covers: some follower counts tied
    @Test
    public void testInfluencersTied() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("evaluator", new HashSet<>(Arrays.asList("alyssa", "bbitdiddle")));

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        // alyssa and bbitdiddle both have 1 follower: either order is correct
        Set<String> topTwo = new HashSet<>();
        topTwo.add(influencers.get(0).toLowerCase());
        topTwo.add(influencers.get(1).toLowerCase());
        assertEquals(new HashSet<>(Arrays.asList("alyssa", "bbitdiddle")), topTwo);
        assertEquals("evaluator", influencers.get(2).toLowerCase());
    }

    /*
     * Helper for the tests: true if follower follows followed in the graph,
     * ignoring case (the spec allows any case for usernames).
     */
    private static boolean follows(Map<String, Set<String>> followsGraph, String follower, String followed) {
        for (Map.Entry<String, Set<String>> entry : followsGraph.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(follower)) {
                for (String name : entry.getValue()) {
                    if (name.equalsIgnoreCase(followed)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}