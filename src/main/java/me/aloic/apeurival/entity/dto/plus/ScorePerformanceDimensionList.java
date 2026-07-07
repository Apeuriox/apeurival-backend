package me.aloic.apeurival.entity.dto.plus;

import lombok.Data;

import java.util.List;

@Data
public class ScorePerformanceDimensionList
{
    private Integer code;
    private List<ScorePerformanceDTO> data;
    private String msg;
}
