package me.aloic.apeurival.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@TableName("work_moments")
@AllArgsConstructor
@NoArgsConstructor
public class WorkMomentPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workId;
    private String imageUrl;
    private String content;
    private LocalDate momentTime;
    private Integer sortOrder;
}
