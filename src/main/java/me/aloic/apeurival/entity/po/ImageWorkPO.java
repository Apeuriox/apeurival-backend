package me.aloic.apeurival.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("image_works")
@AllArgsConstructor
@NoArgsConstructor
public class ImageWorkPO {
    @TableId
    private Long workId;
    private String imageUrl;
    private Integer width;
    private Integer height;
    private String format;
}
