package top.lifeassistant.sharedmedia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.lifeassistant.sharedmedia.model.entity.MediaProgressDO;

import java.util.List;

@Mapper
public interface MediaProgressMapper extends BaseMapper<MediaProgressDO> {

    @Select("SELECT * FROM media_progress WHERE media_id = #{mediaId}")
    List<MediaProgressDO> selectByMediaId(@Param("mediaId") String mediaId);

    @Select("SELECT * FROM media_progress WHERE media_id = #{mediaId} AND user_id IS NULL")
    MediaProgressDO selectSharedByMediaId(@Param("mediaId") String mediaId);

    @Select("SELECT * FROM media_progress WHERE media_id = #{mediaId} AND user_id = #{userId}")
    MediaProgressDO selectByMediaIdAndUser(@Param("mediaId") String mediaId, @Param("userId") String userId);
}
