package me.aloic.apeurival.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName(value = "posts", autoResultMap = true)
@AllArgsConstructor
@NoArgsConstructor
public class BlogPostPO
{
      @TableId(type = IdType.AUTO)
      private Long id;

      private String slug;

      private String titleZh;

      private String titleEn;

      private String excerptZh;

      private String excerptEn;

      private String contentMd;

      private String coverUrl;

      private String tags;              // "announcement,tech"

      private Integer status = 0;       // 0=draft, 1=published

      private Long authorId;

      private LocalDateTime createdAt;

      private LocalDateTime updatedAt;

      private LocalDateTime publishedAt;

  }