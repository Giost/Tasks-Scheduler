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

package com.google.sps.servlets;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.tasks.model.Task;
import com.google.sps.api.calendar.CalendarInterface;
import com.google.sps.api.tasks.TasksProvider;
import com.google.sps.scheduler.Scheduler;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Schedules tasks on tomorrow.
 */
@WebServlet("/schedule")
public class ScheduleServlet extends HttpServlet {

  public static final String TASK_ID_LIST_KEY = "taskId";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    TasksProvider taskProvider = new TasksProvider();
    List<Task> tasks = taskProvider.getTasks();
    if(!request.getParameterMap().containsKey(TASK_ID_LIST_KEY)){
      sendJsonResponse(response, "Select some tasks to schedule.");
      return;
    }
    Set<String> selectedIds = Arrays.stream(request.getParameterValues(TASK_ID_LIST_KEY)).collect(Collectors.toSet());
    List<String> titlesOfTasksToSchedule = filterSelectedTaskTitles(selectedIds, tasks);

    CalendarInterface calendarInterface = new CalendarInterface();
    List<Event> calendarEvents = calendarInterface.loadPrimaryCalendarEventsOfTomorrow();
    String timeZone = calendarInterface.getPrimaryCalendarTimeZone();
    LocalDate tomorrow = calendarInterface.getUsersTomorrowStart().toLocalDate();


    List<Event> tasksEvent = Scheduler.schedule(calendarEvents, titlesOfTasksToSchedule, timeZone, tomorrow);
    for (Event event : tasksEvent) {
      calendarInterface.insertEventToPrimary(event);
    }

    sendJsonResponse(response, tasksEvent.size() + " tasks inserted on " + tomorrow);
  }

  private void sendJsonResponse(HttpServletResponse response, String responseMessage) throws IOException {
    response.setContentType(MediaType.APPLICATION_JSON);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    try{
      JSONObject resultJSON = new JSONObject();
      resultJSON.put("message", responseMessage);
      response.getWriter().println(resultJSON);
    }catch(JSONException exception){
      throw new IOException(exception);
    }
  }

  List<String> filterSelectedTaskTitles(Set<String> selectedIds, List<Task> tasks) {
    return tasks.stream().filter((task) -> selectedIds.contains(task.getId())).map(Task::getTitle).collect(Collectors.toList()); //Java question: does a new array get created here?
  }
}
