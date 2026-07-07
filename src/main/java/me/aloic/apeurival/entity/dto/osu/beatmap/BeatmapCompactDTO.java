package me.aloic.apeurival.entity.dto.osu.beatmap;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class BeatmapCompactDTO implements Serializable {
    private Integer beatmapset_id;
    private Double difficulty_rating;
    private Integer id;
    private String mode;
    private String status;
    private Integer total_length;
    private Integer user_id;
    private String version;
    private String checksum;
    private Integer max_combo;
    private BeatmapsetDTO beatmapset;
}
