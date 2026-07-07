package me.aloic.apeurival.entity.dto.osu.optionalattributes.beatmap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Mod implements Serializable
{
    private String acronym;
    private ModSetting settings;

    public Mod(String acronym)
    {
        this.acronym=acronym;
    }



}
