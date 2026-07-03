package me.aloic.apeurival.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("operation_logs")
@AllArgsConstructor
@NoArgsConstructor
public class OperationLogPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String entityType;

    private Long entityId;

    private String operation;

    private Long operatorId;

    private String entitySnapshot;

    private String previousSnapshot;

    private LocalDateTime createdAt;
}
