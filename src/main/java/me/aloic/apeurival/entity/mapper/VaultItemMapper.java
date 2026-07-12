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

    Page<Map<String, Object>> countByAuthorsPage(Page<?> page,
                                                 @Param("groupId") Long groupId,
                                                 @Param("currentUserId") Long currentUserId,
                                                 @Param("visibilities") List<String> visibilities);

    List<Map<String, Object>> countByAuthorsWithGroups(@Param("groupId") Long groupId,
                                                       @Param("visibilities") java.util.List<String> visibilities);

    Page<VaultItemPO> selectGroupItemsPage(Page<VaultItemPO> page,
                                           @Param("groupId") Long groupId,
                                           @Param("ownerId") Long ownerId,
                                           @Param("authorName") String authorName,
                                           @Param("currentUserId") Long currentUserId,
                                           @Param("visibilities") java.util.List<String> visibilities);

    Page<VaultItemPO> selectNonGroupItemsPage(Page<VaultItemPO> page,
                                              @Param("ownerId") Long ownerId,
                                              @Param("authorName") String authorName,
                                              @Param("currentUserId") Long currentUserId,
                                              @Param("visibilities") java.util.List<String> visibilities);
}
