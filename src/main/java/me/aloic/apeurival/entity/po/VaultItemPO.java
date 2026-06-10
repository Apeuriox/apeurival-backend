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
    private String visibility;  // PUBLIC | MEMBERS | RESTRICTED | PRIVATE
    private LocalDateTime createdAt;
}
