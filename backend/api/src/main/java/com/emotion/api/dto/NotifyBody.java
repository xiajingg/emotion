package com.emotion.api.dto;

import lombok.Data;

import java.util.Date;

/**
 * Auto-generated: 2024-11-23 18:46:23
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
@Data
public class NotifyBody {

    private String id;
    private Date create_time;
    private String resource_type;
    private String event_type;
    private String summary;
    private Object resource;

}