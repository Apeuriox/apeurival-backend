package me.aloic.apeurival.entity.dto.osu.optionalattributes.player;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class Level implements Serializable {
    private Integer current;
    private Integer progress;


}
