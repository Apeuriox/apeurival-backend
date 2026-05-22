package me.aloic.apeurival.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("works")
@AllArgsConstructor
@NoArgsConstructor
public class WorkPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String type;
    private String coverUrl;
    private String contentMd;
    private String tags;
    private Long authorId;
    private Integer status = 0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
