package me.aloic.apeurival.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("video_works")
@AllArgsConstructor
@NoArgsConstructor
public class VideoWorkPO {
    @TableId
    private Long workId;
    private String bvid;
    private String platform;
}
