package me.aloic.apeurival.entity.dto.osu.player;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class UserGroupDTO extends GroupDTO implements Serializable{
    private String[] playmodes;
}
