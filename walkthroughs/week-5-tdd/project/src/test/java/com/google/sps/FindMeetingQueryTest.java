// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
 
package com.google.sps;
 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
 
/** */
@RunWith(JUnit4.class)
public final class FindMeetingQueryTest {
  private static final Collection<Event> NO_EVENTS = Collections.emptySet();
  private static final Collection<String> NO_ATTENDEES = Collections.emptySet();
 
  // Some people that we can use in our tests.
  private static final String PERSON_A = "Person A";
  private static final String PERSON_B = "Person B";
  private static final String PERSON_C = "Person C";
 
  // All dates are the first day of the year 2020.
  private static final int TIME_0800AM = TimeRange.getTimeInMinutes(8, 0);
  private static final int TIME_0830AM = TimeRange.getTimeInMinutes(8, 30);
  private static final int TIME_0845AM = TimeRange.getTimeInMinutes(8, 45);
  private static final int TIME_0900AM = TimeRange.getTimeInMinutes(9, 0);
  private static final int TIME_0930AM = TimeRange.getTimeInMinutes(9, 30);
  private static final int TIME_1000AM = TimeRange.getTimeInMinutes(10, 0);
  private static final int TIME_1100AM = TimeRange.getTimeInMinutes(11, 00);
  private static final int TIME_1230PM = TimeRange.getTimeInMinutes(12, 30);
  private static final int TIME_0100PM = TimeRange.getTimeInMinutes(13, 0);
  private static final int TIME_0400PM = TimeRange.getTimeInMinutes(16, 0);
  private static final int TIME_0430PM = TimeRange.getTimeInMinutes(16, 30);
 
  private static final int DURATION_NEGATIVE = -1;
  private static final int DURATION_15_MINUTES = 15;
  private static final int DURATION_30_MINUTES = 30;
  private static final int DURATION_60_MINUTES = 60;
  private static final int DURATION_90_MINUTES = 90;
  private static final int DURATION_1_HOUR = 60;
  private static final int DURATION_2_HOUR = 120;
  private static final int DURATION_WHOLE_DAY = 24 * 60;
 
  private FindMeetingQuery query;
 
  @Before
  public void setUp() {
    query = new FindMeetingQuery();
  }
 
  @Test
  public void optionsForNoAttendees() {
    MeetingRequest request = new MeetingRequest(NO_ATTENDEES, DURATION_1_HOUR);
 
    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);
 
    Assert.assertEquals(expected, actual);
  }
 
  @Test
  public void noOptionsForTooLongOfARequest() {
    // The duration should be longer than a day. This means there should be no options.
    int duration = TimeRange.WHOLE_DAY.duration() + 1;
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), duration);
 
    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList();
 
    Assert.assertEquals(expected, actual);
  }
 
  @Test
  public void eventSplitsRestriction() {
    // The event should split the day into two options (before and after the event).
    Collection<Event> events = Arrays.asList(new Event("Event 1",
        TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES), Arrays.asList(PERSON_A)));
 
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true));
 
    Assert.assertEquals(expected, actual);
  }
 
  @Test
  public void everyAttendeeIsConsidered() {
    // Have each person have different events. We should see two options because each person has
    // split the restricted times.
    //
    // Events  :       |--A--|     |--B--|
    // Day     : |-----------------------------|
    // Options : |--1--|     |--2--|     |--3--|
 
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)));
 
    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
            TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));
 
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void wholeDayEvent() {
      // Have no events and a meeting that takes the entire day. Should return one TimeRange
      // that spans the entire day.
      //
      // Events:  
      // Day:     |---------------------|
      // Options: |---------------------|

      Collection<Event> events = Arrays.asList();

      MeetingRequest request = new MeetingRequest(Arrays.asList(), DURATION_WHOLE_DAY);

      Collection<TimeRange> actual = query.query(events, request);
      Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);

      Assert.assertEquals(expected, actual);
  }

  @Test
  public void negativeDuration() {
      // Have a meeting request with negative duration. Should return no results.

      Collection<Event> events = Arrays.asList();

      MeetingRequest request = new MeetingRequest(Arrays.asList(), DURATION_NEGATIVE);

      Collection<TimeRange> actual = query.query(events, request);
      Collection<TimeRange> expected = Arrays.asList();

      Assert.assertEquals(expected, actual);
  }
 
  @Test
  public void optionalAttendeeAllDay() {
    // Have each person have different events with one optional attendee
    // busy for the entire day. We should see three options because each
    // person has split the restricted times. The additional optional
    // attendee does not factor in here.
    //
    // Events  :       |--A--|     |--B--|
    // Event   : |--------------C--------------|
    // Day     : |-----------------------------|
    // Options : |--1--|     |--2--|     |--3--|

    Collection<Event> events = Arrays.asList(
      new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
          Arrays.asList(PERSON_A)),
      new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
          Arrays.asList(PERSON_B)),
      new Event("Event 3", TimeRange.WHOLE_DAY,
          Arrays.asList(PERSON_C))
    );

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_C);
    
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
      Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
        TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
        TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true)
    );

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optionalAttendeeMidDay() {
    // Have each person have different events with one optional attendee
    // busy between 8:30 and 9. We should see two options, one early in 
    // the day and one late in the day
    //
    // Events  :       |--A--|--C--|--B--|
    // Day     : |-----------------------------|
    // Options : |--1--|                 |--3--|

    Collection<Event> events = Arrays.asList(
      new Event("Event 1", TimeRange.fromStartDuration(TIME_0800AM, DURATION_30_MINUTES),
          Arrays.asList(PERSON_A)),
      new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
          Arrays.asList(PERSON_B)),
      new Event("Event 3", TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES),
          Arrays.asList(PERSON_C))
    );

    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_C);
    
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
      Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0800AM, false),
        TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true)
    );

    Assert.assertEquals(expected, actual);
  }
 
  @Test
  public void overlappingEvents() {
    // Have an event for each person, but have their events overlap. We should only see two options.
    //
    // Events  :       |--A--|
    //                     |--B--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|
 
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0830AM, DURATION_60_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_60_MINUTES),
            Arrays.asList(PERSON_B)));
 
    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_1000AM, TimeRange.END_OF_DAY, true));
 
    Assert.assertEquals(expected, actual);
  }
 
  @Test
  public void nestedEvents() {
    // Have an event for each person, but have one person's event fully contain another's event. We
    // should see two options.
    //
    // Events  :       |----A----|
    //                   |--B--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|
 
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0830AM, DURATION_90_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_B)));
 
    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_1000AM, TimeRange.END_OF_DAY, true));
 
    Assert.assertEquals(expected, actual);
  }
 
  @Test
  public void doubleBookedPeople() {
    // Have one person, but have them registered to attend two events at the same time.
    //
    // Events  :       |----A----|
    //                     |--A--|
    // Day     : |---------------------|
    // Options : |--1--|         |--2--|
 
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartDuration(TIME_0830AM, DURATION_60_MINUTES),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES),
            Arrays.asList(PERSON_A)));
 
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            TimeRange.fromStartEnd(TIME_0930AM, TimeRange.END_OF_DAY, true));
 
    Assert.assertEquals(expected, actual);
  }
 
  @Test
  public void justEnoughRoom() {
    // Have one person, but make it so that there is just enough room at one point in the day to
    // have the meeting.
    //
    // Events  : |--A--|     |----A----|
    // Day     : |---------------------|
    // Options :       |-----|
 
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_A)));
 
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES));
 
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optionalJustEnoughRoom() {
    // Have two people, but make it so that there is just enough room at one point in the day to
    // have the meeting. Optional person B should be ignored.
    //
    // Events  : |--A--|-B-|   |----A----|
    // Day     : |-----------------------|
    // Options :       |-------|
 
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_A)),
        new Event("Event 3", TimeRange.fromStartEnd(TIME_0830AM, TIME_0845AM, false),
            Arrays.asList(PERSON_B))
    );
 
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_B);

    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected =
        Arrays.asList(TimeRange.fromStartDuration(TIME_0830AM, DURATION_30_MINUTES));
 
    Assert.assertEquals(expected, actual);
  }
 
  @Test
  public void ignoresPeopleNotAttending() {
    // Add an event, but make the only attendee someone different from the person looking to book
    // a meeting. This event should not affect the booking.
    Collection<Event> events = Arrays.asList(new Event("Event 1",
        TimeRange.fromStartDuration(TIME_0900AM, DURATION_30_MINUTES), Arrays.asList(PERSON_A)));
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_B), DURATION_30_MINUTES);
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);
 
    Assert.assertEquals(expected, actual);
  }
 
  @Test
  public void noConflicts() {
    MeetingRequest request =
        new MeetingRequest(Arrays.asList(PERSON_A, PERSON_B), DURATION_30_MINUTES);
 
    Collection<TimeRange> actual = query.query(NO_EVENTS, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);
 
    Assert.assertEquals(expected, actual);
  }
 
  @Test
  public void notEnoughRoom() {
    // Have one person, but make it so that there is not enough room at any point in the day to
    // have the meeting.
    //
    // Events  : |--A-----| |-----A----|
    // Day     : |---------------------|
    // Options :
 
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartEnd(TIME_0900AM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_A)));
 
    MeetingRequest request = new MeetingRequest(Arrays.asList(PERSON_A), DURATION_60_MINUTES);
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList();
 
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optionalOnlyWithGaps() {
    // Two optional attendees with several gaps in their schedules. Those gaps
    // should be returned and identified.
    //
    // Events  :    |-A-|  |-B-|   |-C-|
    // Day     : |---------------------------|
    // Options : |--|   |--|   |---|   |-----|
 
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartEnd(TIME_0830AM, TIME_0900AM, false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartEnd(TIME_1230PM, TIME_0100PM, false),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.fromStartEnd(TIME_0400PM, TIME_0430PM, false),
            Arrays.asList(PERSON_C))
        );
 
    MeetingRequest request = new MeetingRequest(Arrays.asList(), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_A);
    request.addOptionalAttendee(PERSON_B);
    request.addOptionalAttendee(PERSON_C);
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0830AM, false),
        TimeRange.fromStartEnd(TIME_0900AM, TIME_1230PM, false),
        TimeRange.fromStartEnd(TIME_0100PM, TIME_0400PM, false),
        TimeRange.fromStartEnd(TIME_0430PM, TimeRange.END_OF_DAY, true)
    );
 
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void optionalOnlyWithoutGaps() {
    // Two optional attendees with several no gaps in their schedules. Entire day
    // should be returned as available meeting time.
    //
    // Events  : |---A---|-B-|----C----|
    // Day     : |---------------------|
    // Options : |---------------------|
 
    Collection<Event> events = Arrays.asList(
        new Event("Event 1", TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TIME_0900AM, false),
            Arrays.asList(PERSON_A)),
        new Event("Event 2", TimeRange.fromStartEnd(TIME_0900AM, TIME_0100PM, false),
            Arrays.asList(PERSON_B)),
        new Event("Event 3", TimeRange.fromStartEnd(TIME_0100PM, TimeRange.END_OF_DAY, true),
            Arrays.asList(PERSON_C))
        );
 
    MeetingRequest request = new MeetingRequest(Arrays.asList(), DURATION_30_MINUTES);
    request.addOptionalAttendee(PERSON_A);
    request.addOptionalAttendee(PERSON_B);
    request.addOptionalAttendee(PERSON_C);
 
    Collection<TimeRange> actual = query.query(events, request);
    Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);
 
    Assert.assertEquals(expected, actual);
  }
}
