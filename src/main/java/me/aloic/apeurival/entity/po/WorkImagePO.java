package me.aloic.apeurival.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("work_images")
@AllArgsConstructor
@NoArgsConstructor
public class WorkImagePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workId;
    private String imageUrl;
    private String label;
    private Integer sortOrder;
}
