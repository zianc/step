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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.sps.data.Constants;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.gson.Gson;
import com.google.sps.data.Comment;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@WebServlet("/data")
public class DataServlet extends HttpServlet {
    /* 
     * Retrieves comments from Datastore and adds HTML elements to DOM.
     * Returns comments to client in JSON form. 
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {        
        int limit = Integer.parseInt(request.getParameter(Constants.COMMENTS_LIMIT));
        Query query = new Query(Constants.COMMENTS_KIND).addSort(Constants.COMMENTS_TIMESTAMP, SortDirection.DESCENDING);
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery results = datastore.prepare(query);

        List<Comment> comments = new ArrayList<>();
        int counter = 0;
        for (Entity entity : results.asIterable()) {
            /* Only return the number of comments as specified in query. */
            if (counter == limit) {
                break;
            } 
            counter++;
            String line = (String)entity.getProperty(Constants.COMMENTS_PROPERTY);
            long id = (long)entity.getKey().getId();
            long timestamp = (long)entity.getProperty(Constants.COMMENTS_TIMESTAMP);
            Comment comment = new Comment(id, line, timestamp);
            comments.add(comment);
        }

        Gson gson = new Gson();
        response.setContentType("application/json;");
        response.getWriter().println(gson.toJson(comments));
    }

    /* 
     * Adds a comment to Datastore, with corresponding time and ID.
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String comment = request.getParameter(Constants.COMMENTS_PROPERTY);
        Entity taskEntity = new Entity(Constants.COMMENTS_KIND);
        taskEntity.setProperty(Constants.COMMENTS_PROPERTY, comment);
        taskEntity.setProperty(Constants.COMMENTS_TIMESTAMP, System.currentTimeMillis());
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(taskEntity);
        
        response.sendRedirect("/index.html");
    }
}
