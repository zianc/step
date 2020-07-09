package com.google.sps.data;

/*
 * Comment class holds the components of a comment made by the user. Each 
 * comment has a unique ID and timestamp for time of creation, as well as
 * the actual content provided by the user.
 */
public class Comment {
    private long id;
    private String comment;
    private long timestamp;

    public Comment(long id, String comment, long timestamp) {
        this.comment = comment;
        this.id = id;
        this.timestamp = timestamp;
    }
}
