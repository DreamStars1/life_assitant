package top.lifeassistant.sharedmedia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.lifeassistant.sharedmedia.model.entity.MediaProgressEventDO;

import java.util.List;

@Mapper
public interface MediaProgressEventMapper extends BaseMapper<MediaProgressEventDO> {

    @Select("SELECT * FROM media_progress_event WHERE media_id = #{mediaId} ORDER BY created_at DESC")
    List<MediaProgressEventDO> selectByMediaId(@Param("mediaId") String mediaId);
}
