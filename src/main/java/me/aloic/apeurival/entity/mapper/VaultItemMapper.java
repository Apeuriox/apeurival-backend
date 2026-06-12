package me.aloic.apeurival.entity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.entity.po.VaultItemPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface VaultItemMapper extends BaseMapper<VaultItemPO> {

    Page<Map<String, Object>> countByAuthorsPage(Page<?> page);

    Page<VaultItemPO> listVisibleItemsPage(Page<VaultItemPO> page,
                                           @Param("ownerId") Long ownerId,
                                           @Param("authorName") String authorName,
                                           @Param("isAdmin") boolean isAdmin,
                                           @Param("isOwner") boolean isOwner,
                                           @Param("isEditor") boolean isEditor,
                                           @Param("isLoggedIn") boolean isLoggedIn);
}
