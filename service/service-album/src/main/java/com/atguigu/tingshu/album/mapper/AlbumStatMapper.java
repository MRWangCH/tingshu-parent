package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AlbumStatMapper extends BaseMapper<AlbumStat> {

    /**
     * 更新声音统计信息
     * @param albumId
     * @param statType
     * @param count
     */
    @Update("update album_stat set stat_num = stat_num + #{count} where album_id = #{albumId} and stat_type = #{statType}")
    void updateStat(@Param("albumId") Long albumId,@Param("statType") String statType,@Param("count") Integer count);
}
