package me.aloic.apeurival.entity.dto.osu.optionalattributes.player;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class RankHighest implements Serializable {
    private Integer rank;
    private String updated_at;

}
