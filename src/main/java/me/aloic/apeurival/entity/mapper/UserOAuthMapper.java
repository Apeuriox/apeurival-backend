package me.aloic.apeurival.entity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.aloic.apeurival.entity.po.UserOAuthPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserOAuthMapper extends BaseMapper<UserOAuthPO> {
}
