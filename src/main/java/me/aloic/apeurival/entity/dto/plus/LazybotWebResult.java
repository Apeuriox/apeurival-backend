package me.aloic.apeurival.entity.dto.plus;

import lombok.Data;

@Data
public class LazybotWebResult<T>
{
    private Integer code;
    private T data;
    private String msg;
}
