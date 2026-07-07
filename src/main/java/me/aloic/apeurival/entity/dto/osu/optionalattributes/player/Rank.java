package me.aloic.apeurival.entity.dto.osu.optionalattributes.player;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class Rank implements Serializable {
    private Integer country;
}
