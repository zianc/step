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
     * Given a set of events and a list of attendees, find and return the set of
     * valid meeting times.
     */
    public Collection<TimeRange> findMeetingTimes (
        Collection<Event> events,    Collection<String> attendees,
        long duration) {

        /* Sort events with latest end times coming first. */
        List<Event> eventsByEndTime = new ArrayList<Event>(events);
        List<Event> eventsByStartTime = new ArrayList<Event>(events);
        Collections.sort(eventsByEndTime, Event.ORDER_BY_END_DESCENDING);
        Collections.sort(eventsByStartTime, Event.ORDER_BY_START_ASCENDING);

        Collection<TimeRange> meetingTimes = new ArrayList<TimeRange>();
        int prevEndTime = TimeRange.END_OF_DAY + 1;
        
        for (Event event : eventsByEndTime) {
            /* Find attendees required to be at meeting also attending event. */
            Collection<String> conflictAttendees = new HashSet<>(attendees);
            Collection<String> eventAttendees = event.getAttendees();
            conflictAttendees.retainAll(event.getAttendees());

            /**
             * For events that have conflicting attendees, add the time range 
             * spanning from the last recorded end time to the current event's end 
             * time to our output set if the duration fits within it. Move last end 
             * time to current event's start time for the next event. 
             */
            if (conflictAttendees.size() > 0) {
                TimeRange freeTime = TimeRange.fromStartEnd(event.getWhen().end(), prevEndTime, false);
                if (duration <= freeTime.duration()) {
                    meetingTimes.add(freeTime);
                } 
                prevEndTime = (event.getWhen().start() < prevEndTime) ? event.getWhen().start() : prevEndTime;
            }
        }

        /* Check the time range from the start of the day to the first event's start time. */
        if (prevEndTime > 0) {
            TimeRange firstFreeTime = TimeRange.fromStartEnd(0, prevEndTime, false);
            meetingTimes.add(firstFreeTime);
        }

        List<TimeRange> meetingTimesList = new ArrayList<TimeRange>(meetingTimes);
        Collections.reverse(meetingTimesList);
        return meetingTimesList;
    }

    /**
     * The query function returns a set of TimeRanges where a potential meeting,
     * specified as a MeetingRequest argument, can take place among a day of 
     * Events. The algorithm runs in O(n^2) time since we have to sort the events
     * first, requiring O(nlogn) time, and then iterate through each event and 
     * find conflicting attendees, requiring O(n^2) time.
     */
    public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
        Collection<TimeRange> meetingTimes = new ArrayList<TimeRange>();
        Collection<String> mandatoryAttendees = request.getAttendees();
        Collection<String> optionalAttendees = request.getOptionalAttendees();
        Collection<String> allAttendees = new ArrayList<>(mandatoryAttendees);
        allAttendees.addAll(optionalAttendees);
        
        /* Check validity of arguments. */
        long duration = request.getDuration();
        if (duration < 0 || duration >= TimeRange.END_OF_DAY) {
            return meetingTimes; // return empty if invalid duration
        }

        if (events.size() == 0 || allAttendees.size() == 0) {
            meetingTimes.add(TimeRange.WHOLE_DAY);
            return meetingTimes; // no attendees or events
        }

        /* First check if there exist meeting times with optional attendees. */
        meetingTimes = findMeetingTimes(events, allAttendees, eventsByEndTime, eventsByStartTime, duration);
        if (meetingTimes.size() > 0) {
            return meetingTimes;
        }

        /* Otherwise, simply find appropriate meeting times for mandatory attendees. */
        return findMeetingTimes(events, mandatoryAttendees, eventsByEndTime, eventsByStartTime, duration);
    }   
}
