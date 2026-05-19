package me.aloic.apeurival.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("code_works")
@AllArgsConstructor
@NoArgsConstructor
public class CodeWorkPO {
    @TableId
    private Long workId;
    private String repoUrl;
    private String languages;
    private Integer stars;
    private LocalDateTime lastStarFetch;
}
