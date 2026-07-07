package me.aloic.apeurival.entity.dto.osu.player;

import lombok.Data;
import lombok.NoArgsConstructor;
import me.aloic.apeurival.entity.dto.osu.beatmap.ScoreLazerDTO;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class BeatmapUserScoreLazer implements Serializable
{
    private Integer position;
    private ScoreLazerDTO score;
}