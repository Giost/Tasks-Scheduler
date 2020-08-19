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

package com.google.sps.api;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.appengine.api.users.UserServiceFactory;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class CalendarInterface implements Serializable {
    public static final String PRIMARY = "primary";
    //Default timezone
    public static final String CET_TIME_ZONE = "Europe/Zurich";
    private final CalendarClientHelper calendarClientHelper = new CalendarClientHelper();
    private Calendar calendarClient;


    /**
     * Upon instantiation create Calendar instance (calendarClient)
     */
    public CalendarInterface()  throws IOException{
        String userId = UserServiceFactory.getUserService().getCurrentUser().getUserId();
        Credential credential = Utils.newFlow().loadCredential(userId);
        calendarClient = new Calendar.Builder(Utils.HTTP_TRANSPORT, Utils.JSON_FACTORY, credential).build();
    }


    /**
     * Get the user's primary calendar's timezone
     */
    public String getPrimaryCalendarTimeZone() {
        String timeZone;
        try {
            CalendarListEntry calendarListEntryPrimary = calendarClient.calendarList().get("primary").execute();
            timeZone = calendarListEntryPrimary.getTimeZone();
        } catch (Exception e) {
            timeZone = CET_TIME_ZONE;
        }
        return timeZone;
    }

    public Calendar getCalendarClient() throws IOException {
        return calendarClient;
    }

    /**
     * Return the user's events of the next day, from the user's primary calendar.
     * The start of the next day is based on the calendar's timezone setting.
     */

    public List<Event> loadPrimaryCalendarEventsOfTomorrow() throws IOException{
        ZoneId userZoneId = ZoneId.of(getPrimaryCalendarTimeZone());
        LocalDate todayHere = LocalDate.now(userZoneId);
        ZonedDateTime todayStart = todayHere.atStartOfDay(userZoneId);
        ZonedDateTime tomorrowStart = todayStart.plusDays(1);
        ZonedDateTime tomorrowEnd = todayStart.plusDays(2);
        DateTime startDate = new DateTime(tomorrowStart.toInstant().toEpochMilli());
        DateTime endDate = new DateTime(tomorrowEnd.toInstant().toEpochMilli());
        return getAcceptedEventsInTimerange(startDate, endDate);
    }


    /**
     * Get the user's primary calendar's events in the given timerange
     * Recurring events should be handled as separate single events, and only own events and events with accepted invitation should be returned
     */
    public List<Event> getAcceptedEventsInTimerange(DateTime startTime, DateTime endTime) throws IOException {

        Events events = calendarClient.events().list(PRIMARY)
                .setSingleEvents(true) //Handle recurring events as separate single events
                .setTimeMin(startTime)
                .setTimeMax(endTime)
                .execute();
        return events.getItems().stream()
                .filter(event -> calendarClientHelper.isAttending(event))
                .collect(Collectors.toList());
    }

    public void InsertEventToPrimary(Event event) throws IOException{
        calendarClient.events().insert(PRIMARY,event);
    }
}