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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public final class FindMeetingQuery {
    /**
     * Given the duration for a meeting, a set of attendees required to 
     * attend the meeting, and a set of events each with their own respective 
     * set of attendees, find and return the set of valid meeting times. 
     * Valid meeting times are time ranges such that the meeting time is 
     * greater than the given duration and all attendees are available 
     * during that given time. 
     * 
     * To find valid meeting times, we sort all events by latest end time
     * first and then iterate through each event, effectively traveling 
     * backwards. For each event, we check if the event's attendees overlap 
     * with the meeting's required attendees. If they do, then we cap off our 
     * current time range, which represents the range for which all meeting
     * attendees are available and spans [event end time, prevEndTime). We add 
     * it to the valid meeting times if it is longer than the given duration. 
     * We then set prevEndTime to the start of the conflicting event and 
     * continue on to the next event. If there are no intersecting attendees, 
     * we move on to the next event and repeat the process.
     *
     * Alternative approaches include sorting by start time, which will yield
     * the same runtime but reduce the need to reverse the resulting set of 
     * TimeRanges at the end to accommodate the test cases. Another algorithm
     * which does not require sorting of the events continuously cuts out 
     * interfering event TimeRanges from an initial day-long time range while
     * throwing away resulting ranges that are less than duration. Although we
     * no longer require sorting, this approach retains the same worst case 
     * runtime if each event splits a remaining range into two pieces of 
     * valid duration. Furthermore, it is more logically complex to code.
     */
    public Collection<TimeRange> findMeetingTimes(
        Collection<Event> events,    
        Collection<String> attendees,
        long duration) {

        List<Event> eventsByEndTime = new ArrayList<Event>(events);
        Collections.sort(eventsByEndTime, Event.ORDER_BY_END_DESCENDING);

        Collection<TimeRange> meetingTimes = new ArrayList<TimeRange>();

        int prevEndTime = TimeRange.END_OF_DAY + 1;
        for (Event event : eventsByEndTime) {
            /* Check if event attendees are required to be at meeting. */
            Collection<String> eventAttendees = event.getAttendees();
            Collection<String> conflictAttendees = new HashSet<>(attendees);
            conflictAttendees.retainAll(event.getAttendees());

            if (!conflictAttendees.isEmpty()) { 
                TimeRange freeTime = TimeRange.fromStartEnd(event.getWhen().end(), prevEndTime, false);
                if (duration <= freeTime.duration()) {
                    meetingTimes.add(freeTime);
                } 
                prevEndTime = (event.getWhen().start() < prevEndTime) ? event.getWhen().start() : prevEndTime;
            }
        }

        /* Add unchecked TimeRange [0, prevEndTime) to list if range is valid. */
        if (prevEndTime > 0) {
            TimeRange firstFreeTime = TimeRange.fromStartEnd(0, prevEndTime, false);
            meetingTimes.add(firstFreeTime);
        }

        List<TimeRange> meetingTimesList = new ArrayList<TimeRange>(meetingTimes);
        Collections.reverse(meetingTimesList);
        return meetingTimesList;
    }

    /**
     * The query function returns a set of non-overlapping TimeRanges where a 
     * potential meeting, specified as a MeetingRequest argument, can take place 
     * among a day of Events. A MeetingRequest has a specified duration and set 
     * of attendees, which can be broken down into a set of mandatory attendees, 
     * which must attend, and set of optional attendees, that are preferred to 
     * attend but might not necessarily have to. If the algorithm does not find
     * any valid TimeRanges for all of the attendees, including optional ones,
     * then it will only return TimeRanges for mandatory attendees. A drawback of
     * this approach is that we must call the function to find suitable TimeRanges
     * two times.
     *
     * The algorithm runs in O(n^2) time since we have to sort the events first, 
     * requiring O(n*log(n)) time, and then iterate through each event and find 
     * conflicting attendees, requiring O(n*m) time, where m is the number of 
     * attendees.
     */
    public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
        Collection<TimeRange> meetingTimes = new ArrayList<TimeRange>();
        Collection<String> mandatoryAttendees = request.getAttendees();
        Collection<String> optionalAttendees = request.getOptionalAttendees();
        Collection<String> allAttendees = new ArrayList<>(mandatoryAttendees);
        allAttendees.addAll(optionalAttendees);
        
        long duration = request.getDuration();
        if (duration < 0 || duration > TimeRange.END_OF_DAY) {
            return meetingTimes; // return empty if invalid duration
        }

        if (events.isEmpty() || allAttendees.isEmpty()) {
            meetingTimes.add(TimeRange.WHOLE_DAY);
            return meetingTimes; // no attendees or events
        }

        /** 
         * If valid TimeRanges for all attendees exist, return them.
         * Otherwise, only find TimeRanges for mandatory attendees.
         */
        meetingTimes = findMeetingTimes(events, allAttendees, duration);
        if (!meetingTimes.isEmpty()) {
            return meetingTimes;
        }
        return findMeetingTimes(events, mandatoryAttendees, duration);
    }   
}
