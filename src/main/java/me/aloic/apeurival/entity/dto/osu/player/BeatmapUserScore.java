package me.aloic.apeurival.entity.dto.osu.player;

import lombok.Data;
import lombok.NoArgsConstructor;
import me.aloic.apeurival.entity.dto.osu.beatmap.ScoreDTO;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class BeatmapUserScore implements Serializable
{
    private Integer position;
    private ScoreDTO score;
}
