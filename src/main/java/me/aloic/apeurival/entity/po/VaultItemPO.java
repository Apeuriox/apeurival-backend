package me.aloic.apeurival.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("vault_items")
@AllArgsConstructor
@NoArgsConstructor
public class VaultItemPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ownerId;
    private String authorName;
    private String imageUrl;
    private String label;
    private Long groupId;
    private String visibility;  // PUBLIC | RESTRICTED | EDITOR_ONLY | PRIVATE
    private LocalDateTime createdAt;
}
