package me.aloic.apeurival.entity.dto.osu.player;

import lombok.Data;
import lombok.NoArgsConstructor;
import me.aloic.apeurival.entity.dto.osu.beatmap.ScoreLazerDTO;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class BeatmapUserScores implements Serializable
{
    private List<ScoreLazerDTO> scores;
}