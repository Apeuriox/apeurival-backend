package me.aloic.apeurival.entity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.aloic.apeurival.entity.po.VaultItemPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface VaultItemMapper extends BaseMapper<VaultItemPO> {

    @Select("SELECT owner_id, author_name, COUNT(*) AS item_count FROM vault_items GROUP BY owner_id, author_name")
    List<Map<String, Object>> countByAuthors();

    @Select("SELECT owner_id, author_name, COUNT(*) AS item_count FROM vault_items GROUP BY owner_id, author_name")
    Page<Map<String, Object>> countByAuthorsPage(Page<?> page);
}
