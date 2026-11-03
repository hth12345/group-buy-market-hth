package com.hth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.transform.Source;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AllActivitiesRequestDTO {
    private String userId;
    private String source;
    private String channel;
}
