package me.aloic.apeurival.converter;

import me.aloic.apeurival.entity.dto.WorkDetailDTO;
import me.aloic.apeurival.entity.dto.WorkSummaryDTO;
import me.aloic.apeurival.entity.po.CodeWorkPO;
import me.aloic.apeurival.entity.po.ImageWorkPO;
import me.aloic.apeurival.entity.po.UserPO;
import me.aloic.apeurival.entity.po.VideoWorkPO;
import me.aloic.apeurival.entity.po.WorkPO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class WorkConverter {

    private WorkConverter() {}

    public static WorkSummaryDTO toSummary(WorkPO po, UserPO author) {
        WorkSummaryDTO dto = new WorkSummaryDTO();
        dto.setId(po.getId());
        dto.setTitle(po.getTitle());
        dto.setType(po.getType());
        dto.setCoverUrl(po.getCoverUrl());
        dto.setTags(splitTags(po.getTags()));
        dto.setAuthor(authorBrief(author));
        dto.setDate(po.getCreatedAt().toLocalDate());
        return dto;
    }

    public static WorkDetailDTO toDetail(WorkPO po, UserPO author,
                                         CodeWorkPO code, ImageWorkPO image, VideoWorkPO video) {
        WorkDetailDTO dto = new WorkDetailDTO();
        dto.setId(po.getId());
        dto.setTitle(po.getTitle());
        dto.setDescription(po.getDescription());
        dto.setType(po.getType());
        dto.setCoverUrl(po.getCoverUrl());
        dto.setTags(splitTags(po.getTags()));
        dto.setAuthor(authorBrief(author));
        dto.setDate(po.getCreatedAt().toLocalDate());

        if (code != null) {
            WorkDetailDTO.CodeContent c = new WorkDetailDTO.CodeContent();
            c.setRepoUrl(code.getRepoUrl());
            c.setLanguages(splitTags(code.getLanguages()));
            c.setStars(code.getStars());
            dto.setCode(c);
        }
        if (image != null) {
            WorkDetailDTO.ImageContent c = new WorkDetailDTO.ImageContent();
            c.setImageUrl(image.getImageUrl());
            c.setWidth(image.getWidth());
            c.setHeight(image.getHeight());
            c.setFormat(image.getFormat());
            dto.setImage(c);
        }
        if (video != null) {
            WorkDetailDTO.VideoContent c = new WorkDetailDTO.VideoContent();
            c.setBvid(video.getBvid());
            c.setPlatform(video.getPlatform());
            c.setEmbedUrl("//player.bilibili.com/player.html?bvid=" + video.getBvid());
            dto.setVideo(c);
        }
        return dto;
    }

    public static List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) return Collections.emptyList();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static WorkDetailDTO.AuthorBrief authorBrief(UserPO user) {
        if (user == null) return null;
        WorkDetailDTO.AuthorBrief ab = new WorkDetailDTO.AuthorBrief();
        ab.setId(user.getId());
        ab.setDisplayName(user.getDisplayName());
        ab.setAvatarUrl(user.getAvatarUrl());
        ab.setProfileUrl(user.getProfileUrl());
        return ab;
    }
}
