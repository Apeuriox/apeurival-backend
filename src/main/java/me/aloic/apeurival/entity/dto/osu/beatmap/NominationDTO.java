package me.aloic.apeurival.entity.dto.osu.beatmap;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class NominationDTO implements Serializable
{
    private Integer beatmapset_id;
    private RulesetDTO[] rulesets;
    private Boolean reset;
    private Integer user_id;
}
